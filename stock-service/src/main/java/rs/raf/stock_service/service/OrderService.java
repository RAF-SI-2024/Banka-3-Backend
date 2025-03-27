package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.ActuaryLimitDto;
import rs.raf.stock_service.domain.dto.CreateOrderDto;
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.Transaction;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.OrderMapper;
import rs.raf.stock_service.exceptions.CantApproveNonPendingOrder;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.exceptions.OrderStatusNotFoundException;
import rs.raf.stock_service.repository.ListingDailyPriceInfoRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private ListingRepository listingRepository;
    private ListingDailyPriceInfoRepository dailyPriceInfoRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final BankClient bankClient;
    private ListingMapper listingMapper;

    public Page<OrderDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> ordersPage;

        if (status == null) {
            ordersPage = orderRepository.findAll(pageable);
        } else {
            ordersPage = orderRepository.findByStatus(status, pageable);
        }

        return ordersPage.map(order -> OrderMapper.toDto(order, listingMapper.toDto(order.getListing(),
                dailyPriceInfoRepository.findTopByListingOrderByDateDesc(order.getListing()))));
    }

    public void approveOrder(Long id, String authHeader) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getStatus().equals(OrderStatus.PENDING))
            throw new CantApproveNonPendingOrder(order.getId());

        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        order.setStatus(OrderStatus.APPROVED);
        order.setApprovedBy(userId);
        order.setLastModification(LocalDateTime.now());

        orderRepository.save(order);
        executeOrder(order);
    }

    public void declineOrder(Long id, String authHeader) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getStatus().equals(OrderStatus.PENDING))
            throw new CantApproveNonPendingOrder(order.getId());

        order.setStatus(OrderStatus.DECLINED);
        order.setApprovedBy(jwtTokenUtil.getUserIdFromAuthHeader(authHeader));
        order.setLastModification(LocalDateTime.now());
        orderRepository.save(order);
    }

    public OrderDto createOrder(CreateOrderDto createOrderDto, String authHeader){
        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        Listing listing = listingRepository.findById(createOrderDto.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(createOrderDto.getListingId()));

        //Nzm odakle se ovo uzima, msm kada se zatvara trziste
        boolean afterHours = false;

        Order order = OrderMapper.toOrder(createOrderDto, userId, listing, afterHours);

        BigDecimal approxPrice = BigDecimal.valueOf(order.getContractSize()).multiply(order.getPricePerUnit().
                multiply(BigDecimal.valueOf(order.getQuantity())));

        if (jwtTokenUtil.getUserRoleFromAuthHeader(authHeader).equals("CLIENT")) {
            order.setStatus(verifyBalance(order)? OrderStatus.APPROVED : OrderStatus.DECLINED);
        } else {
            ActuaryLimitDto actuaryLimitDto = userClient.getActuaryByEmployeeId(userId); // throw agentNotFound

            if (!actuaryLimitDto.isNeedsApproval()){
                if (actuaryLimitDto.getLimitAmount().subtract(actuaryLimitDto.getUsedLimit()).compareTo(approxPrice) >= 0)
                    order.setStatus(OrderStatus.APPROVED);
            }
        }

        orderRepository.save(order);

        if (order.getStatus()  == OrderStatus.APPROVED)
            executeOrder(order);

        return OrderMapper.toDto(order, listingMapper.toDto(listing,
                dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing)));
    }

    private boolean verifyBalance(Order order){
        BigDecimal price = BigDecimal.valueOf(order.getContractSize()).multiply(BigDecimal.valueOf(order.getQuantity()))
                .multiply(order.getPricePerUnit());

        BigDecimal commissionPercentage = price.multiply(BigDecimal.valueOf(0.14));
        BigDecimal commission = commissionPercentage.compareTo(BigDecimal.valueOf(7)) < 0 ? commissionPercentage : BigDecimal.valueOf(7);

        price =  price.add(commission);
        return price.compareTo(bankClient.getAccountBalance(order.getAccountNumber())) <= 0;
    }

    //async da bi se vratio OrderDto response paralelno sa izvrsenjem ordera,
    @Async
    public void executeOrder(Order order){
        // za svaki slucaj provera ako se zaboravi pre poziva ove metode da se proveri
        if (order.getStatus() != OrderStatus.APPROVED)
            return;

        switch(order.getOrderType()){
            case MARKET -> executeMarketOrder(order);
        }

        order.setIsDone(true);
        orderRepository.save(order);

        //mozda uvesti neko slanje notifikacije da je order zavrsen, nzm da li smo igde uveli notifikacije ili da li je opste scope
    }

    private void executeMarketOrder(Order order){
        Random random = new Random();

        while (order.getRemainingPortions() > 0 ){
            Integer batchSize = random.nextInt(1, order.getRemainingPortions());

            BigDecimal totalPrice = BigDecimal.valueOf(batchSize).multiply(order.getPricePerUnit())
                    .multiply(BigDecimal.valueOf(order.getContractSize()));

            order.getTransactions().add(new Transaction(batchSize, order.getPricePerUnit(), totalPrice));
            order.setRemainingPortions(order.getRemainingPortions() - batchSize);

            order.setLastModification(LocalDateTime.now());
            orderRepository.save(order);

            try {
                Thread.sleep(random.nextInt(0, 24 * 60 / (order.getQuantity() /  order.getRemainingPortions())));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Pomoc za ovo pliz
    private void transferCommissionToBankAccount(){}
}
