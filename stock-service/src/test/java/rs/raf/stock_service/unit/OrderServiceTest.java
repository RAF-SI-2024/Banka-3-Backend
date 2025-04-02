package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.OrderService;
import rs.raf.stock_service.utils.JwtTokenUtil;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
