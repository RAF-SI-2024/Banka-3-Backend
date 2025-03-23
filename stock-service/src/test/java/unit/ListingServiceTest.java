package unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.stock_service.domain.dto.ActuaryLimitDto;
import rs.raf.stock_service.domain.dto.BuyListingDto;
import rs.raf.stock_service.domain.entity.FuturesContract; // Konkretna implementacija Listing
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.ListingService;
import rs.raf.stock_service.utils.JwtTokenUtil;
import rs.raf.stock_service.client.UserClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private ListingService listingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void placeBuyOrder_ShouldApproveOrder_WhenUserIsClient() {
        // Arrange
        String authHeader = "Bearer test-token";
        Long userId = 1L;
        String role = "CLIENT";
        BuyListingDto buyListingDto = new BuyListingDto(10, OrderType.MARKET, 100, 3, "123");

        FuturesContract listing = new FuturesContract();
        listing.setId(10L);
        listing.setPrice(new BigDecimal("50.00"));
        listing.setContractSize(100);
        listing.setContractUnit("UNIT");
        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        // Act
        listingService.placeBuyOrder(buyListingDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.APPROVED) &&
                        order.getUserId().equals(userId) &&
                        order.getAsset().equals(listing.getId())
        ));
    }

    @Test
    void placeBuyOrder_ShouldThrowListingNotFoundException_WhenListingDoesNotExist() {
        // Arrange
        String authHeader = "Bearer test-token";
        Long userId = 1L;
        BuyListingDto buyListingDto = new BuyListingDto(10, OrderType.MARKET, 100, 3, "123");

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(listingRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        ListingNotFoundException exception = assertThrows(ListingNotFoundException.class, () -> {
            listingService.placeBuyOrder(buyListingDto, authHeader);
        });

        assertEquals("Listing with ID 10 not found.", exception.getMessage());
    }

    @Test
    void placeBuyOrder_ShouldApproveOrder_WhenUserIsSupervisor() {
        // Arrange
        String authHeader = "Bearer test-token";
        Long userId = 1L;
        String role = "SUPERVISOR";
        BuyListingDto buyListingDto = new BuyListingDto(10, OrderType.MARKET, 100, 3, "123");

        FuturesContract listing = new FuturesContract();
        listing.setId(10L);
        listing.setPrice(new BigDecimal("50.00"));
        listing.setContractSize(100);
        listing.setContractUnit("UNIT");
        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        // Act
        listingService.placeBuyOrder(buyListingDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.APPROVED) &&
                        order.getUserId().equals(userId) &&
                        order.getAsset().equals(listing.getId())
        ));
    }

    @Test
    void placeBuyOrder_ShouldApproveOrder_WhenUserIsAdmin() {
        // Arrange
        String authHeader = "Bearer test-token";
        Long userId = 1L;
        String role = "ADMIN";
        BuyListingDto buyListingDto = new BuyListingDto(10, OrderType.MARKET, 100, 3, "123");

        FuturesContract listing = new FuturesContract();
        listing.setId(10L);
        listing.setPrice(new BigDecimal("50.00"));
        listing.setContractSize(100);
        listing.setContractUnit("UNIT");
        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        // Act
        listingService.placeBuyOrder(buyListingDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.APPROVED) &&
                        order.getUserId().equals(userId) &&
                        order.getAsset().equals(listing.getId())
        ));
    }



    @Test
    void placeBuyOrder_ShouldSetOrderStatusToPending_WhenActuaryApprovalIsNeeded() {
        // Arrange
        String authHeader = "Bearer test-token";
        Long userId = 1L;
        String role = "EMPLOYEE";
        BuyListingDto buyListingDto = new BuyListingDto(10, OrderType.MARKET, 100, 3, "123");

        FuturesContract listing = new FuturesContract();
        listing.setId(10L);
        listing.setPrice(new BigDecimal("50.00"));
        listing.setContractSize(100);
        listing.setContractUnit("UNIT");
        listing.setSettlementDate(LocalDate.from(LocalDateTime.now().plusMonths(1)));

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn(role);
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        // Pretpostavljamo da je aktuarijski korisnik koji zahteva odobrenje
        ActuaryLimitDto actuaryLimitDto = new ActuaryLimitDto(new BigDecimal("1000"), new BigDecimal("100"), true);
        when(userClient.getActuaryByEmployeeId(userId)).thenReturn(actuaryLimitDto);

        // Act
        listingService.placeBuyOrder(buyListingDto, authHeader);

        // Assert
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getStatus().equals(OrderStatus.PENDING) &&
                        order.getUserId().equals(userId) &&
                        order.getAsset().equals(listing.getId())
        ));
    }
}
