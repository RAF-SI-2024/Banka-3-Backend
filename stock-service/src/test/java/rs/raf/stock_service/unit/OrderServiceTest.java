package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingPriceHistory;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.OrderMapper;
import rs.raf.stock_service.exceptions.CantCancelOrderInCurrentOrderState;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.OrderService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private BankClient bankClient;

    @Mock
    private ListingPriceHistoryRepository dailyPriceInfoRepository;

    @Mock
    private ListingMapper listingMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void shouldReturnOrderDtosWhenUserHasOrders() {
        Long userId = 1L;

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

        ListingDto listingDto = new ListingDto(
                10L,
                ListingType.STOCK,
                "AAPL",
                new BigDecimal("150.25"),
                new BigDecimal("2.75"),
                5000000L,
                new BigDecimal("1000.00"),
                "XNAS",
                new BigDecimal("150.50")
        );

        OrderDto orderDto = new OrderDto();
        orderDto.setId(101L);
        orderDto.setUserId(userId);
        orderDto.setListing(listingDto);

        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order));
        when(dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(listingPriceHistory);
        when(listingMapper.toDto(listing, listingPriceHistory)).thenReturn(listingDto);

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.toDto(order, listingDto)).thenReturn(orderDto);

            List<OrderDto> result = orderService.getOrdersByUser(userId);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(listingDto, result.get(0).getListing());
            assertEquals(userId, result.get(0).getUserId());
        }
    }


    @Test
    void shouldReturnEmptyListWhenUserHasNoOrders() {
        Long userId = 1L;
        when(orderRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        List<OrderDto> result = orderService.getOrdersByUser(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullListingGracefully() {
        Long userId = 1L;
        Order order = new Order();
        order.setId(1L);
        order.setUserId(userId);
        order.setListing(null);
        order.setTransactions(new ArrayList<>());

        ListingDto listingDto = null;
        OrderDto expectedOrderDto = new OrderDto();
        expectedOrderDto.setId(1L);
        expectedOrderDto.setUserId(userId);
        expectedOrderDto.setListing(null);

        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order));
        when(dailyPriceInfoRepository.findTopByListingOrderByDateDesc(null)).thenReturn(null);
        when(listingMapper.toDto(null, null)).thenReturn(null);

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.toDto(order, null)).thenReturn(expectedOrderDto);

            List<OrderDto> result = orderService.getOrdersByUser(userId);

            assertEquals(1, result.size());
            assertNull(result.get(0).getListing());
        }
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

//    @Test
//    void testGetOrdersByStatus_WhenStatusIsProvided() {
//        ListingDailyPriceInfo dailyPriceInfo = new ListingDailyPriceInfo();
//
//        Listing listing = new FuturesContract();
//
//        Order order1 = Order.builder()
//                .status(OrderStatus.APPROVED)
//                .quantity(10)
//                .pricePerUnit(new BigDecimal("150.50"))
//                .lastModification(LocalDateTime.now())
//                .listing(listing)
//                .build();
//
//        Order order2 = Order.builder()
//                .status(OrderStatus.APPROVED)
//                .quantity(5)
//                .pricePerUnit(new BigDecimal("200.00"))
//                .lastModification(LocalDateTime.now().minusDays(1))
//                .listing(listing)
//                .build();
//
//        List<Order> orderList = Arrays.asList(order1, order2);
//        Page<Order> orderPage = new PageImpl<>(orderList);
//
//        when(dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(dailyPriceInfo);
//        when(orderRepository.findByStatus(OrderStatus.APPROVED, PageRequest.of(1, 10))).thenReturn(orderPage);
//
//        Page<OrderDto> result = orderService.getOrdersByStatus(OrderStatus.APPROVED, PageRequest.of(1, 10));
//
//        assertEquals(2, result.getTotalElements());
//        assertEquals(OrderStatus.APPROVED, result.getContent().get(0).getStatus());
//        verify(orderRepository, times(1)).findByStatus(eq(OrderStatus.APPROVED), any(PageRequest.class));
//    }
//
//    @Test
//    void testGetOrdersByStatus_WhenStatusIsNull() {
//        Order order1 = Order.builder()
//                .status(OrderStatus.PENDING)
//                .quantity(8)
//                .pricePerUnit(new BigDecimal("120.00"))
//                .lastModification(LocalDateTime.now())
//                .build();
//
//        Order order2 = Order.builder()
//                .status(OrderStatus.APPROVED)
//                .quantity(15)
//                .pricePerUnit(new BigDecimal("180.00"))
//                .lastModification(LocalDateTime.now().minusHours(3))
//                .build();
//
//        List<Order> orderList = Arrays.asList(order1, order2);
//        Page<Order> orderPage = new PageImpl<>(orderList);
//
//        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);
//
//        Page<OrderDto> result = orderService.getOrdersByStatus(null, PageRequest.of(0, 10));
//
//        assertEquals(2, result.getTotalElements());
//        verify(orderRepository, times(1)).findAll(any(PageRequest.class));
//    }
//
//    @Test
//    void approveOrder_ShouldThrowOrderNotFoundException_WhenOrderDoesNotExist() {
//        Long orderId = 1L;
//        String authHeader = "Bearer test-token";
//
//        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
//
//        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
//            orderService.approveOrder(orderId, authHeader);
//        });
//
//        assertEquals("Order with ID "+orderId+" not found.", exception.getMessage());
//        verify(orderRepository, times(1)).findById(orderId);
//        verify(jwtTokenUtil, never()).getUserIdFromAuthHeader(anyString());
//        verify(orderRepository, never()).save(any(Order.class));
//    }
//    @Test
//    void approveOrder_ShouldApproveOrder_WhenOrderIsPending() {
//        Long orderId = 1L;
//        String authHeader = "Bearer test-token";
//        Long userId = 2L;
//
//        Order order = new Order();
//        order.setId(orderId);
//        order.setStatus(OrderStatus.PENDING);
//
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
//        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
//
//        orderService.approveOrder(orderId, authHeader);
//
//        assertEquals(OrderStatus.APPROVED, order.getStatus());
//        assertEquals(userId, order.getApprovedBy());
//        verify(orderRepository, times(1)).save(order);
//    }
//
//    @Test
//    void approveOrder_ShouldThrowException_WhenOrderIsNotPending() {
//        Long orderId = 1L;
//        String authHeader = "Bearer test-token";
//
//        Order order = new Order();
//        order.setId(orderId);
//        order.setStatus(OrderStatus.APPROVED);
//
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
//
//        CantApproveNonPendingOrder exception = assertThrows(CantApproveNonPendingOrder.class, () -> {
//            orderService.approveOrder(orderId, authHeader);
//        });
//
//        assertEquals("Order with ID "+orderId+" is not in pending status.", exception.getMessage());
//        verify(orderRepository, never()).save(any(Order.class));
//    }
//
//    @Test
//    void declineOrder_ShouldDeclineOrder_WhenOrderIsPending() {
//        Long orderId = 1L;
//        String authHeader = "Bearer test-token";
//        Long userId = 2L;
//
//        Order order = new Order();
//        order.setId(orderId);
//        order.setStatus(OrderStatus.PENDING);
//
//        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
//        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
//
//        orderService.declineOrder(orderId, authHeader);
//
//        assertEquals(OrderStatus.DECLINED, order.getStatus());
//        assertEquals(userId, order.getApprovedBy());
//        verify(orderRepository, times(1)).save(order);
//    }
//
//    @Test
//    void createOrder_ShouldApproveOrder_WhenUserIsClient() {
//        // Arrange
//        String authHeader = "Bearer test-token";
//        Long userId = 1L;
//        String role = "CLIENT";
//        CreateOrderDto createOrderDto = new CreateOrderDto(10L, OrderType.MARKET, 100, 3,
//                OrderDirection.BUY, "123");
//
//        FuturesContract listing = new FuturesContract();
//        listing.setId(10L);
//        listing.setPrice(new BigDecimal("50.00"));
//        listing.setContractSize(100);
//        listing.setContractUnit("UNIT");
//        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));
//
//        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
//        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
//        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
//        when(bankClient.getAccountBalance("123")).thenReturn(BigDecimal.valueOf(999999999));
//
//        // Act
//        orderService.createOrder(createOrderDto, authHeader);
//
//        // Assert
//        verify(orderRepository, times(1)).save(argThat(order ->
//                order.getStatus().equals(OrderStatus.APPROVED) &&
//                        order.getUserId().equals(userId) &&
//                        order.getListing().getId().equals(listing.getId())
//        ));
//    }
//
//    @Test
//    void createOrder_ShouldThrowListingNotFoundException_WhenListingDoesNotExist() {
//        // Arrange
//        String authHeader = "Bearer test-token";
//        Long userId = 1L;
//        CreateOrderDto createOrderDto = new CreateOrderDto(10L, OrderType.MARKET, 100, 3,
//                OrderDirection.BUY, "123");
//
//        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
//        when(listingRepository.findById(10L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        ListingNotFoundException exception = assertThrows(ListingNotFoundException.class, () -> {
//            orderService.createOrder(createOrderDto, authHeader);
//        });
//
//        assertEquals("Listing with ID 10 not found.", exception.getMessage());
//    }
//
//    //Ja sam 99% siguran da po specifikaciji samo client i agent mogu da kreiraju ordere
////    @Test
////    void placeBuyOrder_ShouldApproveOrder_WhenUserIsSupervisor() {
////        // Arrange
////        String authHeader = "Bearer test-token";
////        Long userId = 1L;
////        String role = "SUPERVISOR";
////        CreateOrderDto createOrderDto = new CreateOrderDto(10, OrderType.MARKET, 100, 3, "123");
////
////        FuturesContract listing = new FuturesContract();
////        listing.setId(10L);
////        listing.setPrice(new BigDecimal("50.00"));
////        listing.setContractSize(100);
////        listing.setContractUnit("UNIT");
////        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));
////
////        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
////        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
////        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
////
////        // Act
////        orderService.createOrder(createOrderDto, authHeader);
////
////        // Assert
////        verify(orderRepository, times(1)).save(argThat(order ->
////                order.getStatus().equals(OrderStatus.APPROVED) &&
////                        order.getUserId().equals(userId) &&
////                        order.getListing().getId().equals(listing.getId())
////        ));
////    }
////
////    @Test
////    void placeBuyOrder_ShouldApproveOrder_WhenUserIsAdmin() {
////        // Arrange
////        String authHeader = "Bearer test-token";
////        Long userId = 1L;
////        String role = "ADMIN";
////        CreateOrderDto createOrderDto = new CreateOrderDto(10, OrderType.MARKET, 100, 3, "123");
////
////        FuturesContract listing = new FuturesContract();
////        listing.setId(10L);
////        listing.setPrice(new BigDecimal("50.00"));
////        listing.setContractSize(100);
////        listing.setContractUnit("UNIT");
////        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));
////
////        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
////        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
////        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
////
////        // Act
////        orderService.createOrder(createOrderDto, authHeader);
////
////        // Assert
////        verify(orderRepository, times(1)).save(argThat(order ->
////                order.getStatus().equals(OrderStatus.APPROVED) &&
////                        order.getUserId().equals(userId) &&
////                        order.getListing().getId().equals(listing.getId())
////        ));
////    }
//
//
//
//    @Test
//    void createOrder_ShouldSetOrderStatusToPending_WhenActuaryApprovalIsNeeded() {
//        // Arrange
//        String authHeader = "Bearer test-token";
//        Long userId = 1L;
//        String role = "EMPLOYEE";
//        CreateOrderDto createOrderDto = new CreateOrderDto(10L, OrderType.MARKET, 100, 3,
//                OrderDirection.BUY, "123");
//
//        FuturesContract listing = new FuturesContract();
//        listing.setId(10L);
//        listing.setPrice(new BigDecimal("50.00"));
//        listing.setContractSize(100);
//        listing.setContractUnit("UNIT");
//        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));
//
//        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
//        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
//        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
//
//        // Pretpostavljamo da je aktuarijski korisnik koji zahteva odobrenje
//        ActuaryLimitDto actuaryLimitDto = new ActuaryLimitDto(new BigDecimal("1000"), new BigDecimal("100"), true);
//        when(userClient.getActuaryByEmployeeId(userId)).thenReturn(actuaryLimitDto);
//
//        // Act
//        orderService.createOrder(createOrderDto, authHeader);
//
//        // Assert
//        verify(orderRepository, times(1)).save(argThat(order ->
//                order.getStatus().equals(OrderStatus.PENDING) &&
//                        order.getUserId().equals(userId) &&
//                        order.getListing().getId().equals(listing.getId())
//        ));
//    }
}
