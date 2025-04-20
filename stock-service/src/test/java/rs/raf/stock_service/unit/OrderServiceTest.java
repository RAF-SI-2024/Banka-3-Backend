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
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.ActuaryLimitDto;
import rs.raf.stock_service.domain.dto.CreateOrderDto;
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.dto.TransactionDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.OrderMapper;
import rs.raf.stock_service.exceptions.*;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingPriceHistory;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.exceptions.CantCancelOrderInCurrentOrderState;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.OrderService;
import rs.raf.stock_service.service.PortfolioService;
import rs.raf.stock_service.utils.JwtTokenUtil;

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

    @InjectMocks
    private OrderService orderService;

    private String authHeader;
    private Long userId;
    private Long orderId;
    private Long listingId;

    private Listing listing;
    private ActuaryLimitDto actuaryLimitDto;

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

        pendingOrder = Order.builder().id(3L).status(OrderStatus.PENDING).contractSize(1).quantity(5)
                .pricePerUnit(new BigDecimal(250)).lastModification(LocalDateTime.now().minusDays(2)).listing(listing).build();

        createMarketOrderDto = new CreateOrderDto(1L, OrderType.MARKET, 100, 1, OrderDirection.BUY,
                "123", false);

        createStopOrderDto = new CreateOrderDto(1L, OrderType.STOP, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(200));

        stopOrder = OrderMapper.toOrder(createStopOrderDto, userId, "ADMIN", listing);
        stopOrder.setId(1L);
        stopOrder.setStatus(OrderStatus.APPROVED);
        stopOrder.setTotalPrice(stopOrder.getPricePerUnit().multiply(BigDecimal.valueOf(stopOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(stopOrder.getContractSize())));

        createLimitOrderDto = new CreateOrderDto(1L, OrderType.LIMIT, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(100));

        limitOrder = OrderMapper.toOrder(createLimitOrderDto, userId, "ADMIN", listing);
        limitOrder.setId(2L);
        limitOrder.setStatus(OrderStatus.APPROVED);
        limitOrder.setTotalPrice(limitOrder.getPricePerUnit().multiply(BigDecimal.valueOf(limitOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(limitOrder.getContractSize())));

        createStopLimitOrderDto = new CreateOrderDto(1L, OrderType.STOP_LIMIT, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(100), new BigDecimal(200));

        stopLimitOrder = OrderMapper.toOrder(createStopLimitOrderDto, userId, "ADMIN", listing);
        stopLimitOrder.setId(3L);
        stopLimitOrder.setStatus(OrderStatus.APPROVED);
        stopLimitOrder.setTotalPrice(stopLimitOrder.getPricePerUnit().multiply(BigDecimal.valueOf(stopLimitOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(stopLimitOrder.getContractSize())));
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
    void approveOrder_ShouldApproveOrder_WhenOrderIsPending() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);

        orderService.approveOrder(orderId, authHeader);

        assertEquals(OrderStatus.APPROVED, pendingOrder.getStatus());
        assertEquals(userId, pendingOrder.getApprovedBy());
        verify(orderRepository, times(1)).save(pendingOrder);
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
    void createOrder_ShouldThrowListingNotFoundException_WhenListingDoesNotExist() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        ListingNotFoundException exception = assertThrows(ListingNotFoundException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Listing with ID 1 not found.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowAccountNotFoundException() {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doThrow(new AccountNotFoundException(createStopOrderDto.getAccountNumber()))
                .when(bankClient).updateAvailableBalance(anyString(), any(BigDecimal.class));

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Account " + createStopOrderDto.getAccountNumber() + " not found", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldDeclineOrder_WhenInsufficientFunds() {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doThrow(new InsufficientFundsException(BigDecimal.valueOf(100)))
                .when(bankClient).updateAvailableBalance(anyString(), any(BigDecimal.class));

        OrderDto orderDto = orderService.createOrder(createStopOrderDto, authHeader);

        assertEquals(OrderStatus.DECLINED, orderDto.getStatus());
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void createOrder_ShouldThrowActuaryLimitNotFound() {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("AGENT");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doThrow(new ActuaryLimitNotFoundException(userId)).when(userClient).getActuaryByEmployeeId(userId);

        ActuaryLimitNotFoundException exception = assertThrows(ActuaryLimitNotFoundException.class, () -> {
            orderService.createOrder(createStopOrderDto, authHeader);
        });

        assertEquals("Actuary limit with employeeId: " + userId + " is not found.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
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
    void createAndExecuteMarketOrder() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");

        // Act
        OrderDto orderDto = orderService.createOrder(createMarketOrderDto, authHeader);

        // Assert
        verify(orderRepository, atLeast(4)).save(any(Order.class));
        assertEquals(OrderStatus.DONE, orderDto.getStatus());
        assertEquals(true, orderDto.getIsDone());
        assertEquals(0, orderDto.getRemainingPortions());

        BigDecimal price = BigDecimal.valueOf(orderDto.getContractSize()).multiply(BigDecimal.valueOf(orderDto.getQuantity()))
                .multiply(orderDto.getPricePerUnit());
        BigDecimal totalPrice = BigDecimal.ZERO;
        int quantity = 0;
        for(TransactionDto transactionDto : orderDto.getTransactions()){
            totalPrice = totalPrice.add(transactionDto.getTotalPrice());
            quantity += transactionDto.getQuantity();
        }
        verify(transactionRepository, atLeast(orderDto.getTransactions().size())).save(any(Transaction.class));
        assertEquals(orderDto.getQuantity(), quantity);
        assertEquals(price, totalPrice);
    }

    @Test
    void createAndExecuteAllOrNoneMarketOrder() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");

        // Act
        createMarketOrderDto.setAllOrNone(true);
        OrderDto orderDto = orderService.createOrder(createMarketOrderDto, authHeader);

        // Assert
        verify(orderRepository, atLeast(3)).save(any(Order.class));
        assertEquals(OrderStatus.DONE, orderDto.getStatus());
        assertEquals(true, orderDto.getIsDone());
        assertEquals(0, orderDto.getRemainingPortions());
        assertEquals(1, orderDto.getTransactions().size());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }


    @Test
    void executeStopOrder() {
        when(orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.STOP))
                .thenReturn(Arrays.asList(stopOrder));

        orderService.checkOrders();

        verify(orderRepository, never()).save(stopOrder);
        assertEquals(false, stopOrder.isStopFulfilled());
        assertEquals(false, stopOrder.getIsDone());

        //menjamo cenu da se uslov ispuni pri sledecoj proveri
        listing.setPrice(new BigDecimal(250));
        orderService.checkOrders();

        verify(orderRepository, atLeast(3)).save(stopOrder);
        assertEquals(true, stopOrder.isStopFulfilled());
        assertEquals(OrderStatus.DONE, stopOrder.getStatus());
        assertEquals(true, stopOrder.getIsDone());
        assertEquals(0, stopOrder.getRemainingPortions());

        BigDecimal totalPrice = BigDecimal.ZERO;
        int quantity = 0;
        for(Transaction transaction : stopOrder.getTransactions()){
            totalPrice = totalPrice.add(transaction.getTotalPrice());
            quantity += transaction.getQuantity();
        }

        verify(transactionRepository, atLeast(stopOrder.getTransactions().size())).save(any(Transaction.class));
        assertEquals(stopOrder.getQuantity(), quantity);
        assertEquals(stopOrder.getPricePerUnit().multiply(BigDecimal.valueOf(limitOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(limitOrder.getContractSize())), totalPrice);
    }

    @Test
    void executeLimitOrder() {
        when(orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.LIMIT))
                .thenReturn(Arrays.asList(limitOrder));

        orderService.checkOrders();

        verify(orderRepository, never()).save(limitOrder);
        assertEquals(false, limitOrder.getIsDone());

        //menjamo cenu da se uslov ispuni pri sledecoj proveri
        listing.setPrice(new BigDecimal(50));
        orderService.checkOrders();

        verify(orderRepository, atLeast(3)).save(limitOrder);
        assertEquals(OrderStatus.DONE, limitOrder.getStatus());
        assertEquals(true, limitOrder.getIsDone());
        assertEquals(0, limitOrder.getRemainingPortions());
        assertEquals(listing.getPrice(), limitOrder.getPricePerUnit());

        BigDecimal totalPrice = BigDecimal.ZERO;
        int quantity = 0;
        for(Transaction transaction : limitOrder.getTransactions()){
            totalPrice = totalPrice.add(transaction.getTotalPrice());
            quantity += transaction.getQuantity();
        }

        verify(transactionRepository, atLeast(limitOrder.getTransactions().size())).save(any(Transaction.class));
        assertEquals(stopOrder.getQuantity(), quantity);
        assertEquals(limitOrder.getPricePerUnit().multiply(BigDecimal.valueOf(limitOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(limitOrder.getContractSize())), totalPrice);
    }

    @Test
    void executeStopLimitOrder() {
        when(orderRepository.findByIsDoneAndStatusAndOrderType(false, OrderStatus.APPROVED, OrderType.STOP_LIMIT))
                .thenReturn(Arrays.asList(stopLimitOrder));

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

        verify(orderRepository, atLeast(3)).save(stopLimitOrder);
        assertEquals(OrderStatus.DONE, stopLimitOrder.getStatus());
        assertEquals(true, stopLimitOrder.getIsDone());
        assertEquals(0, stopLimitOrder.getRemainingPortions());
        assertEquals(listing.getPrice(), stopLimitOrder.getPricePerUnit());

        BigDecimal totalPrice = BigDecimal.ZERO;
        int quantity = 0;
        for(Transaction transaction : stopLimitOrder.getTransactions()){
            totalPrice = totalPrice.add(transaction.getTotalPrice());
            quantity += transaction.getQuantity();
        }

        verify(transactionRepository, atLeast(stopLimitOrder.getTransactions().size())).save(any(Transaction.class));
        assertEquals(stopLimitOrder.getQuantity(), quantity);
        assertEquals(stopLimitOrder.getPricePerUnit().multiply(BigDecimal.valueOf(stopLimitOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(stopLimitOrder.getContractSize())), totalPrice);
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
        order.setStatus(OrderStatus.APPROVED);
        order.setIsDone(true);

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
}
