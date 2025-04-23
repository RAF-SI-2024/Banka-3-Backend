package rs.raf.stock_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.ClientDto;
import rs.raf.stock_service.domain.dto.UseOptionDto;
import rs.raf.stock_service.domain.dto.PortfolioEntryDto;
import rs.raf.stock_service.domain.dto.PublicStockDto;
import rs.raf.stock_service.domain.dto.SetPublicAmountDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.exceptions.InvalidListingTypeException;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.OptionNotEligibleException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.service.PortfolioService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.mapper.PortfolioMapper;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
public class PortfolioServiceTest {
    private final Long userId = 123L;
    Stock stock = new Stock();
    Stock stock2 = new Stock();
    @InjectMocks
    private PortfolioService portfolioService;
    @Mock
    private PortfolioEntryRepository portfolioEntryRepository;
    @Mock
    private ListingPriceHistoryRepository priceHistoryRepository;
    @Mock
    private PortfolioMapper portfolioEntryMapper;

    @Mock
    private UserClient userClient;


    private void initialiseStock() {
        stock.setId(1L);
        stock.setTicker("AAPL");
        stock.setName("Apple Inc.");
        stock.setContractSize(1);
        stock.setMaintenanceMargin(BigDecimal.ZERO);
    }

    private void initialiseGOOGLStock() {
        stock2.setId(2L);
        stock2.setTicker("GOOGL");
        stock2.setName("Alphabet Inc.");
        stock2.setContractSize(1);
        stock2.setMaintenanceMargin(BigDecimal.ZERO);
    }


    @Test
    void testUpdateHoldings_newBuyOrder_shouldCreateEntry() {
        initialiseStock();
        Order order = buildOrder(OrderDirection.BUY, 10, 1, BigDecimal.TEN);
        order.setIsDone(true);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
                .thenReturn(Optional.empty());

        portfolioService.updateHoldingsOnOrderExecution(order);

        verify(portfolioEntryRepository).save(any(PortfolioEntry.class));
    }

    @Test
    void testUpdateHoldings_existingBuyOrder_shouldUpdateEntry() {
        initialiseStock();
        PortfolioEntry existing = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .amount(10)
                .averagePrice(BigDecimal.valueOf(100))
                .build();

        Order order = buildOrder(OrderDirection.BUY, 10, 1, BigDecimal.valueOf(200));
        order.setIsDone(true);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
                .thenReturn(Optional.of(existing));

        portfolioService.updateHoldingsOnOrderExecution(order);

        verify(portfolioEntryRepository).save(argThat(entry ->
                entry.getAmount() == 20 &&
                        entry.getAveragePrice().compareTo(BigDecimal.valueOf(150)) == 0
        ));
    }

    @Test
    void testUpdateHoldings_sellOrder_shouldReduceAmount() {
        initialiseStock();
        PortfolioEntry existing = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .amount(20)
                .averagePrice(BigDecimal.valueOf(100))
                .reservedAmount(10)
                .build();

        Order order = buildOrder(OrderDirection.SELL, 10, 1, BigDecimal.valueOf(100));
        order.setIsDone(true);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
                .thenReturn(Optional.of(existing));

        portfolioService.updateHoldingsOnOrderExecution(order);

        verify(portfolioEntryRepository).save(argThat(entry ->
                entry.getAmount() == 10
        ));
    }

    @Test
    void testUpdateHoldings_sellAll_shouldDeleteEntry() {
        initialiseStock();
        PortfolioEntry existing = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .amount(10)
                .averagePrice(BigDecimal.valueOf(100))
                .build();

        Order order = buildOrder(OrderDirection.SELL, 10, 1, BigDecimal.valueOf(100));
        order.setIsDone(true);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
                .thenReturn(Optional.of(existing));

        portfolioService.updateHoldingsOnOrderExecution(order);

        verify(portfolioEntryRepository).delete(existing);
    }

    private Order buildOrder(OrderDirection direction, int qty, int contractSize, BigDecimal price) {
        initialiseStock();
        Order order = new Order();
        order.setUserId(userId);
        order.setListing(stock);
        order.setQuantity(qty);
        order.setContractSize(contractSize);
        order.setPricePerUnit(price);
        order.setDirection(direction);
        order.setIsDone(true);
        order.setRemainingPortions(0);
        return order;
    }


   @Test
    void testGetPortfolio_userHasHoldings_shouldReturnMappedDtos() {
        initialiseStock();
        initialiseGOOGLStock();

        stock.setPrice(BigDecimal.valueOf(115));
        stock2.setPrice(BigDecimal.valueOf(2200));

        PortfolioEntry entry1 = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .type(ListingType.STOCK)
                .amount(10)
                .averagePrice(BigDecimal.TEN)
                .lastModified(LocalDateTime.now())
                .publicAmount(0)
                .inTheMoney(false)
                .used(false)
                .build();

        PortfolioEntry entry2 = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock2)
                .type(ListingType.STOCK)
                .amount(5)
                .averagePrice(BigDecimal.valueOf(2000))
                .lastModified(LocalDateTime.now())
                .publicAmount(0)
                .inTheMoney(true)
                .used(false)
                .build();

        List<PortfolioEntry> entries = List.of(entry1, entry2);

        when(portfolioEntryRepository.findAllByUserId(userId)).thenReturn(entries);
        List<PortfolioEntryDto> result = portfolioService.getPortfolioForUser(userId);

        assertEquals(2, result.size());

        PortfolioEntryDto dto1 = result.get(0);
        assertEquals("AAPL", dto1.getTicker());
        assertEquals(BigDecimal.valueOf(1050), dto1.getProfit());

        PortfolioEntryDto dto2 = result.get(1);
        assertEquals("GOOGL", dto2.getTicker());
        assertEquals(BigDecimal.valueOf(1000), dto2.getProfit());

        verify(portfolioEntryRepository).findAllByUserId(userId);
    }



    @Test
    void testGetPortfolio_userHasNoHoldings_shouldReturnEmptyList() {
        when(portfolioEntryRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        List<PortfolioEntryDto> result = portfolioService.getPortfolioForUser(userId);

        assertTrue(result.isEmpty());
        verify(portfolioEntryRepository).findAllByUserId(userId);
        verifyNoInteractions(portfolioEntryMapper);
    }

    @Test
    void testSetPublicAmount_success() {
        PortfolioEntry entry = PortfolioEntry.builder()
                .id(1L) // Dodato jer se sad pretraga radi po ID-u
                .userId(userId)
                .listing(stock)
                .type(ListingType.STOCK)
                .amount(100)
                .publicAmount(0)
                .reservedAmount(0)
                .lastModified(LocalDateTime.now())
                .build();

        SetPublicAmountDto dto = new SetPublicAmountDto(1L, 50);

        when(portfolioEntryRepository.findByUserIdAndId(userId, 1L))
                .thenReturn(Optional.of(entry));

        portfolioService.setPublicAmount(userId, dto);

        assertEquals(50, entry.getPublicAmount());
        verify(portfolioEntryRepository).save(entry);
    }

    @Test
    void testSetPublicAmount_portfolioEntryNotFound() {
        SetPublicAmountDto dto = new SetPublicAmountDto(1L, 50);

        when(portfolioEntryRepository.findByUserIdAndId(userId, 1L))
                .thenReturn(Optional.empty());

        assertThrows(PortfolioEntryNotFoundException.class, () -> {
            portfolioService.setPublicAmount(userId, dto);
        });
    }

    @Test
    void testSetPublicAmount_invalidListingType() {
        PortfolioEntry entry = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .type(ListingType.FUTURES)
                .amount(100)
                .publicAmount(0)
                .build();

        SetPublicAmountDto dto = new SetPublicAmountDto(1L, 50);

        when(portfolioEntryRepository.findByUserIdAndId(userId, 1L))
                .thenReturn(Optional.of(entry));

        assertThrows(InvalidListingTypeException.class, () -> {
            portfolioService.setPublicAmount(userId, dto);
        });
    }

    @Test
    void testSetPublicAmount_exceedsOwnedAmount() {
        PortfolioEntry entry = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .type(ListingType.STOCK)
                .amount(40)
                .publicAmount(0)
                .reservedAmount(0)
                .build();

        SetPublicAmountDto dto = new SetPublicAmountDto(1L, 50); // vise od amount

        when(portfolioEntryRepository.findByUserIdAndId(userId, 1L))
                .thenReturn(Optional.of(entry));

        assertThrows(InvalidPublicAmountException.class, () -> {
            portfolioService.setPublicAmount(userId, dto);
        });
    }

    @Test
    void testGetAllPublicStocks_shouldReturnBasicFields() {
        initialiseStock();

        PortfolioEntry entry = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .type(ListingType.STOCK)
                .amount(100)
                .publicAmount(20)
                .lastModified(LocalDateTime.now())
                .build();

        when(portfolioEntryRepository.findAllByTypeAndPublicAmountGreaterThan(ListingType.STOCK, 0))
                .thenReturn(List.of(entry));

        ClientDto clientDto = ClientDto.builder()
                .firstName("Marko")
                .lastName("Markovic")
                .build();

        when(userClient.getClientById(userId)).thenReturn(clientDto);

        List<PublicStockDto> result = portfolioService.getAllPublicStocks(55L, "CLIENT");

        assertEquals(1, result.size());
        PublicStockDto dto = result.get(0);
        assertEquals("AAPL", dto.getTicker());
        assertEquals(ListingType.STOCK.name(), dto.getSecurity());
        assertEquals(20, dto.getAmount());
        assertEquals("Marko Markovic", dto.getOwner());
    }
}
