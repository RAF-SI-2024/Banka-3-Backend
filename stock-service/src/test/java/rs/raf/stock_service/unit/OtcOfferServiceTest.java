package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.stock_service.domain.dto.CreateOtcOfferDto;
import rs.raf.stock_service.domain.dto.OtcOfferDto;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.mapper.OtcOfferMapper;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedActionException;
import rs.raf.stock_service.repository.OtcOfferRepository;
import rs.raf.stock_service.repository.OtcOptionRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;
import rs.raf.stock_service.service.OtcService;

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

    @InjectMocks
    private OtcService otcService;

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

        otcService.acceptOffer(1L, buyerId);

        assertEquals(OtcOfferStatus.ACCEPTED, offer.getStatus());
        assertEquals(buyerId, offer.getLastModifiedById());
        assertNotNull(offer.getLastModified());
        verify(otcOfferRepository).save(offer);
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
}
