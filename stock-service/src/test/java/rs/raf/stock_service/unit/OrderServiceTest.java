package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

        Country country = Country.builder().openTime(LocalTime.of(16, 0))
                .closeTime(LocalTime.of(22, 0)).build();

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

        stopOrder = OrderMapper.toOrder(createStopOrderDto, userId, listing);
        stopOrder.setId(1L);
        stopOrder.setStatus(OrderStatus.APPROVED);
        stopOrder.setReservedAmount(stopOrder.getPricePerUnit().multiply(BigDecimal.valueOf(stopOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(stopOrder.getContractSize())));

        createLimitOrderDto = new CreateOrderDto(1L, OrderType.LIMIT, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(100));

        limitOrder = OrderMapper.toOrder(createLimitOrderDto, userId, listing);
        limitOrder.setId(2L);
        limitOrder.setStatus(OrderStatus.APPROVED);
        limitOrder.setReservedAmount(limitOrder.getPricePerUnit().multiply(BigDecimal.valueOf(limitOrder.getQuantity()))
                .multiply(BigDecimal.valueOf(limitOrder.getContractSize())));

        createStopLimitOrderDto = new CreateOrderDto(1L, OrderType.STOP_LIMIT, 100, 1, OrderDirection.BUY,
                "123", false, new BigDecimal(100), new BigDecimal(200));

        stopLimitOrder = OrderMapper.toOrder(createStopLimitOrderDto, userId, listing);
        stopLimitOrder.setId(3L);
        stopLimitOrder.setStatus(OrderStatus.APPROVED);
        stopLimitOrder.setReservedAmount(stopLimitOrder.getPricePerUnit().multiply(BigDecimal.valueOf(stopLimitOrder.getQuantity()))
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
        verify(orderRepository, atLeast(3)).save(any(Order.class));
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
        verify(orderRepository, atLeast(2)).save(any(Order.class));
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

        verify(orderRepository, atLeast(2)).save(stopOrder);
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

        verify(orderRepository, atLeast(2)).save(limitOrder);
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

        verify(orderRepository, atLeast(2)).save(stopLimitOrder);
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
}
