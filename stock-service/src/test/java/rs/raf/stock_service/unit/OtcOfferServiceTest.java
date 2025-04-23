package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.ClientDto;
import rs.raf.stock_service.domain.dto.CreateOtcOfferDto;
import rs.raf.stock_service.domain.dto.OtcOfferDto;
import rs.raf.stock_service.domain.dto.OtcOptionDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.enums.OtcOptionStatus;
import rs.raf.stock_service.domain.mapper.OtcOfferMapper;
import rs.raf.stock_service.domain.mapper.OtcOptionMapper;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.PortfolioAmountNotEnoughException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedActionException;
import rs.raf.stock_service.repository.OtcOfferRepository;
import rs.raf.stock_service.repository.OtcOptionRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;
import rs.raf.stock_service.service.OtcService;
import rs.raf.stock_service.service.TrackedPaymentService;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class OtcOfferServiceTest {

    @Mock
    private OtcOfferRepository otcOfferRepository;

    @Mock
    private PortfolioEntryRepository portfolioEntryRepository;

    @Mock
    private OtcOfferMapper otcOfferMapper;

    @Mock
    private OtcOptionRepository otcOptionRepository;

    @Mock
    private BankClient bankClient;

    @Mock
    private TrackedPaymentService trackedPaymentService;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OtcService otcService;

    @Mock
    private OtcOptionMapper otcOptionMapper;

    private final Long buyerId = 10L;
    private final Long sellerId = 20L;

    private Stock stock;
    private PortfolioEntry entry;
    private CreateOtcOfferDto dto;

    @BeforeEach
    public void setup() {
        stock = new Stock();
        stock.setId(1L);

        entry = new PortfolioEntry();
        entry.setId(1L);
        entry.setAmount(10);
        entry.setReservedAmount(0);
        entry.setListing(stock);
        entry.setUserId(sellerId);
        entry.setPublicAmount(100);

        dto = new CreateOtcOfferDto();
        dto.setPortfolioEntryId(1L);
        dto.setAmount(BigDecimal.valueOf(50));
        dto.setPricePerStock(BigDecimal.valueOf(150));
        dto.setPremium(BigDecimal.valueOf(10));
        dto.setSettlementDate(LocalDate.now());
    }

    @Test
    public void testCreateOffer_success() {
        when(portfolioEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(otcOfferRepository.save(any(OtcOffer.class))).thenAnswer(i -> i.getArguments()[0]);

        when(otcOfferMapper.toDto(any(OtcOffer.class), eq(buyerId)))
                .thenReturn(new OtcOfferDto());

        OtcOfferDto result = otcService.createOffer(dto, buyerId);

        assertNotNull(result);
        verify(otcOfferRepository).save(any(OtcOffer.class));
        verify(otcOfferMapper).toDto(any(OtcOffer.class), eq(buyerId));
    }

    @Test
    public void testCreateOffer_portfolioEntryNotFound() {
        when(portfolioEntryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PortfolioEntryNotFoundException.class,
                () -> otcService.createOffer(dto, buyerId));
    }

    @Test
    public void testCreateOffer_invalidPublicAmount() {
        entry.setPublicAmount(10);
        when(portfolioEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThrows(InvalidPublicAmountException.class,
                () -> otcService.createOffer(dto, buyerId));
    }

    @Test
    public void testAcceptOffer_authorized() {
        Stock stock = Stock.builder().build();

        OtcOffer offer = OtcOffer.builder()
                .id(1L)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .lastModifiedById(sellerId)
                .status(OtcOfferStatus.PENDING)
                .amount(5)
                .stock(stock)
                .build();

        when(otcOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(portfolioEntryRepository.findByUserIdAndListing (sellerId, stock)).thenReturn(Optional.of(entry));
        when(bankClient.getUSDAccountNumberByClientId(any())).thenReturn(ResponseEntity.ok("1"));
        when(trackedPaymentService.createTrackedPayment(any(), any())).thenReturn(new TrackedPayment());

        otcService.acceptOffer(1L, buyerId);

        verify(otcOfferRepository).save(offer);
        verify(portfolioEntryRepository).save(entry);
        verify(bankClient).executeSystemPayment(any());

        assertEquals(OtcOfferStatus.ACCEPTED, offer.getStatus());
        assertEquals(buyerId, offer.getLastModifiedById());
        assertNotNull(offer.getLastModified());
    }

    @Test
    public void testAcceptOffer_unauthorized_sameLastModifier() {
        OtcOffer offer = OtcOffer.builder()
                .id(1L)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .lastModifiedById(buyerId)
                .status(OtcOfferStatus.PENDING)
                .build();

        when(otcOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(UnauthorizedActionException.class, () -> otcService.acceptOffer(1L, buyerId));
        verify(otcOfferRepository, never()).save(any());
    }

    @Test
    public void testRejectOffer_authorized() {
        OtcOffer offer = OtcOffer.builder()
                .id(1L)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .lastModifiedById(buyerId)
                .status(OtcOfferStatus.PENDING)
                .build();

        when(otcOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        otcService.rejectOffer(1L, sellerId);

        assertEquals(OtcOfferStatus.REJECTED, offer.getStatus());
        assertEquals(sellerId, offer.getLastModifiedById());
        verify(otcOfferRepository).save(offer);
    }

    @Test
    public void testUpdateOffer_authorized() {
        Long offerId = 1L;

        OtcOffer offer = OtcOffer.builder()
                .id(offerId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .lastModifiedById(sellerId)
                .status(OtcOfferStatus.PENDING)
                .build();

        CreateOtcOfferDto dto = new CreateOtcOfferDto();
        dto.setAmount(BigDecimal.TEN);
        dto.setPricePerStock(new BigDecimal("100"));
        dto.setPremium(new BigDecimal("5"));
        dto.setSettlementDate(LocalDate.now().plusDays(7));

        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));

        otcService.updateOffer(offerId, buyerId, dto);

        verify(otcOfferRepository).save(argThat(updated ->
                updated.getAmount() == 10 &&
                        updated.getPricePerStock().equals(dto.getPricePerStock()) &&
                        updated.getPremium().equals(dto.getPremium()) &&
                        updated.getSettlementDate().equals(dto.getSettlementDate()) &&
                        updated.getLastModifiedById().equals(buyerId) &&
                        updated.getStatus() == OtcOfferStatus.PENDING
        ));
    }

    @Test
    public void testUpdateOffer_unauthorized_sameModifier() {
        Long offerId = 1L;

        OtcOffer offer = OtcOffer.builder()
                .id(offerId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .lastModifiedById(buyerId)
                .status(OtcOfferStatus.PENDING)
                .build();

        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));

        assertThrows(UnauthorizedActionException.class, () -> otcService.updateOffer(offerId, buyerId, dto));
        verify(otcOfferRepository, never()).save(any());
    }

    @Test
    public void testGetAllActiveOffersForUser_returnsOffersSortedAndFiltered() {
        Long userId = 100L;

        OtcOffer offer1 = OtcOffer.builder()
                .id(1L)
                .buyerId(userId)
                .sellerId(200L)
                .lastModifiedById(200L)
                .status(OtcOfferStatus.PENDING)
                .lastModified(LocalDateTime.now().minusMinutes(10))
                .build();

        OtcOffer offer2 = OtcOffer.builder()
                .id(2L)
                .buyerId(300L)
                .sellerId(userId)
                .lastModifiedById(300L)
                .status(OtcOfferStatus.PENDING)
                .lastModified(LocalDateTime.now().minusMinutes(5))
                .build();

        OtcOffer offer3 = OtcOffer.builder()
                .id(3L)
                .buyerId(userId)
                .sellerId(400L)
                .lastModifiedById(userId)
                .status(OtcOfferStatus.PENDING)
                .lastModified(LocalDateTime.now().minusMinutes(1))
                .build();

        List<OtcOffer> offers = Arrays.asList(offer1, offer2, offer3);

        when(otcOfferRepository.findAllByStatus(OtcOfferStatus.PENDING)).thenReturn(offers);


        when(otcOfferMapper.toDto(any(OtcOffer.class), eq(userId)))
                .thenAnswer(invocation -> {
                    OtcOffer o = invocation.getArgument(0);
                    return OtcOfferDto.builder()
                            .id(o.getId())
                            .amount(o.getAmount())
                            .pricePerStock(o.getPricePerStock())
                            .premium(o.getPremium())
                            .settlementDate(o.getSettlementDate())
                            .status(o.getStatus())
                            .canInteract(!o.getLastModifiedById().equals(userId))
                            .name("Mock User")
                            .build();
                });

        List<OtcOfferDto> result = otcService.getAllActiveOffersForUser(userId);

        assertEquals(3, result.size());
        assertEquals(3L, result.get(0).getId()); // najskorije
        assertEquals(2L, result.get(1).getId());
        assertEquals(1L, result.get(2).getId());

        assertFalse(result.get(0).getCanInteract()); // user je poslednji menjao
        assertTrue(result.get(1).getCanInteract());
        assertTrue(result.get(2).getCanInteract());
    }

    @Test
    public void testHandleAcceptSuccessfulPayment_createsOtcOptionAndSavesIt() {
        Long trackedPaymentId = 1L;
        Long offerId = 2L;

        // Simulirani TrackedPayment
        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setId(trackedPaymentId);
        trackedPayment.setTrackedEntityId(offerId);

        // Simulirani OtcOffer
        Stock stock = new Stock();
        stock.setTicker("AAPL");
        stock.setName("Apple Inc.");
        Exchange exchange = Exchange.builder()
                .name("NASDAQ")
                .build();
        stock.setExchange(exchange);

        OtcOffer offer = OtcOffer.builder()
                .id(offerId)
                .stock(stock)
                .sellerId(20L)
                .buyerId(10L)
                .pricePerStock(BigDecimal.valueOf(150))
                .amount(10)
                .premium(BigDecimal.valueOf(5))
                .settlementDate(LocalDate.now().plusDays(5))
                .status(OtcOfferStatus.ACCEPTED)
                .build();

        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));

        otcService.handleAcceptSuccessfulPayment(trackedPaymentId);

        // Proveravamo da se OtcOption sačuvao
        verify(otcOptionRepository).save(argThat(option ->
                option.getOtcOffer().equals(offer) &&
                        option.getSellerId().equals(offer.getSellerId()) &&
                        option.getBuyerId().equals(offer.getBuyerId()) &&
                        option.getUnderlyingStock().equals(offer.getStock()) &&
                        option.getStrikePrice().equals(offer.getPricePerStock()) &&
                        option.getAmount() == offer.getAmount() &&
                        option.getPremium().equals(offer.getPremium()) &&
                        option.getSettlementDate().equals(offer.getSettlementDate()) &&
                        option.getStatus() == OtcOptionStatus.VALID
        ));
    }

    @Test
    void testGetOtcOptionsForUser_validOptionsOnly() {
        Long userId = 1L;
        OtcOption option = new OtcOption();
        option.setBuyerId(userId);
        option.setSellerId(2L);
        option.setSettlementDate(LocalDate.now().plusDays(1));
        OtcOptionDto dto = new OtcOptionDto();
        when(otcOptionMapper.toDto(eq(option), eq("John Doe"))).thenReturn(dto);

        when(otcOptionRepository.findAllValid(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of(option));
        when(userClient.getClientById(2L)).thenReturn(new ClientDto(2L, "John", "Doe","john@doe.com"));
        List<OtcOptionDto> result = otcService.getOtcOptionsForUser(true, userId);

        assertEquals(1, result.size());
    }

    @Test
    void testCancelOffer_authorized() {
        Long offerId = 1L;
        Long userId = 5L;
        OtcOffer offer = OtcOffer.builder()
                .id(offerId)
                .lastModifiedById(userId)
                .status(OtcOfferStatus.PENDING)
                .build();

        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));

        otcService.cancelOffer(offerId, userId);

        assertEquals(OtcOfferStatus.CANCELLED, offer.getStatus());
        verify(otcOfferRepository).save(offer);
    }

    @Test
    void testCancelOffer_unauthorized() {
        Long offerId = 1L;
        OtcOffer offer = OtcOffer.builder()
                .id(offerId)
                .lastModifiedById(99L)
                .status(OtcOfferStatus.PENDING)
                .build();

        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));

        assertThrows(UnauthorizedActionException.class, () -> otcService.cancelOffer(offerId, 1L));
    }

    @Test
    void testCheckOtcOptionExpiration_expiresCorrectly() {
        OtcOption option = new OtcOption();
        option.setAmount(10);
        option.setUnderlyingStock(new Stock());
        option.setSellerId(1L);

        PortfolioEntry entry = new PortfolioEntry();
        entry.setAmount(0);
        entry.setReservedAmount(10);
        entry.setListing(option.getUnderlyingStock());

        when(otcOptionRepository.findAllValidButExpired(any(LocalDate.class)))
                .thenReturn(List.of(option));
        when(portfolioEntryRepository.findByUserIdAndListing(eq(1L), any()))
                .thenReturn(Optional.of(entry));

        otcService.checkOtcOptionExpiration();

        assertEquals(OtcOptionStatus.EXPIRED, option.getStatus());
        assertEquals(10, entry.getAmount());
        assertEquals(0, entry.getReservedAmount());
    }

    @Test
    public void handleAcceptSuccessfulPayment_CreatesOtcOption() {
        Long trackedPaymentId = 1L;
        Long offerId = 2L;

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(offerId);

        OtcOffer offer = new OtcOffer();
        offer.setSellerId(3L);
        offer.setBuyerId(4L);
        offer.setStock(new Stock());
        offer.setPricePerStock(BigDecimal.valueOf(100.0));
        offer.setAmount(10);
        offer.setSettlementDate(LocalDate.from(LocalDateTime.now().plusDays(1)));
        offer.setPremium(BigDecimal.valueOf(50.0));

        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(otcOptionRepository.save(any(OtcOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        otcService.handleAcceptSuccessfulPayment(trackedPaymentId);

        verify(otcOptionRepository).save(any(OtcOption.class));
        verify(otcOfferRepository).save(offer);
        assertNotNull(offer.getOtcOption());
        assertEquals(OtcOptionStatus.VALID, offer.getOtcOption().getStatus());
    }

    @Test
    public void handleAcceptSuccessfulPayment_OfferNotFound_ThrowsException() {
        Long trackedPaymentId = 1L;
        Long offerId = 2L;

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(offerId);

        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                otcService.handleAcceptSuccessfulPayment(trackedPaymentId));
    }

    @Test
    public void handleAcceptFailedPayment_UpdatesPortfolioAndOffer() {
        Long trackedPaymentId = 1L;
        Long offerId = 2L;

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(offerId);

        OtcOffer offer = new OtcOffer();
        offer.setSellerId(3L);
        offer.setAmount(10);
        offer.setStock(new Stock());
        offer.setStatus(OtcOfferStatus.ACCEPTED);

        PortfolioEntry portfolioEntry = new PortfolioEntry();
        portfolioEntry.setPublicAmount(15); // Must be >= offer amount (10)
        portfolioEntry.setReservedAmount(10); // Must match offer amount
        portfolioEntry.setAmount(25); // Total amount

        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(portfolioEntryRepository.findByUserIdAndListing(offer.getSellerId(), offer.getStock()))
                .thenReturn(Optional.of(portfolioEntry));

        otcService.handleAcceptFailedPayment(trackedPaymentId);

        assertEquals(25, portfolioEntry.getPublicAmount()); // 15 (public) + 10 (reserved)
        assertEquals(0, portfolioEntry.getReservedAmount()); // reserved should be reduced by offer amount
        assertEquals(OtcOfferStatus.PENDING, offer.getStatus());
        assertNotNull(offer.getLastModified());
        verify(portfolioEntryRepository).save(portfolioEntry);
        verify(otcOfferRepository).save(offer);
    }

    @Test
    public void handleAcceptFailedPayment_PortfolioEntryNotFound_ThrowsException() {
        Long trackedPaymentId = 1L;
        Long offerId = 2L;

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(offerId);

        OtcOffer offer = new OtcOffer();
        offer.setSellerId(3L);
        offer.setStock(new Stock());

        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(portfolioEntryRepository.findByUserIdAndListing(offer.getSellerId(), offer.getStock()))
                .thenReturn(Optional.empty());

        assertThrows(PortfolioEntryNotFoundException.class, () ->
                otcService.handleAcceptFailedPayment(trackedPaymentId));
    }

    @Test
    public void handleAcceptFailedPayment_InsufficientPortfolioAmount_ThrowsException() {
        Long trackedPaymentId = 1L;
        Long offerId = 2L;

        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(offerId);

        OtcOffer offer = new OtcOffer();
        offer.setSellerId(3L);
        offer.setAmount(10);
        offer.setStock(new Stock());

        PortfolioEntry portfolioEntry = new PortfolioEntry();
        portfolioEntry.setPublicAmount(5);
        portfolioEntry.setReservedAmount(5);

        when(trackedPaymentService.getTrackedPayment(trackedPaymentId)).thenReturn(trackedPayment);
        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(portfolioEntryRepository.findByUserIdAndListing(offer.getSellerId(), offer.getStock()))
                .thenReturn(Optional.of(portfolioEntry));

        assertThrows(PortfolioAmountNotEnoughException.class, () ->
                otcService.handleAcceptFailedPayment(trackedPaymentId));
    }

}
