package unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.stock_service.domain.dto.CreateOtcOfferDto;
import rs.raf.stock_service.domain.dto.OtcOfferDto;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.mapper.OtcOfferMapper;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedActionException;
import rs.raf.stock_service.repository.OtcOfferRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;
import rs.raf.stock_service.service.OtcService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class OtcOfferServiceTest {

    @Mock
    private OtcOfferRepository otcOfferRepository;

    @Mock
    private PortfolioEntryRepository portfolioEntryRepository;

    @Mock
    private OtcOfferMapper otcOfferMapper;

    @InjectMocks
    private OtcService otcService;

    private final Long buyerId = 10L;
    private final Long sellerId = 20L;

    private Stock stock;
    private PortfolioEntry entry;
    private CreateOtcOfferDto dto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        stock = new Stock();
        stock.setId(1L);

        entry = new PortfolioEntry();
        entry.setId(1L);
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
        when(otcOfferMapper.toDto(any(OtcOffer.class))).thenReturn(new OtcOfferDto());

        OtcOfferDto result = otcService.createOffer(dto, buyerId);

        assertNotNull(result);
        verify(otcOfferRepository, times(1)).save(any(OtcOffer.class));
        verify(otcOfferMapper).toDto(any(OtcOffer.class));
    }

    @Test
    public void testCreateOffer_portfolioEntryNotFound() {
        when(portfolioEntryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PortfolioEntryNotFoundException.class,
                () -> otcService.createOffer(dto, buyerId));
    }

    @Test
    public void testCreateOffer_invalidPublicAmount() {
        entry.setPublicAmount(10); // manje od onoga što traži dto
        when(portfolioEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThrows(InvalidPublicAmountException.class,
                () -> otcService.createOffer(dto, buyerId));
    }

    @Test
    void testAcceptOffer_success() {
        OtcOffer offer = OtcOffer.builder()
                .id(1L)
                .sellerId(123L)
                .status(OtcOfferStatus.PENDING)
                .build();

        when(otcOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        otcService.acceptOffer(1L, 123L);

        assertEquals(OtcOfferStatus.ACCEPTED, offer.getStatus());
        assertEquals(123L, offer.getLastModifiedById());
        assertNotNull(offer.getLastModified());
        verify(otcOfferRepository).save(offer);
    }

    @Test
    void testUpdateOffer_success() {
        Long offerId = 1L;
        Long sellerId = 100L;

        OtcOffer existingOffer = OtcOffer.builder()
                .id(offerId)
                .sellerId(sellerId)
                .status(OtcOfferStatus.PENDING)
                .lastModified(LocalDateTime.now().minusDays(1))
                .build();

        CreateOtcOfferDto dto = new CreateOtcOfferDto();
        dto.setAmount(BigDecimal.TEN);
        dto.setPricePerStock(new BigDecimal("100"));
        dto.setPremium(new BigDecimal("5"));
        dto.setSettlementDate(LocalDate.now().plusDays(7));

        when(otcOfferRepository.findById(offerId)).thenReturn(Optional.of(existingOffer));

        otcService.updateOffer(offerId, sellerId, dto);

        verify(otcOfferRepository).save(argThat(updatedOffer ->
                updatedOffer.getAmount() == 10 &&
                        updatedOffer.getPricePerStock().equals(new BigDecimal("100")) &&
                        updatedOffer.getPremium().equals(new BigDecimal("5")) &&
                        updatedOffer.getSettlementDate().equals(dto.getSettlementDate()) &&
                        updatedOffer.getStatus() == OtcOfferStatus.PENDING &&
                        updatedOffer.getLastModifiedById().equals(sellerId)
        ));
    }
}
