package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.entity.Transaction;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.OrderMapper;
import rs.raf.stock_service.exceptions.CantApproveNonPendingOrder;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.repository.TransactionRepository;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final BankClient bankClient;
    private ListingRepository listingRepository;
    private ListingPriceHistoryRepository dailyPriceInfoRepository;
    private ListingMapper listingMapper;
    private TransactionRepository transactionRepository;
    private final PortfolioService portfolioService;
    private final PortfolioEntryRepository portfolioEntryRepository;

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

    public OrderDto createOrder(CreateOrderDto createOrderDto, String authHeader) {
        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        Listing listing = listingRepository.findById(createOrderDto.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(createOrderDto.getListingId()));

        //Nzm odakle se ovo uzima, msm kada se zatvara trziste
        boolean afterHours = false;

        Order order = OrderMapper.toOrder(createOrderDto, userId, listing, afterHours);

        BigDecimal approxPrice = BigDecimal.valueOf(order.getContractSize()).multiply(order.getPricePerUnit().
                multiply(BigDecimal.valueOf(order.getQuantity())));

        if (jwtTokenUtil.getUserRoleFromAuthHeader(authHeader).equals("CLIENT")) {
            order.setStatus(verifyBalance(order) ? OrderStatus.APPROVED : OrderStatus.DECLINED);
        } else {
            ActuaryLimitDto actuaryLimitDto = userClient.getActuaryByEmployeeId(userId); // throw agentNotFound

            if (!actuaryLimitDto.isNeedsApproval()) {
                if (actuaryLimitDto.getLimitAmount().subtract(actuaryLimitDto.getUsedLimit()).compareTo(approxPrice) >= 0)
                    order.setStatus(OrderStatus.APPROVED);
            }
        }
        if (order.getDirection().equals(OrderDirection.SELL)) {
            PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(userId, listing).
                    orElseThrow(PortfolioEntryNotFoundException::new);
            BigDecimal buyingPrice = portfolioEntry.getAveragePrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            BigDecimal sellPrice = order.getPricePerUnit().multiply(BigDecimal.valueOf(order.getQuantity()));
            BigDecimal potentialProfit = sellPrice.subtract(buyingPrice);
            if (potentialProfit.compareTo(BigDecimal.ZERO) > 0) {
                order.setTaxStatus(TaxStatus.PENDING);
                order.setTaxAmount(potentialProfit.multiply(new BigDecimal("0.15")));
            } else {
                order.setTaxStatus(TaxStatus.TAXFREE);
                order.setTaxAmount(BigDecimal.ZERO);
            }
        }else{
            order.setTaxStatus(TaxStatus.TAXFREE);
            order.setTaxAmount(BigDecimal.ZERO);
        }


        orderRepository.save(order);

        if (order.getStatus() == OrderStatus.APPROVED)
            executeOrder(order);

        return OrderMapper.toDto(order, listingMapper.toDto(listing,
                dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing)));
    }

    private boolean verifyBalance(Order order) {
        BigDecimal price = BigDecimal.valueOf(order.getContractSize()).multiply(BigDecimal.valueOf(order.getQuantity()))
                .multiply(order.getPricePerUnit());

        BigDecimal commissionPercentage = price.multiply(BigDecimal.valueOf(0.14));
        BigDecimal commission = commissionPercentage.compareTo(BigDecimal.valueOf(7)) < 0 ? commissionPercentage : BigDecimal.valueOf(7);

        price = price.add(commission);
        return price.compareTo(bankClient.getAccountBalance(order.getAccountNumber())) <= 0;
    }

    //async da bi se vratio OrderDto response paralelno sa izvrsenjem ordera,
    @Async
    public void executeOrder(Order order) {
        // za svaki slucaj provera ako se zaboravi pre poziva ove metode da se proveri
        if (order.getStatus() != OrderStatus.APPROVED)
            return;

        switch (order.getOrderType()) {
            case MARKET -> executeMarketOrder(order);
        }

        order.setIsDone(true);
        orderRepository.save(order);


        portfolioService.updateHoldingsOnOrderExecution(order);


        //mozda uvesti neko slanje notifikacije da je order zavrsen, nzm da li smo igde uveli notifikacije ili da li je opste scope
    }

    private void executeMarketOrder(Order order) {
        Random random = new Random();

        while (order.getRemainingPortions() > 0) {
            Integer remainingPortions = order.getRemainingPortions();
            Integer batchSize = random.nextInt(1, remainingPortions + 1);

            BigDecimal totalPrice = BigDecimal.valueOf(batchSize).multiply(order.getPricePerUnit())
                    .multiply(BigDecimal.valueOf(order.getContractSize()));

            Transaction transaction = new Transaction(batchSize, order.getPricePerUnit(), totalPrice, order);
            transactionRepository.save(transaction);

            order.getTransactions().add(transaction);

            order.setRemainingPortions(remainingPortions - batchSize);

            order.setLastModification(LocalDateTime.now());
            orderRepository.save(order);

            try {
                Thread.sleep(random.nextInt(0, 24 * 60 / (order.getQuantity() / remainingPortions)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Pomoc za ovo pliz
    private void transferCommissionToBankAccount() {
    }

    //@Scheduled(cron = "0 0 0 * * *")
    public void processTaxes() {
        for (Order order : orderRepository.findAll()) {
            if (order.getTaxAmount() != null && order.getTaxStatus().equals(TaxStatus.PENDING) &&
                    bankClient.getAccountBalance(order.getAccountNumber()).compareTo(order.getTaxAmount()) > 0) {
                TaxDto taxDto = new TaxDto();
                taxDto.setAmount(order.getTaxAmount());
                taxDto.setClientId(order.getUserId());
                taxDto.setSenderAccountNumber(order.getAccountNumber());
                bankClient.handleTax(taxDto);
                order.setTaxStatus(TaxStatus.PAID);
                orderRepository.save(order);
            }
        }
    }

}
