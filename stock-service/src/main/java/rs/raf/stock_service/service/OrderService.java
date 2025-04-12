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
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.OrderMapper;
import rs.raf.stock_service.exceptions.*;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.repository.TransactionRepository;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final BankClient bankClient;
    private ListingRepository listingRepository;
    private ListingPriceHistoryRepository listingPriceHistoryRepository;
    private ListingMapper listingMapper;
    private TransactionRepository transactionRepository;
    private final PortfolioService portfolioService;
    private PortfolioEntryRepository portfolioEntryRepository;

    public Page<OrderDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> ordersPage = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);

        return ordersPage.map(order -> {
            ListingDto listingDto = listingMapper.toDto(order.getListing(),
                    listingPriceHistoryRepository.findTopByListingOrderByDateDesc(order.getListing()));
            String clientName = getClientName(order);
            return OrderMapper.toDto(order, listingDto, clientName, order.getAccountNumber());
        });
    }

    public List<OrderDto> getOrdersByUser(Long userId, String authHeader) {
        Long userIdFromAuth = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        String role = jwtTokenUtil.getUserRoleFromAuthHeader(authHeader);
        List<Order> ordersList;

        if (userId.equals(userIdFromAuth) || role.equalsIgnoreCase("SUPERVISOR") || role.equalsIgnoreCase("ADMIN")) {
            ordersList = orderRepository.findAllByUserId(userId);
        } else {
            throw new UnauthorizedException("Unauthorized attempt at getting user's orders.");
        }

        return ordersList.stream().map(order -> {
            ListingDto listingDto = listingMapper.toDto(order.getListing(),
                    listingPriceHistoryRepository.findTopByListingOrderByDateDesc(order.getListing()));
            String clientName = getClientName(order);
            return OrderMapper.toDto(order, listingDto, clientName, order.getAccountNumber());
        }).toList();
    }

    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAllByDirection(OrderDirection.SELL);

        return orders.stream().map(order -> {
            ListingDto listingDto = listingMapper.toDto(order.getListing(),
                    listingPriceHistoryRepository.findTopByListingOrderByDateDesc(order.getListing()));
            String clientName = getClientName(order);
            return OrderMapper.toDto(order, listingDto, clientName, order.getAccountNumber());
        }).collect(Collectors.toList());
    }

    private String getClientName(Order order) {
        try {
            ClientDto client = userClient.getClientById(order.getUserId());
            return formatName(client.getFirstName(), client.getLastName());
        } catch (Exception e1) {
            try {
                ActuaryDto actuary = userClient.getEmployeeById(order.getUserId());
                return formatName(actuary.getFirstName(), actuary.getLastName());
            } catch (Exception e2) {
                return "Unknown User";
            }
        }
    }

    private String formatName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return "Unknown User";
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public void cancelOrder(Long id, String authHeader) {
        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        String role = jwtTokenUtil.getUserRoleFromAuthHeader(authHeader);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getUserId().equals(userId) || role.equalsIgnoreCase("SUPERVISOR") || role.equalsIgnoreCase("ADMIN")) {
            if (!order.getIsDone() && (order.getStatus().equals(OrderStatus.PENDING) || order.getStatus().equals(OrderStatus.APPROVED))) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setLastModification(LocalDateTime.now());

                orderRepository.save(order);
            } else {
                throw new CantCancelOrderInCurrentOrderState(id);
            }
        } else {
            throw new UnauthorizedException("Unauthorized attempt at cancelling an order.");
        }
    }

    public void approveOrder(Long id, String authHeader) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getStatus().equals(OrderStatus.PENDING))
            throw new CantApproveNonPendingOrder(order.getId());

        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        BigDecimal price = BigDecimal.valueOf(order.getContractSize()).multiply(BigDecimal.valueOf(order.getQuantity()))
                .multiply(order.getPricePerUnit());
        if(order.getDirection() == OrderDirection.BUY)
            order.setStatus(updateAvailableBalance(order, price) ? OrderStatus.APPROVED : OrderStatus.DECLINED);
        else
            order.setStatus(OrderStatus.APPROVED);
        order.setApprovedBy(userId);
        order.setLastModification(LocalDateTime.now());

        orderRepository.save(order);

        if(order.getOrderType() == OrderType.MARKET)
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
        if((createOrderDto.getOrderType() == OrderType.STOP || createOrderDto.getOrderType() == OrderType.STOP_LIMIT) &&
                createOrderDto.getStopPrice() == null){
            throw new StopPriceMissingException(createOrderDto.getOrderType());
        }

        if((createOrderDto.getOrderType() == OrderType.LIMIT || createOrderDto.getOrderType() == OrderType.STOP_LIMIT) &&
                createOrderDto.getLimitPrice() == null){
            throw new LimitPriceMissingException(createOrderDto.getOrderType());
        }

        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        String role = jwtTokenUtil.getUserRoleFromAuthHeader(authHeader);
        Listing listing = listingRepository.findById(createOrderDto.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(createOrderDto.getListingId()));

        Order order = OrderMapper.toOrder(createOrderDto, userId, listing, role);

        BigDecimal price = BigDecimal.valueOf(order.getContractSize()).multiply(BigDecimal.valueOf(order.getQuantity()))
                .multiply(order.getPricePerUnit());

        // commission
        if (role.equals("CLIENT")) {
            BigDecimal commission = price.multiply(BigDecimal.valueOf(0.01)); // lupio sam 1%
            order.setCommission(commission);
        } else {
            order.setCommission(null); // aktuar nema proviziju
        }


        boolean checksPassed = false;
        if(role.equals("AGENT")) {
            ActuaryLimitDto actuaryLimitDto = userClient.getActuaryByEmployeeId(userId);

            if (!actuaryLimitDto.isNeedsApproval() && actuaryLimitDto.getLimitAmount().subtract(actuaryLimitDto.
                    getUsedLimit()).compareTo(price) >= 0)
                checksPassed = true;
        }  else {
            checksPassed = true;
        }

        if(checksPassed){
            if(order.getDirection() == OrderDirection.BUY){
                if(role.equals("CLIENT")){
                    price = priceWithCommission(order.getOrderType(), price);
                }

                order.setStatus(updateAvailableBalance(order, price) ? OrderStatus.APPROVED : OrderStatus.DECLINED);
            } else {
                order.setStatus(OrderStatus.APPROVED);
            }
        }

        if (order.getDirection().equals(OrderDirection.SELL)) {
            PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(userId, listing).
                    orElseThrow(PortfolioEntryNotFoundException::new);
            BigDecimal buyingPrice = portfolioEntry.getAveragePrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            BigDecimal sellPrice = order.getPricePerUnit().multiply(BigDecimal.valueOf(order.getQuantity()));
            BigDecimal potentialProfit = sellPrice.subtract(buyingPrice);
            //profit je uvek iz usd u rsd jer su stocks uvek u dolarima, a drzavni racun u rsd
            order.setProfit(bankClient.convert(new ConvertDto("USD", "RSD", potentialProfit)));
            if (potentialProfit.compareTo(BigDecimal.ZERO) > 0) {
                order.setTaxStatus(TaxStatus.PENDING);
                order.setTaxAmount(potentialProfit.multiply(new BigDecimal("0.15")));
            } else {
                order.setTaxStatus(TaxStatus.TAXFREE);
                order.setTaxAmount(BigDecimal.ZERO);
            }
        } else {
            order.setTaxStatus(TaxStatus.TAXFREE);
            order.setTaxAmount(BigDecimal.ZERO);
        }


        orderRepository.save(order);

        if (order.getOrderType() == OrderType.MARKET && order.getStatus() == OrderStatus.APPROVED)
            executeOrder(order);

        ListingDto listingDto = listingMapper.toDto(listing,
                listingPriceHistoryRepository.findTopByListingOrderByDateDesc(listing));

        String clientName = getClientName(order);

        return OrderMapper.toDto(order, listingDto, clientName, order.getAccountNumber());
    }

    private boolean updateAvailableBalance(Order order, BigDecimal amount) {
        if(order.getDirection() == OrderDirection.SELL)
            return true;

        try{
            bankClient.updateAvailableBalance(order.getAccountNumber(), amount);
        }catch (InsufficientFundsException e){
            return false;
        }

        order.setReservedAmount(amount);
        return true;
    }

    public BigDecimal priceWithCommission(OrderType orderType, BigDecimal amount){
        BigDecimal commissionPercentage;
        BigDecimal commissionMax;
        if (orderType == OrderType.MARKET || orderType == OrderType.STOP){
            commissionPercentage = BigDecimal.valueOf(0.14);
            commissionMax = BigDecimal.valueOf(7);
        }
        else{
            commissionPercentage = BigDecimal.valueOf(0.24);
            commissionMax = BigDecimal.valueOf(12);
        }

        return amount.add(amount.multiply(commissionPercentage).min(commissionMax));
    }

    @Async
    public void executeOrder(Order order) {
        if (order.getIsDone() || order.getStatus() != OrderStatus.APPROVED) return; //better safe than sorry
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        long volume = 1000000;
        if(order.getListing() instanceof Stock){
            volume = Math.max(200000, ((Stock) order.getListing()).getVolume());
        }

        BigDecimal spentAmount = BigDecimal.ZERO;
        if (order.isAllOrNone()){
            spentAmount = spentAmount.add(executeTransaction(order, order.getRemainingPortions(), volume));
        } else {
            Random random = new Random();

            while (order.getRemainingPortions() > 0) {
                spentAmount = spentAmount.add(executeTransaction(order,
                        random.nextInt(1, order.getRemainingPortions() + 1), volume));
                orderRepository.save(order);
            }
        }

        //Extreme edge case PARTIAL: account nije u dolarima, a exhange rate se promenio i
        // stime trosak ispada vise od rezervisanog pa se obustavlja
        order.setStatus(order.getRemainingPortions() == 0? OrderStatus.DONE : OrderStatus.PARTIAL);
        order.setIsDone(true);
        orderRepository.save(order);

        //finalna azuriranja sredstava
        if(order.getDirection() == OrderDirection.BUY){
            BigDecimal priceWithCommission = order.getRole().equals("CLIENT") ?
                    priceWithCommission(order.getOrderType(), spentAmount) : spentAmount;

            updateBalance(order, priceWithCommission.subtract(spentAmount));
            updateAvailableBalance(order, priceWithCommission.subtract(order.getReservedAmount()));
        }

        portfolioService.updateHoldingsOnOrderExecution(order);
    }

    private BigDecimal executeTransaction(Order order, int batchSize, long volume){
        Long extraTime = order.getAfterHours() ? 300000L : 0L;
        Random random = new Random();

        try {
            Double randomTime = random.nextDouble(0, 1440.0 * order.getRemainingPortions() / volume) * 1000;
            Thread.sleep(randomTime.longValue() + extraTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BigDecimal totalPrice = BigDecimal.valueOf(batchSize).multiply(order.getPricePerUnit()).multiply(BigDecimal.valueOf(order.getContractSize()));

        if(order.getDirection() == OrderDirection.BUY && !updateBalance(order, totalPrice)) return BigDecimal.ZERO;

        Transaction transaction = new Transaction(batchSize, order.getPricePerUnit(), totalPrice, order);
        transactionRepository.save(transaction);

        order.getTransactions().add(transaction);
        order.setLastModification(LocalDateTime.now());
        order.setRemainingPortions(order.getRemainingPortions() - batchSize);

        return totalPrice;
    }

    private boolean updateBalance(Order order, BigDecimal amount){
        if (order.getDirection() == OrderDirection.SELL)
            return true;

        try{
            bankClient.updateBalance(order.getAccountNumber(), amount);
        }catch (InsufficientFundsException e){
            return false;
        }

        return true;
    }

    @Scheduled(fixedRate = 15000)
    public void checkOrders() {
        checkStopOrders();
        checkStopLimitOrders();
        checkLimitOrders();
    }

    private void checkStopOrders(){
        List<Order> orders = orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.STOP);

        for (Order order : orders) {
            checkStopOrder(order);
        }
    }

    private void checkStopLimitOrders(){
        List<Order> orders = orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.STOP_LIMIT);

        for (Order order : orders) {
            if (!order.isStopFulfilled()){
               checkStopOrder(order);
            } else {
               checkLimitOrder(order);
            }
        }
    }

    private void checkLimitOrders(){
        List<Order> orders = orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.LIMIT);

        for (Order order : orders) {
           checkLimitOrder(order);
        }
    }

    private void checkStopOrder(Order order){
        boolean conditionFulfilled = false;

        if (order.getDirection() == OrderDirection.BUY){
            BigDecimal askPrice = order.getListing().getAsk() == null ? order.getListing().getPrice() : order.getListing().getAsk();
            if (askPrice.compareTo(order.getStopPrice()) > 0)
                conditionFulfilled = true;
        } else if (order.getListing().getPrice().compareTo(order.getStopPrice()) < 0) {
            conditionFulfilled = true;
        }

        if (conditionFulfilled){
            order.setStopFulfilled(true);
            if (order.getOrderType() == OrderType.STOP)
                executeOrder(order);
            else
                orderRepository.save(order);
        }
    }

    private void checkLimitOrder(Order order){
        if (order.getDirection() == OrderDirection.BUY){
            BigDecimal askPrice = order.getListing().getAsk() == null ? order.getListing().getPrice() : order.getListing().getAsk();

            if(askPrice.compareTo(order.getPricePerUnit()) <= 0) {
                order.setPricePerUnit(order.getPricePerUnit().min(askPrice));
                executeOrder(order);
            }
        } else if (order.getListing().getPrice().compareTo(order.getPricePerUnit()) >= 0) {
            order.setPricePerUnit(order.getPricePerUnit().max(order.getListing().getPrice()));
            executeOrder(order);
        }
    }


}
