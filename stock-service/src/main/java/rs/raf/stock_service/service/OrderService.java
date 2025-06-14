package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.*;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.OrderMapper;
import rs.raf.stock_service.exceptions.*;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.repository.TransactionRepository;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
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
    private final TrackedPaymentService trackedPaymentService;

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
        List<Order> orders = orderRepository.findAllByDirectionAndStatus(OrderDirection.SELL, OrderStatus.DONE);

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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        String role = jwtTokenUtil.getUserRoleFromAuthHeader(authHeader);

        if (!order.getUserId().equals(userId) && !role.equalsIgnoreCase("SUPERVISOR") && !role.equalsIgnoreCase("ADMIN"))
            throw new UnauthorizedException("Unauthorized attempt at cancelling an order.");

        if (!order.getStatus().equals(OrderStatus.PENDING) && !order.getStatus().equals(OrderStatus.APPROVED))
            throw new CantCancelOrderInCurrentOrderState(id);

        order.setStatus(OrderStatus.CANCELLED);
        order.setLastModification(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void approveOrder(Long id, String authHeader) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getStatus().equals(OrderStatus.PENDING))
            throw new CantApproveNonPendingOrder(order.getId());

        if (order.getDirection() == OrderDirection.SELL){
            PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(order.getUserId(), order.getListing())
                    .orElseThrow(PortfolioEntryNotFoundException::new);

            Integer amount = order.getQuantity();

            if (portfolioEntry.getAvailableAmount() < amount)
                throw new PortfolioAmountNotEnoughException(portfolioEntry.getAvailableAmount(), amount);

            portfolioEntry.setReservedAmount(portfolioEntry.getReservedAmount() + amount);
            portfolioEntryRepository.save(portfolioEntry);
        }

        order.setStatus(OrderStatus.APPROVED);
        order.setApprovedBy(jwtTokenUtil.getUserIdFromAuthHeader(authHeader));
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public OrderDto createOrder(CreateOrderDto createOrderDto, String authHeader) {
        if((createOrderDto.getOrderType() == OrderType.STOP || createOrderDto.getOrderType() == OrderType.STOP_LIMIT) &&
                createOrderDto.getStopPrice() == null)
            throw new StopPriceMissingException(createOrderDto.getOrderType());

        if((createOrderDto.getOrderType() == OrderType.LIMIT || createOrderDto.getOrderType() == OrderType.STOP_LIMIT) &&
                createOrderDto.getLimitPrice() == null)
            throw new LimitPriceMissingException(createOrderDto.getOrderType());

        Listing listing = listingRepository.findById(createOrderDto.getListingId())
                .orElseThrow(() -> new ListingNotFoundException(createOrderDto.getListingId()));

        AccountDetailsDto accountDetailsDto;
        try {
            accountDetailsDto = bankClient.getAccountDetails(createOrderDto.getAccountNumber());
        } catch (Exception e){
            throw new AccountNotFoundException(createOrderDto.getAccountNumber());
        }

        if (accountDetailsDto == null)
            throw new AccountNotFoundException(createOrderDto.getAccountNumber());

        if (!accountDetailsDto.getCurrencyCode().equals("USD"))
            throw new WrongCurrencyAccountException("USD");

        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        String role = jwtTokenUtil.getUserRoleFromAuthHeader(authHeader);

        Order order = OrderMapper.toOrder(createOrderDto, userId, role, listing);

        //Ako kupuje provera da li klijent ima sredstva, ako prodaje provera da li ima dovoljno listing-a u vlasnistvu
        BigDecimal expectedTotalPrice = order.getPricePerUnit()
                .multiply(BigDecimal.valueOf(order.getQuantity()));

        PortfolioEntry portfolioEntry = null;
        if (order.getDirection() == OrderDirection.SELL) {
            portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(userId, listing).
                    orElseThrow(PortfolioEntryNotFoundException::new);

            Integer amount = order.getQuantity();

            order.setAverageBuyingPrice(portfolioEntry.getAveragePrice());

            if (portfolioEntry.getAvailableAmount() < amount)
                throw new PortfolioAmountNotEnoughException(portfolioEntry.getAvailableAmount(), amount);
        } else if (order.getUserRole().equals("CLIENT")) {
            BigDecimal price =  expectedTotalPrice.add(getCommission(order.getOrderType(), expectedTotalPrice));

            if (price.compareTo(accountDetailsDto.getAvailableBalance()) > 0)
                throw new InsufficientFundsException(price);
        }


        if(order.getUserRole().equals("AGENT")) {
            ActuaryLimitDto actuaryLimitDto = userClient.getActuaryByEmployeeId(order.getUserId());

            if (actuaryLimitDto.getLimitAmount().subtract(actuaryLimitDto.getUsedLimit()).compareTo(expectedTotalPrice) < 0) {
                order.setStatus(OrderStatus.CANCELLED);
            } else {
                if (!actuaryLimitDto.isNeedsApproval())
                    order.setStatus(OrderStatus.APPROVED);
            }


        } else {
            order.setStatus(OrderStatus.APPROVED);
        }

        orderRepository.save(order);

        if (order.getStatus() == OrderStatus.APPROVED){
            if (order.getDirection() == OrderDirection.SELL){
                portfolioEntry.setReservedAmount(portfolioEntry.getReservedAmount() + order.getQuantity());
                portfolioEntryRepository.save(portfolioEntry);
            }

            if (order.getOrderType() == OrderType.MARKET)
                executeOrder(order);
        }

        ListingDto listingDto = listingMapper.toDto(listing,
                listingPriceHistoryRepository.findTopByListingOrderByDateDesc(listing));

        return OrderMapper.toDto(order, listingDto, getClientName(order), order.getAccountNumber());
    }

    private BigDecimal getCommission(OrderType orderType, BigDecimal amount){
        if (orderType == OrderType.MARKET || orderType == OrderType.STOP){
            return amount.multiply(BigDecimal.valueOf(0.14)).min( BigDecimal.valueOf(7));
        }

        return amount.multiply(BigDecimal.valueOf(0.24)).min(BigDecimal.valueOf(12));
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Async
    public void executeOrder(Order order) {
        if (order.getStatus() != OrderStatus.APPROVED) return;

        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);

        executeTransaction(order);
    }

    private void executeTransaction(Order order){
        Random random = new Random();
        long extraTime = order.getAfterHours() ? 300000L : 0L;
        long volume = order.getListing() instanceof Stock ? Math.max(200000, ((Stock) order.getListing()).getVolume()) : 1000000;

        try {
            if (!order.isAllOrNone() && order.getRemainingPortions() < order.getQuantity()) {
                Double randomTime = random.nextDouble(0, 1440.0 * order.getRemainingPortions() / volume) * 1000;
                System.out.println("Sleeping for " + randomTime);
                Thread.sleep(randomTime.longValue() + extraTime);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int batchSize = order.isAllOrNone() ? order.getRemainingPortions()
                : random.nextInt(1, Math.min(Math.max(order.getQuantity() / 10, 2), order.getRemainingPortions() + 1));

        BigDecimal totalPrice = BigDecimal.valueOf(batchSize).multiply(order.getPricePerUnit());

        Transaction transaction = new Transaction(batchSize, order.getPricePerUnit(), totalPrice, order);
        transactionRepository.save(transaction);

        transferMoney(transaction);
    }

    private void transferMoney(Transaction transaction){
        Order order = transaction.getOrder();

        String stockMarketAccount;
        try{
            stockMarketAccount = bankClient.getUSDAccountNumberByCompanyId(4L).getBody();
        } catch (Exception e){
            e.printStackTrace();
            finaliseExecution(order);
            return;
        }

        String senderAccount, receiverAccount;
        if(order.getDirection() == OrderDirection.BUY){
            senderAccount = order.getAccountNumber();
            receiverAccount = stockMarketAccount;
        } else {
            senderAccount = stockMarketAccount;
            receiverAccount = order.getAccountNumber();
        }

        TrackedPayment trackedPayment = trackedPaymentService.createTrackedPayment(
                transaction.getId(),
                TrackedPaymentType.ORDER_TRANSACTION
        );

        log.info("Created tracked payment {}", trackedPayment);

        CreatePaymentDto createPaymentDto = CreatePaymentDto.builder()
                .amount(transaction.getTotalPrice())
                .senderAccountNumber(senderAccount)
                .receiverAccountNumber(receiverAccount)
                .callbackId(trackedPayment.getId())
                .purposeOfPayment("Order Transaction")
                .paymentCode("289")
                .build();

        bankClient.executeSystemPayment(ExecutePaymentDto.builder()
                .clientId(order.getUserId())
                .createPaymentDto(createPaymentDto)
                .build()
        );
    }

    public void handleTransactionSuccessfulPayment(Long trackedPaymentId){
        TrackedPayment trackedPayment = trackedPaymentService.getTrackedPayment(trackedPaymentId);
        Long transactionId = trackedPayment.getTrackedEntityId();

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        Order order = transaction.getOrder();
        order.getTransactions().add(transaction);
        order.setTotalPrice(order.getTotalPrice().add(transaction.getTotalPrice()));
        order.setRemainingPortions(order.getRemainingPortions() - transaction.getQuantity());
        order.setLastModification(LocalDateTime.now());

        portfolioService.updateHoldingsOnOrderExecution(transaction);

        if(order.getRemainingPortions() == 0)
            finaliseExecution(order);
        else {
            orderRepository.save(order);
            executeTransaction(order);
        }
    }

    public void handleTransactionFailedPayment(Long trackedPaymentId){
        TrackedPayment trackedPayment = trackedPaymentService.getTrackedPayment(trackedPaymentId);
        Long transactionId = trackedPayment.getTrackedEntityId();

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        transaction.getOrder().getTransactions().remove(transaction);
        transactionRepository.delete(transaction);

        finaliseExecution(transaction.getOrder());
    }

    private void finaliseExecution(Order order){
        if (!order.getUserRole().equals("CLIENT") || order.getRemainingPortions() == order.getQuantity())
            finaliseOrder(order);
        else
            payCommission(order);
    }

    private void payCommission(Order order){
        order.setCommission(getCommission(order.getOrderType(), order.getTotalPrice()));
        order.setStatus(OrderStatus.COMMISSION_PAYMENT_FAILED);
        orderRepository.save(order);

        String bankAccount;
        try{
            bankAccount = bankClient.getUSDAccountNumberByCompanyId(1L).getBody();
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        TrackedPayment trackedPayment = trackedPaymentService.createTrackedPayment(
                order.getId(),
                TrackedPaymentType.ORDER_COMMISSION
        );

        log.info("Created tracked payment {}", trackedPayment);

        CreatePaymentDto createPaymentDto = CreatePaymentDto.builder()
                .amount(order.getCommission())
                .senderAccountNumber(order.getAccountNumber())
                .receiverAccountNumber(bankAccount)
                .callbackId(trackedPayment.getId())
                .purposeOfPayment("Order Commission")
                .paymentCode("289")
                .build();

        System.out.println("receiver bank");
        System.out.println(bankAccount);

        bankClient.executeSystemPayment(ExecutePaymentDto.builder()
                .clientId(order.getUserId())
                .createPaymentDto(createPaymentDto)
                .build()
        );
    }

    public void handleCommissionSuccessfulPayment(Long trackedPaymentId){
        TrackedPayment trackedPayment = trackedPaymentService.getTrackedPayment(trackedPaymentId);
        Long orderId = trackedPayment.getTrackedEntityId();
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        finaliseOrder(order);
    }

    private void finaliseOrder(Order order){
        if (order.getRemainingPortions() == order.getQuantity())
            order.setStatus(OrderStatus.FAILED);
        else if (order.getRemainingPortions() == 0)
            order.setStatus(OrderStatus.DONE);
        else
            order.setStatus(OrderStatus.PARTIAL);

        order.setIsDone(true);
        setOrderProfitAndTax(order);
        orderRepository.save(order);

//        portfolioService.updateHoldingsOnOrderExecution(order);
    }

    private void setOrderProfitAndTax(Order order){
        if (order.getDirection() == OrderDirection.BUY) {
            order.setTaxStatus(TaxStatus.TAXFREE);
            order.setTaxAmount(BigDecimal.ZERO);
            return;
        }
        BigDecimal buyingPrice = BigDecimal.valueOf(order.getQuantity() - order.getRemainingPortions())
                .multiply(order.getAverageBuyingPrice());

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
        return bankClient.convert(
                new ConvertDto("USD", "RSD", orderRepository.getBankProfitFromOrders())
        );
    }
}
