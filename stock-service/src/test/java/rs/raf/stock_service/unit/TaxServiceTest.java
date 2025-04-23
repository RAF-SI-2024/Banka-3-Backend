package rs.raf.stock_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.TrackedPayment;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.domain.enums.TrackedPaymentStatus;
import rs.raf.stock_service.domain.enums.TrackedPaymentType;
import rs.raf.stock_service.domain.mapper.TrackedPaymentMapper;
import rs.raf.stock_service.exceptions.InsufficientFundsException;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.repository.TrackedPaymentRepository;
import rs.raf.stock_service.service.PortfolioService;
import rs.raf.stock_service.service.TaxService;
import rs.raf.stock_service.service.TrackedPaymentService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaxServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private BankClient bankClient;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TrackedPaymentService trackedPaymentService;

    @Mock
    private TrackedPaymentRepository trackedPaymentRepository;

    @Mock
    private TrackedPaymentMapper trackedPaymentMapper;

    @InjectMocks
    private TaxService taxService;

    @Test
    public void getTaxes_ReturnsUserTaxes() {
        UserTaxDto userTaxDto = new UserTaxDto();
        userTaxDto.setId(1L);
        when(userClient.getAgentsAndClients(any(), any(), any())).thenReturn(List.of(userTaxDto));
        TaxGetResponseDto taxResponse = new TaxGetResponseDto();
        taxResponse.setUnpaidForThisMonth(BigDecimal.TEN);
        taxResponse.setPaidForThisYear(BigDecimal.ONE);
        when(portfolioService.getUserTaxes(any())).thenReturn(taxResponse);

        List<UserTaxDto> result = taxService.getTaxes("test", "test", "test");

        assertEquals(1, result.size());
        assertEquals(BigDecimal.TEN, result.get(0).getUnpaidTaxThisMonth());
        assertEquals(BigDecimal.ONE, result.get(0).getPaidTaxThisYear());
    }

    @Test
    public void processTaxes_NoEligibleOrders_ReturnsNull() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        TrackedPaymentDto result = taxService.processTaxes();

        assertNull(result);
    }

    @Test
    public void processTaxes_InsufficientFunds_ThrowsException() {
        Order order = new Order();
        order.setTaxAmount(BigDecimal.TEN);
        order.setTaxStatus(TaxStatus.PENDING);
        order.setAccountNumber("123");
        when(orderRepository.findAll()).thenReturn(List.of(order));
        AccountDetailsDto accountDetails = new AccountDetailsDto();
        accountDetails.setBalance(BigDecimal.ONE);
        accountDetails.setCurrencyCode("USD");
        when(bankClient.getAccountDetails(any())).thenReturn(accountDetails);

        assertThrows(InsufficientFundsException.class, () -> taxService.processTaxes());
    }

    @Test
    public void processTaxes_ValidOrders_CreatesPayments() {
        Order order = new Order();
        order.setTaxAmount(BigDecimal.TEN);
        order.setTaxStatus(TaxStatus.PENDING);
        order.setAccountNumber("123");
        order.setUserId(1L);
        when(orderRepository.findAll()).thenReturn(List.of(order));
        AccountDetailsDto accountDetails = new AccountDetailsDto();
        accountDetails.setBalance(BigDecimal.valueOf(100));
        accountDetails.setCurrencyCode("USD");
        when(bankClient.getAccountDetails(any())).thenReturn(accountDetails);
        TrackedPayment mainPayment = new TrackedPayment();
        mainPayment.setId(1L);
        when(trackedPaymentService.createTrackedPayment(any(), any())).thenReturn(mainPayment);
        TrackedPayment childPayment = new TrackedPayment();
        when(trackedPaymentService.createTrackedPayment(any(), any(), any())).thenReturn(childPayment);

        TrackedPaymentDto result = taxService.processTaxes();

        assertNotNull(result);
        verify(bankClient).handleTax(any());
    }

    @Test
    public void convertTaxAmountIfNeeded_NonUSDCurrency_ConvertsAmount() {
        ConvertDto convertDto = new ConvertDto("USD", "EUR", BigDecimal.TEN);
        when(bankClient.convert(any())).thenReturn(BigDecimal.valueOf(8.5));

        BigDecimal result = taxService.convertTaxAmountIfNeeded(BigDecimal.TEN, "EUR");

        assertEquals(BigDecimal.valueOf(8.5), result);
    }

    @Test
    public void convertTaxAmountIfNeeded_USDCurrency_ReturnsSameAmount() {
        BigDecimal result = taxService.convertTaxAmountIfNeeded(BigDecimal.TEN, "USD");

        assertEquals(BigDecimal.TEN, result);
    }

    @Test
    public void handleSuccessfulTaxPayment_UpdatesStatuses() {
        TrackedPayment taxPayment = new TrackedPayment();
        taxPayment.setId(1L);
        taxPayment.setSecondaryTrackedEntityId(2L);
        when(trackedPaymentService.getTrackedPayment(1L)).thenReturn(taxPayment);

        Order order = new Order();
        order.setTaxStatus(TaxStatus.PENDING);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        TrackedPayment taxProcessor = new TrackedPayment();
        taxProcessor.setId(2L);
        when(trackedPaymentService.getTrackedPayment(2L)).thenReturn(taxProcessor);
        when(trackedPaymentService.getTrackedPaymentsForTaxProcessor(any())).thenReturn(Collections.emptyList());

        taxService.handleSuccessfulTaxPayment(1L);

        assertEquals(TrackedPaymentStatus.SUCCESS, taxPayment.getStatus());
        assertEquals(TaxStatus.PAID, order.getTaxStatus());
        assertEquals(TrackedPaymentStatus.SUCCESS, taxProcessor.getStatus());
    }

    @Test
    public void handleSuccessfulTaxPayment_OrderNotFound_ThrowsException() {
        TrackedPayment taxPayment = new TrackedPayment();
        taxPayment.setId(1L);
        taxPayment.setTrackedEntityId(1L);
        when(trackedPaymentService.getTrackedPayment(1L)).thenReturn(taxPayment);
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> taxService.handleSuccessfulTaxPayment(1L));
    }

    @Test
    public void handleFailedTaxPayment_UpdatesStatus() {
        TrackedPayment taxPayment = new TrackedPayment();
        taxPayment.setId(1L);
        taxPayment.setTrackedEntityId(2L);
        when(trackedPaymentService.getTrackedPayment(1L)).thenReturn(taxPayment);

        TrackedPayment taxProcessor = new TrackedPayment();
        taxProcessor.setId(2L);
        when(trackedPaymentService.getTrackedPayment(2L)).thenReturn(taxProcessor);

        taxService.handleFailedTaxPayment(1L);

        assertEquals(TrackedPaymentStatus.FAIL, taxProcessor.getStatus());
        verify(trackedPaymentRepository).save(taxProcessor);
    }
}