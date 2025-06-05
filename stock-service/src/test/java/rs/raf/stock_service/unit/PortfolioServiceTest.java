package rs.raf.stock_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.exceptions.InvalidListingTypeException;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.PortfolioService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.mapper.PortfolioMapper;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

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
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private BankClient bankClient;

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


//    @Test
//    void testUpdateHoldings_newBuyOrder_shouldCreateEntry() {
//        initialiseStock();
//        Order order = buildOrder(OrderDirection.BUY, 10, 1, BigDecimal.TEN);
//        order.setIsDone(true);
//
//        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
//                .thenReturn(Optional.empty());
//
//        portfolioService.updateHoldingsOnOrderExecution(order);
//
//        verify(portfolioEntryRepository).save(any(PortfolioEntry.class));
//    }
//
//    @Test
//    void testUpdateHoldings_existingBuyOrder_shouldUpdateEntry() {
//        initialiseStock();
//        PortfolioEntry existing = PortfolioEntry.builder()
//                .userId(userId)
//                .listing(stock)
//                .amount(10)
//                .averagePrice(BigDecimal.valueOf(100))
//                .build();
//
//        Order order = buildOrder(OrderDirection.BUY, 10, 1, BigDecimal.valueOf(200));
//        order.setIsDone(true);
//
//        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
//                .thenReturn(Optional.of(existing));
//
//        portfolioService.updateHoldingsOnOrderExecution(order);
//
//        verify(portfolioEntryRepository).save(argThat(entry ->
//                entry.getAmount() == 20 &&
//                        entry.getAveragePrice().compareTo(BigDecimal.valueOf(150)) == 0
//        ));
//    }
//
//    @Test
//    void testUpdateHoldings_sellOrder_shouldReduceAmount() {
//        initialiseStock();
//        PortfolioEntry existing = PortfolioEntry.builder()
//                .userId(userId)
//                .listing(stock)
//                .amount(20)
//                .averagePrice(BigDecimal.valueOf(100))
//                .reservedAmount(10)
//                .build();
//
//        Order order = buildOrder(OrderDirection.SELL, 10, 1, BigDecimal.valueOf(100));
//        order.setIsDone(true);
//
//        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
//                .thenReturn(Optional.of(existing));
//
//        portfolioService.updateHoldingsOnOrderExecution(order);
//
//        verify(portfolioEntryRepository).save(argThat(entry ->
//                entry.getAmount() == 10
//        ));
//    }
//
//    @Test
//    void testUpdateHoldings_sellAll_shouldDeleteEntry() {
//        initialiseStock();
//        PortfolioEntry existing = PortfolioEntry.builder()
//                .userId(userId)
//                .listing(stock)
//                .amount(10)
//                .averagePrice(BigDecimal.valueOf(100))
//                .build();
//
//        Order order = buildOrder(OrderDirection.SELL, 10, 1, BigDecimal.valueOf(100));
//        order.setIsDone(true);
//
//        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock))
//                .thenReturn(Optional.of(existing));
//
//        portfolioService.updateHoldingsOnOrderExecution(order);
//
//        verify(portfolioEntryRepository).delete(existing);
//    }

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
    void testGetALlClientPublicStocks_shouldReturnBasicFields() {
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

        List<PublicStockDto> result = portfolioService.getALlClientPublicStocks(55L);

        assertEquals(1, result.size());
        PublicStockDto dto = result.get(0);
        assertEquals("AAPL", dto.getTicker());
        assertEquals(ListingType.STOCK.name(), dto.getSecurity());
        assertEquals(20, dto.getAmount());
        assertEquals("Marko Markovic", dto.getOwner());
    }


    @Test
    void updateHoldingsOnBuyTransaction_whenEntryDoesNotExist_shouldCreateNewEntry() {
        initialiseStock(); // ako imaš ovu metodu iz ranije

        Order order = new Order();
        order.setUserId(userId);
        order.setListing(stock);
        order.setContractSize(1);
        order.setDirection(OrderDirection.BUY);
        order.setPricePerUnit(new BigDecimal("50.00"));
        order.setQuantity(10);

        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setQuantity(100);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock)).thenReturn(Optional.empty());

        portfolioService.updateHoldingsOnOrderExecution(transaction);

        ArgumentCaptor<PortfolioEntry> captor = ArgumentCaptor.forClass(PortfolioEntry.class);
        verify(portfolioEntryRepository).save(captor.capture());
        PortfolioEntry savedEntry = captor.getValue();

        assertEquals(userId, savedEntry.getUserId());
        assertEquals(stock, savedEntry.getListing());
        assertEquals(100, savedEntry.getAmount()); // 10 x 1
        assertEquals(new BigDecimal("50.00"), savedEntry.getAveragePrice());
    }
    @Test
    void updateHoldingsOnBuyTransaction_whenEntryExists_shouldUpdateEntry() {
        initialiseStock();

        PortfolioEntry existing = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .type(stock.getType())
                .amount(100)
                .averagePrice(new BigDecimal("40.00"))
                .reservedAmount(0)
                .publicAmount(0)
                .build();

        Order order = new Order();
        order.setUserId(userId);
        order.setListing(stock);
        order.setContractSize(1);
        order.setDirection(OrderDirection.BUY);
        order.setPricePerUnit(new BigDecimal("60.00"));
        order.setQuantity(50);

        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setQuantity(50);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock)).thenReturn(Optional.of(existing));

        portfolioService.updateHoldingsOnOrderExecution(transaction);

        ArgumentCaptor<PortfolioEntry> captor = ArgumentCaptor.forClass(PortfolioEntry.class);
        verify(portfolioEntryRepository).save(captor.capture());
        PortfolioEntry updated = captor.getValue();

        assertEquals(150, updated.getAmount());
        assertEquals(new BigDecimal("46.67"), updated.getAveragePrice().setScale(2, RoundingMode.HALF_UP));
    }
    @Test
    void updateHoldingsOnSellTransaction_shouldDeleteEntryWhenNoRemainingAmount() {
        initialiseStock();

        PortfolioEntry entry = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .amount(100)
                .averagePrice(new BigDecimal("30.00"))
                .reservedAmount(100)
                .build();

        Order order = new Order();
        order.setUserId(userId);
        order.setListing(stock);
        order.setContractSize(1);
        order.setDirection(OrderDirection.SELL);
        order.setQuantity(100);

        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setQuantity(100);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock)).thenReturn(Optional.of(entry));

        portfolioService.updateHoldingsOnOrderExecution(transaction);

        verify(portfolioEntryRepository).delete(entry);
    }
    @Test
    void updateHoldingsOnSellTransaction_shouldReduceAmountAndReserved() {
        initialiseStock();

        PortfolioEntry entry = PortfolioEntry.builder()
                .userId(userId)
                .listing(stock)
                .amount(150)
                .averagePrice(new BigDecimal("30.00"))
                .reservedAmount(150)
                .build();

        Order order = new Order();
        order.setUserId(userId);
        order.setListing(stock);
        order.setContractSize(1);
        order.setDirection(OrderDirection.SELL);
        order.setQuantity(50);

        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setQuantity(50);

        when(portfolioEntryRepository.findByUserIdAndListing(userId, stock)).thenReturn(Optional.of(entry));

        portfolioService.updateHoldingsOnOrderExecution(transaction);

        ArgumentCaptor<PortfolioEntry> captor = ArgumentCaptor.forClass(PortfolioEntry.class);
        verify(portfolioEntryRepository).save(captor.capture());
        PortfolioEntry updated = captor.getValue();

        assertEquals(100, updated.getAmount());
        assertEquals(100, updated.getReservedAmount());
    }

    @Test
    void updateHoldingsOnOtcExecution_shouldTransferWhenBuyerEntryDoesNotExist() {
        initialiseStock(); // postavlja stock, uključujući contractSize itd.

        Long sellerId = 1L;
        Long buyerId = 2L;

        PortfolioEntry sellerEntry = PortfolioEntry.builder()
                .userId(sellerId)
                .listing(stock)
                .amount(100)
                .reservedAmount(100)
                .averagePrice(new BigDecimal("50.00"))
                .build();

        when(portfolioEntryRepository.findByUserIdAndListing(sellerId, stock)).thenReturn(Optional.of(sellerEntry));
        when(portfolioEntryRepository.findByUserIdAndListing(buyerId, stock)).thenReturn(Optional.empty());

        portfolioService.updateHoldingsOnOtcOptionExecution(sellerId, buyerId, stock, 50, new BigDecimal("75.00"));

        ArgumentCaptor<PortfolioEntry> captor = ArgumentCaptor.forClass(PortfolioEntry.class);
        verify(portfolioEntryRepository, times(2)).save(captor.capture());

        List<PortfolioEntry> savedEntries = captor.getAllValues();

        PortfolioEntry updatedSeller = savedEntries.get(0);
        PortfolioEntry newBuyer = savedEntries.get(1);

        assertEquals(50, updatedSeller.getAmount());
        assertEquals(50, updatedSeller.getReservedAmount());

        assertEquals(buyerId, newBuyer.getUserId());
        assertEquals(50, newBuyer.getAmount());
        assertEquals(new BigDecimal("75.00"), newBuyer.getAveragePrice());
    }

    @Test
    void updateHoldingsOnOtcExecution_shouldUpdateBuyerEntryIfExists() {
        Long sellerId = 1L;
        Long buyerId = 2L;

        PortfolioEntry sellerEntry = PortfolioEntry.builder()
                .userId(sellerId)
                .listing(stock)
                .amount(100)
                .reservedAmount(100)
                .averagePrice(new BigDecimal("40.00"))
                .build();

        PortfolioEntry buyerEntry = PortfolioEntry.builder()
                .userId(buyerId)
                .listing(stock)
                .amount(50)
                .averagePrice(new BigDecimal("80.00"))
                .build();

        when(portfolioEntryRepository.findByUserIdAndListing(sellerId, stock)).thenReturn(Optional.of(sellerEntry));
        when(portfolioEntryRepository.findByUserIdAndListing(buyerId, stock)).thenReturn(Optional.of(buyerEntry));

        portfolioService.updateHoldingsOnOtcOptionExecution(sellerId, buyerId, stock, 50, new BigDecimal("100.00"));

        ArgumentCaptor<PortfolioEntry> captor = ArgumentCaptor.forClass(PortfolioEntry.class);
        verify(portfolioEntryRepository, times(2)).save(captor.capture());

        PortfolioEntry updatedBuyer = captor.getAllValues().get(1);
        assertEquals(100, updatedBuyer.getAmount());

        BigDecimal expectedAvgPrice = new BigDecimal("9000.00") // (50*80 + 50*100)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        assertEquals(expectedAvgPrice, updatedBuyer.getAveragePrice());
    }

    @Test
    void updateHoldingsOnOtcExecution_whenSellerEntryMissing_shouldThrow() {
        when(portfolioEntryRepository.findByUserIdAndListing(anyLong(), any())).thenReturn(Optional.empty());

        assertThrows(PortfolioEntryNotFoundException.class, () -> {
            portfolioService.updateHoldingsOnOtcOptionExecution(1L, 2L, stock, 10, new BigDecimal("70.00"));
        });

        verify(portfolioEntryRepository, never()).save(any());
    }

    @Test
    public void getUserTaxes_NoOrders_ReturnsZeroValues() {
        // Arrange
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(bankClient.convert(any(ConvertDto.class)))
                .thenReturn(BigDecimal.ZERO);

        // Act
        TaxGetResponseDto result = portfolioService.getUserTaxes(userId);

        // Assert
        assertEquals(BigDecimal.ZERO, result.getUnpaidForThisMonth());
        assertEquals(BigDecimal.ZERO, result.getPaidForThisYear());
        verify(orderRepository).findAllByUserId(userId);
    }

    @Test
    public void getUserTaxes_WithPendingTaxInLastMonth_CalculatesUnpaid() {
        // Arrange
        Order pendingOrder = createOrder(LocalDateTime.now().minusDays(15), TaxStatus.PENDING, new BigDecimal("100.00"));

        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(pendingOrder));
        when(bankClient.convert(any(ConvertDto.class)))
                .thenAnswer(invocation -> {
                    ConvertDto dto = invocation.getArgument(0);
                    return dto.getAmount(); // return same amount for simplicity
                });

        // Act
        TaxGetResponseDto result = portfolioService.getUserTaxes(userId);

        // Assert
        assertEquals(new BigDecimal("100.00"), result.getUnpaidForThisMonth());
        assertEquals(BigDecimal.ZERO, result.getPaidForThisYear());
    }

    @Test
    public void getUserTaxes_WithPaidTaxThisYear_CalculatesPaid() {
        // Arrange
        Order paidOrder = createOrder(LocalDateTime.now().minusMonths(3), TaxStatus.PAID, new BigDecimal("200.00"));

        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(paidOrder));
        when(bankClient.convert(any(ConvertDto.class)))
                .thenAnswer(invocation -> {
                    ConvertDto dto = invocation.getArgument(0);
                    return dto.getAmount(); // return same amount for simplicity
                });

        // Act
        TaxGetResponseDto result = portfolioService.getUserTaxes(userId);

        // Assert
        assertEquals(BigDecimal.ZERO, result.getUnpaidForThisMonth());
        assertEquals(new BigDecimal("200.00"), result.getPaidForThisYear());
    }

    @Test
    public void getUserTaxes_WithMixedOrders_CalculatesBothValues() {
        // Arrange
        Order pendingOrder = createOrder(LocalDateTime.now().minusDays(10), TaxStatus.PENDING, new BigDecimal("150.00"));
        Order paidOrder = createOrder(LocalDateTime.now().minusMonths(2), TaxStatus.PAID, new BigDecimal("300.00"));
        Order oldPendingOrder = createOrder(LocalDateTime.now().minusMonths(2), TaxStatus.PENDING, new BigDecimal("50.00")); // shouldn't count
        Order paidLastYearOrder = createOrder(LocalDateTime.now().minusYears(1), TaxStatus.PAID, new BigDecimal("100.00")); // shouldn't count

        when(orderRepository.findAllByUserId(userId)).thenReturn(Arrays.asList(
                pendingOrder, paidOrder, oldPendingOrder, paidLastYearOrder
        ));
        when(bankClient.convert(any(ConvertDto.class)))
                .thenAnswer(invocation -> {
                    ConvertDto dto = invocation.getArgument(0);
                    return dto.getAmount(); // return same amount for simplicity
                });

        // Act
        TaxGetResponseDto result = portfolioService.getUserTaxes(userId);

        // Assert
        assertEquals(new BigDecimal("150.00"), result.getUnpaidForThisMonth());
        assertEquals(new BigDecimal("300.00"), result.getPaidForThisYear());
    }

    @Test
    public void getUserTaxes_WithNullTaxStatus_IgnoresOrder() {
        // Arrange
        Order nullStatusOrder = new Order();
        nullStatusOrder.setTaxStatus(null);
        nullStatusOrder.setTaxAmount(new BigDecimal("500.00"));
        nullStatusOrder.setLastModification(LocalDateTime.now().minusDays(5));

        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(nullStatusOrder));
        when(bankClient.convert(any(ConvertDto.class)))
                .thenReturn(BigDecimal.ZERO);

        // Act
        TaxGetResponseDto result = portfolioService.getUserTaxes(userId);

        // Assert
        assertEquals(BigDecimal.ZERO, result.getUnpaidForThisMonth());
        assertEquals(BigDecimal.ZERO, result.getPaidForThisYear());
    }

    @Test
    public void getUserTaxes_ConvertsCurrency() {
        // Arrange
        Order pendingOrder = createOrder(LocalDateTime.now().minusDays(10), TaxStatus.PENDING, new BigDecimal("100.00"));
        Order paidOrder = createOrder(LocalDateTime.now().minusMonths(1), TaxStatus.PAID, new BigDecimal("200.00"));

        when(orderRepository.findAllByUserId(userId)).thenReturn(Arrays.asList(pendingOrder, paidOrder));

        // Stub in sequence
        when(bankClient.convert(any(ConvertDto.class)))
                .thenReturn(new BigDecimal("10500.00"))  // first call (for 100.00)
                .thenReturn(new BigDecimal("21000.00")); // second call (for 200.00)

        // Act
        TaxGetResponseDto result = portfolioService.getUserTaxes(userId);

        // Assert
        assertEquals(new BigDecimal("10500.00"), result.getUnpaidForThisMonth());
        assertEquals(new BigDecimal("21000.00"), result.getPaidForThisYear());
    }

    private Order createOrder(LocalDateTime date, TaxStatus status, BigDecimal amount) {
        Order order = new Order();
        order.setTaxStatus(status);
        order.setTaxAmount(amount);
        order.setLastModification(date);
        return order;
    }

}
