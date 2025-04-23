package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.*;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.OrderMapper;
import rs.raf.stock_service.exceptions.*;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingPriceHistory;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.exceptions.CantCancelOrderInCurrentOrderState;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.OrderService;
import rs.raf.stock_service.service.PortfolioService;
import rs.raf.stock_service.service.TrackedPaymentService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private BankClient bankClient;

    @Mock
    private ListingPriceHistoryRepository listingPriceHistoryRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PortfolioEntryRepository portfolioEntryRepository;

    @Mock
    private TrackedPaymentService trackedPaymentService;

    @InjectMocks
    private OrderService orderService;

    private String authHeader;
    private Long userId;
    private Long orderId;
    private Long listingId;

    private Listing listing;
    private ActuaryLimitDto actuaryLimitDto;
    private PortfolioEntry portfolioEntry;

    private AccountDetailsDto accountDetailsDto;

    private CreateOrderDto createMarketOrderDto;
    private CreateOrderDto createStopOrderDto;
    private CreateOrderDto createLimitOrderDto;
    private CreateOrderDto createStopLimitOrderDto;

    private Order pendingOrder;
    private Order stopOrder;
    private Order limitOrder;
    private Order stopLimitOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        authHeader = "Bearer test-token";
        userId = 1L;
        orderId = 1L;
        listingId = 1L;

        Country country = Country.builder().openTime(LocalTime.of(0, 0))
                .closeTime(LocalTime.of(23, 59)).build();

        Exchange exchange = Exchange.builder().polity(country).timeZone(-5L).build();

        listing = Stock.builder().contractSize(1).volume(500000L).build();
        listing.setId(1L);
        listing.setExchange(exchange);
        listing.setPrice(new BigDecimal(150));


        actuaryLimitDto = new ActuaryLimitDto(new BigDecimal(1000), new BigDecimal(100), true);

        portfolioEntry = PortfolioEntry.builder().id(1L).listing(listing).amount(100).publicAmount(0).reservedAmount(0).build();

        accountDetailsDto = new AccountDetailsDto();
        accountDetailsDto.setAccountNumber("123");
        accountDetailsDto.setAvailableBalance(new BigDecimal(999999));
        accountDetailsDto.setCurrencyCode("USD");

        pendingOrder = Order.builder().id(3L).status(OrderStatus.PENDING).direction(OrderDirection.SELL).contractSize(1).quantity(100)
                .pricePerUnit(new BigDecimal(250)).lastModification(LocalDateTime.now().minusDays(2)).listing(listing).build();

        createMarketOrderDto = new CreateOrderDto(1L, OrderType.MARKET, 100, 1, OrderDirection.BUY,
                "123", false);

        createStopOrderDto = new CreateOrderDto(1L, OrderType.STOP, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(200));

        stopOrder = OrderMapper.toOrder(createStopOrderDto, userId, "ADMIN", listing);
        stopOrder.setId(1L);
        stopOrder.setStatus(OrderStatus.APPROVED);

        createLimitOrderDto = new CreateOrderDto(1L, OrderType.LIMIT, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(100));

        limitOrder = OrderMapper.toOrder(createLimitOrderDto, userId, "ADMIN", listing);
        limitOrder.setId(2L);
        limitOrder.setStatus(OrderStatus.APPROVED);

        createStopLimitOrderDto = new CreateOrderDto(1L, OrderType.STOP_LIMIT, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(100), new BigDecimal(200));

        stopLimitOrder = OrderMapper.toOrder(createStopLimitOrderDto, userId, "ADMIN", listing);
        stopLimitOrder.setId(3L);
        stopLimitOrder.setStatus(OrderStatus.APPROVED);
    }

    @Test
    void testGetOrdersByStatus_WhenStatusIsProvided() {
        ListingPriceHistory dailyPriceInfo = new ListingPriceHistory();

        List<Order> orderList = Arrays.asList(stopOrder, limitOrder);
        Page<Order> orderPage = new PageImpl<>(orderList);

        when(listingPriceHistoryRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(dailyPriceInfo);
        when(orderRepository.findByStatus(OrderStatus.APPROVED, PageRequest.of(1, 10))).thenReturn(orderPage);

        Page<OrderDto> result = orderService.getOrdersByStatus(OrderStatus.APPROVED, PageRequest.of(1, 10));

        assertEquals(2, result.getTotalElements());
        Random random = new Random();
        assertEquals(OrderStatus.APPROVED, result.getContent().get(random.nextInt(0, (int)result.getTotalElements())).getStatus());
        verify(orderRepository, times(1)).findByStatus(eq(OrderStatus.APPROVED), any(PageRequest.class));
    }

    @Test
    void testGetOrdersByStatus_WhenStatusIsNull() {
        List<Order> orderList = Arrays.asList(stopOrder, limitOrder, pendingOrder);
        Page<Order> orderPage = new PageImpl<>(orderList);

        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);

        Page<OrderDto> result = orderService.getOrdersByStatus(null, PageRequest.of(0, 10));

        assertEquals(3, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void approveOrder_ShouldThrowOrderNotFoundException_WhenOrderDoesNotExist() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            orderService.approveOrder(orderId, authHeader);
        });

        assertEquals("Order with ID "+orderId+" not found.", exception.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
        verify(jwtTokenUtil, never()).getUserIdFromAuthHeader(anyString());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void approveOrder_ShouldThrowException_WhenOrderIsNotPending() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(stopOrder));

        CantApproveNonPendingOrder exception = assertThrows(CantApproveNonPendingOrder.class, () -> {
            orderService.approveOrder(orderId, authHeader);
        });

        assertEquals("Order with ID "+orderId+" is not in pending status.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void approveOrder_ShouldThrowPortfolioEntryNotFoundException_WhenPortfolioEntryDoesNotExist() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(portfolioEntryRepository.findByUserIdAndListing(pendingOrder.getUserId(), listing)).thenReturn(Optional.empty());

        PortfolioEntryNotFoundException exception = assertThrows(PortfolioEntryNotFoundException.class, () -> {
            orderService.approveOrder(orderId, authHeader);
        });

        assertEquals("Portfolio entry not found", exception.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
        verify(portfolioEntryRepository, times(1)).findByUserIdAndListing(pendingOrder.getUserId(), listing);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void approveOrder_ShouldThrowPortfolioAmountNotEnoughException_WhenPortfolioAvailableAmountLessThanOrderAmount() {
        portfolioEntry.setReservedAmount(1);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(portfolioEntryRepository.findByUserIdAndListing(pendingOrder.getUserId(), listing)).thenReturn(Optional.ofNullable(portfolioEntry));

        PortfolioAmountNotEnoughException exception = assertThrows(PortfolioAmountNotEnoughException.class, () -> {
            orderService.approveOrder(orderId, authHeader);
        });

        assertEquals("Portfolio available amount of " + portfolioEntry.getAvailableAmount() +
                " not enough to cover amount of " + pendingOrder.getContractSize() * pendingOrder.getQuantity() + ".", exception.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
        verify(portfolioEntryRepository, times(1)).findByUserIdAndListing(pendingOrder.getUserId(), listing);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void approveOrder_ShouldApproveOrder_WhenOrderIsPending() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(portfolioEntryRepository.findByUserIdAndListing(pendingOrder.getUserId(), listing)).thenReturn(Optional.ofNullable(portfolioEntry));

        orderService.approveOrder(orderId, authHeader);

        verify(portfolioEntryRepository, times(1)).save(portfolioEntry);
        assertEquals(0, portfolioEntry.getAvailableAmount());

        assertEquals(OrderStatus.APPROVED, pendingOrder.getStatus());
        assertEquals(userId, pendingOrder.getApprovedBy());
        verify(orderRepository, times(1)).save(pendingOrder);
    }

    @Test
    void declineOrder_ShouldThrowOrderNotFoundException_WhenOrderDoesNotExist() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            orderService.declineOrder(orderId, authHeader);
        });

        assertEquals("Order with ID "+orderId+" not found.", exception.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
        verify(jwtTokenUtil, never()).getUserIdFromAuthHeader(anyString());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void declineOrder_ShouldThrowException_WhenOrderIsNotPending() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(stopOrder));

        CantApproveNonPendingOrder exception = assertThrows(CantApproveNonPendingOrder.class, () -> {
            orderService.declineOrder(orderId, authHeader);
        });

        assertEquals("Order with ID "+orderId+" is not in pending status.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void declineOrder_ShouldDeclineOrder_WhenOrderIsPending() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);

        orderService.declineOrder(orderId, authHeader);

        assertEquals(OrderStatus.DECLINED, pendingOrder.getStatus());
        assertEquals(userId, pendingOrder.getApprovedBy());
        verify(orderRepository, times(1)).save(pendingOrder);
    }

    @Test
    void createOrder_ShouldApproveOrder_WhenUserIsClient() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);

        // Act
        orderService.createOrder(createStopOrderDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.APPROVED) &&
                        order.getUserId().equals(userId) &&
                        order.getListing().getId().equals(listing.getId())
        ));
    }

    @Test
    void createOrder_ShouldApproveOrder_WhenUserIsSupervisor() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("SUPERVISOR");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);

        // Act
        orderService.createOrder(createStopOrderDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.APPROVED) &&
                        order.getUserId().equals(userId) &&
                        order.getListing().getId().equals(listing.getId())
        ));
    }

    @Test
    void createOrder_ShouldApproveOrder_WhenUserIsAdmin() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);

        // Act
        orderService.createOrder(createStopOrderDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.APPROVED) &&
                        order.getUserId().equals(userId) &&
                        order.getListing().getId().equals(listing.getId())
        ));
    }

    @Test
    void createOrder_ShouldSetOrderStatusToPending_WhenActuaryApprovalIsNeeded() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("AGENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userClient.getActuaryByEmployeeId(userId)).thenReturn(actuaryLimitDto);
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);

        // Act
        orderService.createOrder(createStopOrderDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.PENDING) &&
                        order.getUserId().equals(userId) &&
                        order.getListing().getId().equals(listing.getId())
        ));
    }

    @Test
    void createOrder_ShouldThrowStopPriceMissingException() {
        createStopOrderDto.setStopPrice(null);
        StopPriceMissingException exception = assertThrows(StopPriceMissingException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Stop price cannot be null for " +  createStopOrderDto.getOrderType() + " orders.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }
    @Test
    void createOrder_ShouldThrowLimitPriceMissingException() {
        createStopOrderDto.setOrderType(OrderType.LIMIT);
        LimitPriceMissingException exception = assertThrows(LimitPriceMissingException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Limit price cannot be null for " +  createStopOrderDto.getOrderType() + " orders.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowListingNotFoundException_WhenListingDoesNotExist() {
        // Arrange
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        ListingNotFoundException exception = assertThrows(ListingNotFoundException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Listing with ID 1 not found.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        // Arrange
        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(null);

        // Act & Assert
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Account " + createStopOrderDto.getAccountNumber() + " not found", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowWrongCurrencyAccountException_WhenAccountIsNotUSD() {
        // Arrange
        accountDetailsDto.setCurrencyCode("RSD");
        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);

        // Act & Assert
        WrongCurrencyAccountException exception = assertThrows(WrongCurrencyAccountException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Account not in USD.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createBuyOrder_ShouldThrowInsufficientFundsException_WhenAvailableBalanceLessThanTotalPrice() {
        accountDetailsDto.setAvailableBalance(new BigDecimal(100));

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);


        assertThrows(InsufficientFundsException.class, () -> orderService.createOrder(createStopOrderDto, authHeader));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createSellOrder_ShouldThrowPortfolioNotFoundException_WhenPortfolioEntryDoesNotExist() {
        createStopOrderDto.setOrderDirection(OrderDirection.SELL);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);
        when(portfolioEntryRepository.findByUserIdAndListing(userId, listing)).thenReturn(Optional.empty());

        PortfolioEntryNotFoundException exception = assertThrows(PortfolioEntryNotFoundException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Portfolio entry not found", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createSellOrder_ShouldThrowPortfolioAmountNotEnoughException_WhenPortfolioEntryDoesNotExist() {
        createStopOrderDto.setOrderDirection(OrderDirection.SELL);
        portfolioEntry.setAmount(90);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);
        when(portfolioEntryRepository.findByUserIdAndListing(userId, listing)).thenReturn(Optional.ofNullable(portfolioEntry));

        PortfolioAmountNotEnoughException exception = assertThrows(PortfolioAmountNotEnoughException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Portfolio available amount of " + portfolioEntry.getAmount() + " not enough to cover amount of "
                + createStopOrderDto.getContractSize() * createStopOrderDto.getQuantity() + ".", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowActuaryLimitNotFound() {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("AGENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);
        doThrow(new ActuaryLimitNotFoundException(userId)).when(userClient).getActuaryByEmployeeId(userId);

        ActuaryLimitNotFoundException exception = assertThrows(ActuaryLimitNotFoundException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Actuary limit with employeeId: " + userId + " is not found.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createMarketOrderAndExecuteFirstTransaction() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);
        when(bankClient.getUSDAccountNumberByCompanyId(4L)).thenReturn(ResponseEntity.ok("1"));
        when(trackedPaymentService.createTrackedPayment(any(), any())).thenReturn(new TrackedPayment());

        // Act
        OrderDto orderDto = orderService.createOrder(createMarketOrderDto, authHeader);

        // Assert
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(bankClient, times(1)).getUSDAccountNumberByCompanyId(4L);
        verify(bankClient, times(1)).executeSystemPayment(any(ExecutePaymentDto.class));

        assertEquals(OrderStatus.PROCESSING, orderDto.getStatus());
        assertEquals(false, orderDto.getIsDone());
    }

    @Test
    void createAndExecuteAllOrNoneMarketOrder() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");
        when(bankClient.getAccountDetails(createStopOrderDto.getAccountNumber())).thenReturn(accountDetailsDto);
        when(bankClient.getUSDAccountNumberByCompanyId(4L)).thenReturn(ResponseEntity.ok("1"));
        when(trackedPaymentService.createTrackedPayment(any(), any())).thenReturn(new TrackedPayment());

        // Act
        OrderDto orderDto = orderService.createOrder(createMarketOrderDto, authHeader);

        // Assert
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(bankClient, times(1)).getUSDAccountNumberByCompanyId(4L);
        verify(bankClient, times(1)).executeSystemPayment(any(ExecutePaymentDto.class));

        assertEquals(OrderStatus.PROCESSING, orderDto.getStatus());
        assertEquals(false, orderDto.getIsDone());
    }


    @Test
    void executeStopOrder() {
        when(orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.STOP))
                .thenReturn(Arrays.asList(stopOrder));
        when(bankClient.getUSDAccountNumberByCompanyId(4L)).thenReturn(ResponseEntity.ok("1"));
        when(trackedPaymentService.createTrackedPayment(any(), any())).thenReturn(new TrackedPayment());

        orderService.checkOrders();

        verify(orderRepository, never()).save(stopOrder);
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(bankClient, never()).getUSDAccountNumberByClientId(any());
        verify(bankClient, never()).executeSystemPayment(any(ExecutePaymentDto.class));

        assertEquals(false, stopOrder.getIsDone());
        assertEquals(OrderStatus.APPROVED, stopOrder.getStatus());

        listing.setPrice(new BigDecimal(250));
        orderService.checkOrders();

        // Assert
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(bankClient, times(1)).getUSDAccountNumberByCompanyId(4L);
        verify(bankClient, times(1)).executeSystemPayment(any(ExecutePaymentDto.class));

        assertEquals(OrderStatus.PROCESSING, stopOrder.getStatus());
        assertEquals(false, stopOrder.getIsDone());
    }

    @Test
    void executeLimitOrder() {
        when(orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.LIMIT))
                .thenReturn(Arrays.asList(limitOrder));
        when(bankClient.getUSDAccountNumberByCompanyId(4L)).thenReturn(ResponseEntity.ok("1"));
        when(trackedPaymentService.createTrackedPayment(any(), any())).thenReturn(new TrackedPayment());

        orderService.checkOrders();

        verify(orderRepository, never()).save(limitOrder);
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(bankClient, never()).getUSDAccountNumberByClientId(any());
        verify(bankClient, never()).executeSystemPayment(any(ExecutePaymentDto.class));

        assertEquals(false, limitOrder.getIsDone());
        assertEquals(OrderStatus.APPROVED, limitOrder.getStatus());

        //menjamo cenu da se uslov ispuni pri sledecoj proveri
        listing.setPrice(new BigDecimal(50));
        orderService.checkOrders();

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(bankClient, times(1)).getUSDAccountNumberByCompanyId(4L);
        verify(bankClient, times(1)).executeSystemPayment(any(ExecutePaymentDto.class));

        assertEquals(OrderStatus.PROCESSING, limitOrder.getStatus());
        assertEquals(false, limitOrder.getIsDone());
    }

    @Test
    void executeStopLimitOrder() {
        when(orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.STOP_LIMIT))
                .thenReturn(Arrays.asList(stopLimitOrder));
        when(bankClient.getUSDAccountNumberByCompanyId(4L)).thenReturn(ResponseEntity.ok("1"));
        when(trackedPaymentService.createTrackedPayment(any(), any())).thenReturn(new TrackedPayment());

        orderService.checkOrders();

        verify(orderRepository, never()).save(stopLimitOrder);
        assertEquals(false, stopLimitOrder.isStopFulfilled());
        assertEquals(false, stopLimitOrder.getIsDone());

        //menjamo cenu da se stop uslov ispuni pri sledecoj proveri
        listing.setPrice(new BigDecimal(250));
        orderService.checkOrders();

        verify(orderRepository, atLeast(1)).save(stopLimitOrder);
        assertEquals(true, stopLimitOrder.isStopFulfilled());
        assertEquals(false, stopLimitOrder.getIsDone());

        //menjamo cenu da se limit uslov ispuni pri sledecoj proveri
        listing.setPrice(new BigDecimal(50));
        orderService.checkOrders();

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(bankClient, times(1)).getUSDAccountNumberByCompanyId(4L);
        verify(bankClient, times(1)).executeSystemPayment(any(ExecutePaymentDto.class));

        assertEquals(OrderStatus.PROCESSING, stopLimitOrder.getStatus());
        assertEquals(false, stopLimitOrder.getIsDone());
    }

    @Test
    void shouldReturnOrderDtosWhenUserIsAuthorized() {
        Long userId = 1L;
        String authHeader = "Bearer valid-token";

        Listing listing = new Stock();
        listing.setId(10L);
        listing.setTicker("AAPL");

        ListingPriceHistory listingPriceHistory = new ListingPriceHistory();
        listingPriceHistory.setDate(LocalDateTime.now());

        Order order = new Order();
        order.setId(101L);
        order.setUserId(userId);
        order.setListing(listing);
        order.setQuantity(10);
        order.setOrderType(OrderType.MARKET);
        order.setAccountNumber("123456789012345678");

        ListingDto listingDto = new ListingDto(
                10L,
                ListingType.STOCK,
                "AAPL",
                new BigDecimal("150.25"),
                new BigDecimal("2.75"),
                5000000L,
                new BigDecimal("1000.00"),
                "XNAS",
                new BigDecimal("150.50"),
                null
        );

        OrderDto orderDto = new OrderDto();
        orderDto.setId(101L);
        orderDto.setUserId(userId);
        orderDto.setListing(listingDto);
        orderDto.setClientName("Test Korisnik");
        orderDto.setAccountNumber("123456789012345678");

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("USER");
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order));
        when(listingPriceHistoryRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(listingPriceHistory);
        when(listingMapper.toDto(listing, listingPriceHistory)).thenReturn(listingDto);

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.toDto(any(), any(), any(), any()))
                    .thenReturn(orderDto);

            List<OrderDto> result = orderService.getOrdersByUser(userId, authHeader);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(listingDto, result.get(0).getListing());
            assertEquals(userId, result.get(0).getUserId());
            assertEquals("Test Korisnik", result.get(0).getClientName());
            assertEquals("123456789012345678", result.get(0).getAccountNumber());
        }
    }

    @Test
    void shouldReturnOrderDtosWhenUserIsSupervisor() {
        Long userId = 1L;
        Long supervisorId = 2L;
        String authHeader = "Bearer supervisor-token";

        Listing listing = new Stock();
        listing.setId(10L);
        listing.setTicker("AAPL");

        ListingPriceHistory listingPriceHistory = new ListingPriceHistory();
        listingPriceHistory.setDate(LocalDateTime.now());

        Order order = new Order();
        order.setId(101L);
        order.setUserId(userId);
        order.setListing(listing);
        order.setQuantity(10);
        order.setOrderType(OrderType.MARKET);
        order.setAccountNumber("111111111111111111");

        ListingDto listingDto = new ListingDto(
                10L,
                ListingType.STOCK,
                "AAPL",
                new BigDecimal("150.25"),
                new BigDecimal("2.75"),
                5000000L,
                new BigDecimal("1000.00"),
                "XNAS",
                new BigDecimal("150.50"),
                null
        );

        OrderDto orderDto = new OrderDto();
        orderDto.setId(101L);
        orderDto.setUserId(userId);
        orderDto.setListing(listingDto);
        orderDto.setClientName("Test Ime");
        orderDto.setAccountNumber("111111111111111111");

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(supervisorId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("SUPERVISOR");
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order));
        when(listingPriceHistoryRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(listingPriceHistory);
        when(listingMapper.toDto(listing, listingPriceHistory)).thenReturn(listingDto);

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.toDto(any(), any(), any(), any()))
                    .thenReturn(orderDto);

            List<OrderDto> result = orderService.getOrdersByUser(userId, authHeader);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(listingDto, result.get(0).getListing());
            assertEquals(userId, result.get(0).getUserId());
            assertEquals("Test Ime", result.get(0).getClientName());
            assertEquals("111111111111111111", result.get(0).getAccountNumber());
        }
    }

    @Test
    void shouldReturnOrderDtosWhenUserIsAdmin() {
        Long userId = 1L;
        Long adminId = 3L;
        String authHeader = "Bearer admin-token";

        Listing listing = new Stock();
        listing.setId(10L);
        listing.setTicker("AAPL");

        ListingPriceHistory listingPriceHistory = new ListingPriceHistory();
        listingPriceHistory.setDate(LocalDateTime.now());

        Order order = new Order();
        order.setId(101L);
        order.setUserId(userId);
        order.setListing(listing);
        order.setQuantity(10);
        order.setOrderType(OrderType.MARKET);
        order.setAccountNumber("987654321000000000");

        ListingDto listingDto = new ListingDto(
                10L,
                ListingType.STOCK,
                "AAPL",
                new BigDecimal("150.25"),
                new BigDecimal("2.75"),
                5000000L,
                new BigDecimal("1000.00"),
                "XNAS",
                new BigDecimal("150.50"),
                null
        );

        OrderDto orderDto = new OrderDto();
        orderDto.setId(101L);
        orderDto.setUserId(userId);
        orderDto.setListing(listingDto);
        orderDto.setClientName("Admin Test");
        orderDto.setAccountNumber("987654321000000000");

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(adminId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order));
        when(listingPriceHistoryRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(listingPriceHistory);
        when(listingMapper.toDto(listing, listingPriceHistory)).thenReturn(listingDto);

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.toDto(any(), any(), any(), any()))
                    .thenReturn(orderDto);

            List<OrderDto> result = orderService.getOrdersByUser(userId, authHeader);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(listingDto, result.get(0).getListing());
            assertEquals(userId, result.get(0).getUserId());
            assertEquals("Admin Test", result.get(0).getClientName());
            assertEquals("987654321000000000", result.get(0).getAccountNumber());
        }
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoOrders() {
        Long userId = 1L;
        String authHeader = "Bearer valid-token";

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("USER");
        when(orderRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        List<OrderDto> result = orderService.getOrdersByUser(userId, authHeader);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullListingGracefully() {
        Long userId = 1L;
        String authHeader = "Bearer valid-token";

        Order order = new Order();
        order.setId(1L);
        order.setUserId(userId);
        order.setListing(null);
        order.setAccountNumber("123456789");
        order.setUserRole("CLIENT");
        order.setTransactions(new ArrayList<>());

        OrderDto expectedOrderDto = new OrderDto();
        expectedOrderDto.setId(1L);
        expectedOrderDto.setUserId(userId);
        expectedOrderDto.setListing(null);
        expectedOrderDto.setClientName("N/A");
        expectedOrderDto.setAccountNumber("123456789");

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("USER");
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order));
        when(listingPriceHistoryRepository.findTopByListingOrderByDateDesc(null)).thenReturn(null);
        when(listingMapper.toDto(null, null)).thenReturn(null);

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.toDto(any(), any(), any(), any()))
                    .thenReturn(expectedOrderDto);

            List<OrderDto> result = orderService.getOrdersByUser(userId, authHeader);

            assertNotNull(result); // lista nije null
            assertEquals(1, result.size()); // sadrži jedan OrderDto
            assertNotNull(result.get(0)); // sam OrderDto nije null
            assertNull(result.get(0).getListing()); // ali listing jeste
            assertEquals("N/A", result.get(0).getClientName());
            assertEquals("123456789", result.get(0).getAccountNumber());
        }
    }


    @Test
    void shouldThrowUnauthorizedExceptionWhenUserIsNotAuthorized() {
        Long userId = 1L;
        Long differentUserId = 2L;
        String authHeader = "Bearer different-user-token";

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(differentUserId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("USER");

        assertThrows(UnauthorizedException.class, () -> orderService.getOrdersByUser(userId, authHeader));
    }

    @Test
    void shouldCancelOrder_WhenUserIsOwnerAndStatusIsPending() {
        Long orderId = 1L;
        Long userId = 100L;
        String authHeader = "Bearer token";

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setIsDone(false);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId, authHeader);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldCancelOrder_WhenUserIsAdminCancellingOthersOrder() {
        Long orderId = 2L;
        String authHeader = "Bearer token";

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(123L);
        order.setStatus(OrderStatus.APPROVED);
        order.setIsDone(false);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(999L); // different user
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId, authHeader);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldThrowCantCancelOrderInCurrentState_WhenOrderIsDone() {
        Long orderId = 3L;
        String authHeader = "Bearer token";
        Long userId = 123L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.DONE);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(CantCancelOrderInCurrentOrderState.class,
                () -> orderService.cancelOrder(orderId, authHeader));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowUnauthorizedException_WhenUserTriesToCancelOthersOrder() {
        Long orderId = 4L;
        String authHeader = "Bearer token";

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(100L);
        order.setStatus(OrderStatus.PENDING);
        order.setIsDone(false);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(200L);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedException.class,
                () -> orderService.cancelOrder(orderId, authHeader));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowOrderNotFoundException_WhenOrderDoesNotExist() {
        Long orderId = 99L;
        String authHeader = "Bearer token";

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.cancelOrder(orderId, authHeader));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleCommissionSuccessfulPayment_ShouldFinalizeOrder() {
        Long trackedPaymentId = 1L;
        Long orderId = 2L;
        Long userId = 3L;

        // Set up Order and related objects
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setUserRole("CLIENT"); // Bitno zbog logike u finaliseExecution
        order.setQuantity(10);
        order.setRemainingPortions(0); // Da bi ispunio uslov za DONE
        order.setTotalPrice(BigDecimal.valueOf(1000));
        order.setListing(listing);
        order.setTransactions(new ArrayList<>());
        order.setContractSize(1);

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setId(trackedPaymentId);
        trackedPayment.setTrackedEntityId(orderId);

        PortfolioEntry portfolioEntry = new PortfolioEntry();
        portfolioEntry.setId(1L);
        portfolioEntry.setUserId(userId);
        portfolioEntry.setListing(listing);
        portfolioEntry.setAveragePrice(new BigDecimal("50"));

        // Mockovi
        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(portfolioEntryRepository.findByUserIdAndListing(userId, listing)).thenReturn(Optional.of(portfolioEntry));
        when(bankClient.convert(any(ConvertDto.class))).thenReturn(BigDecimal.valueOf(1500)); // simulate RSD profit

        // Poziv
        orderService.handleCommissionSuccessfulPayment(trackedPaymentId);

        // Verifikacije
        verify(orderRepository, times(1)).save(order);
        verify(portfolioService, times(1)).updateHoldingsOnOrderExecution(order);

        // Asercije
        assertEquals(OrderStatus.DONE, order.getStatus());
        assertTrue(order.getIsDone());
        assertEquals(TaxStatus.PENDING, order.getTaxStatus());
        assertEquals(0, BigDecimal.valueOf(75.00).compareTo(order.getTaxAmount()));
    }

    @Test
    void handleTransactionSuccessfulPayment_ShouldFinalizeOrContinueExecution() {
        Order order = Order.builder()
                .id(1L)
                .userId(userId)
                .status(OrderStatus.PROCESSING)
                .userRole("CLIENT")
                .contractSize(1)
                .quantity(10)
                .remainingPortions(5)
                .totalPrice(BigDecimal.ZERO)
                .listing(listing)
                .transactions(new ArrayList<>())
                .build();

        Transaction transaction = new Transaction(5, new BigDecimal("100"), new BigDecimal("500"), order);
        transaction.setId(100L);

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(transaction.getId());

        when(trackedPaymentService.getTrackedPayment(anyLong())).thenReturn(trackedPayment);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.handleTransactionSuccessfulPayment(1L);

        verify(orderRepository).save(any(Order.class));
        verify(transactionRepository).findById(transaction.getId());
    }

    @Test
    void getCommissionProfit_ShouldReturnExpectedProfit() {
        BigDecimal expectedProfit = new BigDecimal("1234.56");

        when(orderRepository.getBankProfitFromOrders()).thenReturn(expectedProfit);

        BigDecimal result = orderService.getCommissionProfit();

        assertEquals(expectedProfit, result);
        verify(orderRepository, times(1)).getBankProfitFromOrders();
    }

    @Test
    void payCommission_ShouldCalculateCommissionCorrectly() {
        // Arrange
        Long userId = 1L;
        String accountNumber = "123-456";
        Order order = new Order();
        order.setId(1L);
        order.setUserId(userId);
        order.setUserRole("CLIENT");
        order.setAccountNumber(accountNumber);
        order.setOrderType(OrderType.MARKET);
        order.setDirection(OrderDirection.BUY);
        order.setQuantity(10);
        order.setContractSize(1);
        order.setPricePerUnit(new BigDecimal("100"));
        order.setStatus(OrderStatus.PROCESSING);
        order.setTotalPrice(new BigDecimal("500")); // cena transakcije

        // Očekivana provizija za MARKET order je: 500 * 0.14 = 70, ali min(70, 7) = 7.00
        String expectedCommission = "7";

        // Mock - da ne poziva pravi bankClient i trackedPaymentService
        when(bankClient.getUSDAccountNumberByCompanyId(1L)).thenReturn(ResponseEntity.ok("bank-account-USD"));

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setId(1L);
        trackedPayment.setType(TrackedPaymentType.ORDER_COMMISSION);
        trackedPayment.setTrackedEntityId(order.getId());
        trackedPayment.setCreatedAt(LocalDateTime.now());
        when(trackedPaymentService.createTrackedPayment(order.getId(), TrackedPaymentType.ORDER_COMMISSION)).thenReturn(trackedPayment);

        // Act - direktno pozivamo privatnu metodu preko refleksije
        try {
            Method method = OrderService.class.getDeclaredMethod("payCommission", Order.class);
            method.setAccessible(true);
            method.invoke(orderService, order);
        } catch (Exception e) {
            fail("Reflection error: " + e.getMessage());
        }

        // Assert - proveravamo da je komisija postavljena kako treba
        assertEquals(new BigDecimal(expectedCommission), order.getCommission());
        assertEquals(OrderStatus.COMMISSION_PAYMENT_FAILED, order.getStatus());

        // Provera da je pozvan bankClient za slanje provizije
        verify(bankClient, times(1)).executeSystemPayment(any(ExecutePaymentDto.class));
        verify(orderRepository, times(1)).save(order);
    }


    @Test
    void setOrderProfitAndTax_shouldSetTaxFreeForBuyOrder() throws Exception {
        Order order = new Order();
        order.setUserId(userId);
        order.setListing(listing);
        order.setDirection(OrderDirection.BUY);

        Method method = OrderService.class.getDeclaredMethod("setOrderProfitAndTax", Order.class);
        method.setAccessible(true);
        method.invoke(orderService, order);

        assertEquals(TaxStatus.TAXFREE, order.getTaxStatus());
        assertEquals(BigDecimal.ZERO, order.getTaxAmount());
    }

    @Test
    void setOrderProfitAndTax_shouldSetTaxFreeWhenProfitZeroOrNegative() throws Exception {
        Order order = new Order();
        order.setUserId(userId);
        order.setListing(listing);
        order.setDirection(OrderDirection.SELL);
        order.setContractSize(1);
        order.setQuantity(10);
        order.setRemainingPortions(0);
        order.setTotalPrice(new BigDecimal("500"));

        PortfolioEntry portfolioEntry = new PortfolioEntry();
        portfolioEntry.setAveragePrice(new BigDecimal("90"));

        when(portfolioEntryRepository.findByUserIdAndListing(userId, listing)).thenReturn(Optional.of(portfolioEntry));
        when(bankClient.convert(any())).thenReturn(new BigDecimal("-100")); // negative profit

        Method method = OrderService.class.getDeclaredMethod("setOrderProfitAndTax", Order.class);
        method.setAccessible(true);
        method.invoke(orderService, order);

        assertEquals(TaxStatus.TAXFREE, order.getTaxStatus());
        assertEquals(BigDecimal.ZERO, order.getTaxAmount());
    }

    @Test
    void handleCommissionSuccessfulPayment_shouldCalculateTaxCorrectly() {
        // Arrange
        Long trackedPaymentId = 1L;
        Long orderId = 2L;

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(100L);
        order.setDirection(OrderDirection.SELL);
        order.setQuantity(10);
        order.setContractSize(1);
        order.setRemainingPortions(0);
        order.setTotalPrice(new BigDecimal("3000"));
        order.setTransactions(new ArrayList<>());

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(orderId);

        PortfolioEntry entry = new PortfolioEntry();
        entry.setAveragePrice(new BigDecimal("200")); // total cost = 2000

        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(portfolioEntryRepository.findByUserIdAndListing(order.getUserId(), order.getListing()))
                .thenReturn(Optional.of(entry));
        when(bankClient.convert(any(ConvertDto.class))).thenReturn(new BigDecimal("1000"));

        // Act
        orderService.handleCommissionSuccessfulPayment(trackedPaymentId);

        // Assert
        assertEquals(new BigDecimal("150.00"), order.getTaxAmount());
        assertEquals(TaxStatus.PENDING, order.getTaxStatus());
    }
}
