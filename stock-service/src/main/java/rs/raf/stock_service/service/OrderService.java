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
            return OrderMapper.toDto(order, listingDto, "", order.getAccountNumber());
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

        if (!order.getUserId().equals(userId) && !role.equalsIgnoreCase("SUPERVISOR") && !role.equalsIgnoreCase("ADMIN"))
            throw new UnauthorizedException("Unauthorized attempt at cancelling an order.");

        if (order.getIsDone() || (!order.getStatus().equals(OrderStatus.PENDING) && !order.getStatus().equals(OrderStatus.APPROVED)))
            throw new CantCancelOrderInCurrentOrderState(id);

        if (order.getDirection() == OrderDirection.SELL){
            PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(userId, order.getListing()).
                    orElseThrow(PortfolioEntryNotFoundException::new);

            portfolioEntry.setReservedAmount(portfolioEntry.getReservedAmount() - order.getContractSize() * order.getQuantity());
            portfolioEntryRepository.save(portfolioEntry);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setLastModification(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void approveOrder(Long id, String authHeader) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getStatus().equals(OrderStatus.PENDING))
            throw new CantApproveNonPendingOrder(order.getId());

        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        if(order.getDirection() == OrderDirection.BUY && !reserveBalance(order))
            order.setStatus(OrderStatus.DECLINED);
        else {
            order.setStatus(OrderStatus.APPROVED);
            order.setApprovedBy(userId);
        }

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

        if(order.getDirection() == OrderDirection.SELL){
            PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(order.getUserId(), order.getListing()).
                    orElseThrow(PortfolioEntryNotFoundException::new);

            portfolioEntry.setReservedAmount(portfolioEntry.getReservedAmount() - order.getContractSize() * order.getQuantity());
            portfolioEntryRepository.save(portfolioEntry);
        }

        order.setStatus(OrderStatus.DECLINED);
        order.setApprovedBy(jwtTokenUtil.getUserIdFromAuthHeader(authHeader));
        order.setLastModification(LocalDateTime.now());
        orderRepository.save(order);
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public OrderDto createOrder(CreateOrderDto createOrderDto, String authHeader) {
        checkFields(createOrderDto);

        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        String role = jwtTokenUtil.getUserRoleFromAuthHeader(authHeader);
        Listing listing = listingRepository.findById(createOrderDto.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(createOrderDto.getListingId()));

        if (createOrderDto.getOrderDirection() == OrderDirection.SELL) {
            PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(userId, listing).
                    orElseThrow(PortfolioEntryNotFoundException::new);

            reservePortfolioAmount(portfolioEntry, createOrderDto.getContractSize() * createOrderDto.getQuantity());
        }

        Order order = OrderMapper.toOrder(createOrderDto, userId, role, listing);
        setOrderStatus(order);

        orderRepository.save(order);

        if (order.getOrderType() == OrderType.MARKET && order.getStatus() == OrderStatus.APPROVED)
            executeOrder(order);

        ListingDto listingDto = listingMapper.toDto(listing,
                listingPriceHistoryRepository.findTopByListingOrderByDateDesc(listing));

        return OrderMapper.toDto(order, listingDto, getClientName(order), order.getAccountNumber());
    }

    private void checkFields(CreateOrderDto createOrderDto){
        if((createOrderDto.getOrderType() == OrderType.STOP || createOrderDto.getOrderType() == OrderType.STOP_LIMIT) &&
                createOrderDto.getStopPrice() == null){
            throw new StopPriceMissingException(createOrderDto.getOrderType());
        }

        if((createOrderDto.getOrderType() == OrderType.LIMIT || createOrderDto.getOrderType() == OrderType.STOP_LIMIT) &&
                createOrderDto.getLimitPrice() == null){
            throw new LimitPriceMissingException(createOrderDto.getOrderType());
        }
    }

    private void reservePortfolioAmount(PortfolioEntry portfolioEntry, Integer amount){
        if (portfolioEntry.getAvailableAmount() < amount)
            throw new PortfolioAmountNotEnoughException(portfolioEntry.getAmount(), amount);

        portfolioEntry.setReservedAmount(portfolioEntry.getReservedAmount() + amount);
        portfolioEntryRepository.save(portfolioEntry);
    }

    private void setOrderStatus(Order order){
        if(order.getUserRole().equals("AGENT")) {
            ActuaryLimitDto actuaryLimitDto = userClient.getActuaryByEmployeeId(order.getUserId());

            if (actuaryLimitDto.isNeedsApproval() || actuaryLimitDto.getLimitAmount().subtract(actuaryLimitDto.
                    getUsedLimit()).compareTo(order.getTotalPrice()) < 0)
                return;
        }

        order.setStatus(reserveBalance(order) ? OrderStatus.APPROVED : OrderStatus.DECLINED);
    }

    private boolean reserveBalance(Order order){
        BigDecimal price =  order.getTotalPrice();
        BigDecimal commission = order.getUserRole().equals("CLIENT") ?
                getCommission(order.getOrderType(), price) : BigDecimal.ZERO;

        order.setCommission(commission);

        if(order.getDirection() == OrderDirection.SELL)
            return true;

        return updateAvailableBalance(order.getAccountNumber(), price.add(commission).multiply(BigDecimal.valueOf(-1)));
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Async
    public void executeOrder(Order order) {
        if (order.getIsDone() || order.getStatus() != OrderStatus.APPROVED) return; //better safe than sorry
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        long volume = 1000000;
        if(order.getListing() instanceof Stock){
            volume = Math.max(200000, ((Stock) order.getListing()).getVolume());
        }

        BigDecimal finalAmount = BigDecimal.ZERO;
        if (order.isAllOrNone()){
            finalAmount = finalAmount.add(executeTransaction(order, order.getRemainingPortions(), volume));
        } else {
            Random random = new Random();

            while (order.getRemainingPortions() > 0) {
                finalAmount = finalAmount.add(executeTransaction(order,
                        random.nextInt(1, order.getRemainingPortions() + 1), volume));
                orderRepository.save(order);
            }
        }

        balanceAdjustment(order, finalAmount);
        //Extreme edge case PARTIAL: account nije u dolarima, a exhange rate se promenio i
        // stime trosak ispada vise od rezervisanog pa se obustavilo na pola
        order.setStatus(order.getRemainingPortions() == 0? OrderStatus.DONE : OrderStatus.PARTIAL);
        order.setIsDone(true);

        setOrderProfitAndTax(order);
        portfolioService.updateHoldingsOnOrderExecution(order);
        orderRepository.save(order);
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

        BigDecimal totalPrice = BigDecimal.valueOf(batchSize).multiply(order.getPricePerUnit())
                .multiply(BigDecimal.valueOf(order.getContractSize()));

        if(order.getDirection() == OrderDirection.BUY){
            if (!updateBalance(order.getAccountNumber(), totalPrice.multiply(BigDecimal.valueOf(-1))))
                return BigDecimal.ZERO;
        } else {
            updateBalance(order.getAccountNumber(), totalPrice);
        }

        Transaction transaction = new Transaction(batchSize, order.getPricePerUnit(), totalPrice, order);
        transactionRepository.save(transaction);

        order.getTransactions().add(transaction);
        order.setLastModification(LocalDateTime.now());
        order.setRemainingPortions(order.getRemainingPortions() - batchSize);

        return totalPrice;
    }

    private void balanceAdjustment(Order order, BigDecimal finalAmount){
        BigDecimal finalCommission = BigDecimal.ZERO;

        if (order.getUserRole().equals("CLIENT")){
            finalCommission = getCommission(order.getOrderType(), finalAmount);
            transferCommission(order.getAccountNumber(), finalCommission);
        }

        if (order.getDirection() == OrderDirection.BUY){
            bankClient.updateAvailableBalance(order.getAccountNumber(),
                    order.getTotalPrice().add(order.getCommission()).subtract(finalAmount.add(finalCommission)));
        } else {
            bankClient.updateAvailableBalance(order.getAccountNumber(),
                    finalAmount.subtract(finalCommission));
        }

        order.setTotalPrice(finalAmount);
        order.setCommission(finalCommission);
    }

    private void transferCommission(String accountNumber, BigDecimal amount){
        try{
            String bankAccount = bankClient.getUSDAccountNumberByClientId(1L).getBody();
            
            bankClient.updateBalance(accountNumber, amount.multiply(BigDecimal.valueOf(-1)));
        }catch (InsufficientFundsException e){
            e.printStackTrace();
        }
    }

    private void setOrderProfitAndTax(Order order){
        if (order.getDirection() == OrderDirection.BUY) {
            order.setTaxStatus(TaxStatus.TAXFREE);
            order.setTaxAmount(BigDecimal.ZERO);
            return;
        }

        PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(order.getUserId(), order.getListing()).
                orElseThrow(PortfolioEntryNotFoundException::new);

        BigDecimal buyingPrice = portfolioEntry.getAveragePrice()
                .multiply(BigDecimal.valueOf(order.getQuantity() * order.getContractSize()));
        BigDecimal potentialProfit = order.getTotalPrice().subtract(buyingPrice);

        //profit je uvek iz usd u rsd jer su stocks uvek u dolarima, a drzavni racun u rsd
        order.setProfit(bankClient.convert(new ConvertDto("USD", "RSD", potentialProfit)));

        if (potentialProfit.compareTo(BigDecimal.ZERO) > 0) {
            order.setTaxStatus(TaxStatus.PENDING);
            order.setTaxAmount(potentialProfit.multiply(new BigDecimal("0.15")));
        } else {
            order.setTaxStatus(TaxStatus.TAXFREE);
            order.setTaxAmount(BigDecimal.ZERO);
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private BigDecimal getCommission(OrderType orderType, BigDecimal amount){
        if (orderType == OrderType.MARKET || orderType == OrderType.STOP){
            return amount.multiply(BigDecimal.valueOf(0.14)).min( BigDecimal.valueOf(7));
        }

        return amount.multiply(BigDecimal.valueOf(0.24)).min(BigDecimal.valueOf(12));
    }

    private boolean updateAvailableBalance(String accountNumber, BigDecimal amount){
        try{
            bankClient.updateAvailableBalance(accountNumber, amount);
        }catch (InsufficientFundsException e){
            return false;
        }

        return true;
    }

    private boolean updateBalance(String accountNumber, BigDecimal amount){
        try{
            bankClient.updateBalance(accountNumber, amount);
        }catch (InsufficientFundsException e){
            return false;
        }

        return true;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    public BigDecimal getCommissionProfit() {
        return orderRepository.getBankProfitFromOrders();
    }
}
