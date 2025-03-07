package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.controller.PaymentController;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    void getPaymentsWithFiltersTest() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        LocalDateTime startDate = LocalDateTime.of(2023, 10, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 10, 31, 23, 59);
        BigDecimal minAmount = BigDecimal.valueOf(100);
        BigDecimal maxAmount = BigDecimal.valueOf(1000);
        PaymentStatus paymentStatus = PaymentStatus.COMPLETED;
        String accountNumber = "123456789";
        String cardNumber = "1234123412341234";
        int page = 0;
        int size = 10;

        // Kreirajte testni PaymentOverviewDto
        PaymentOverviewDto paymentOverviewDto = new PaymentOverviewDto();
        paymentOverviewDto.setId(1L);
        paymentOverviewDto.setAmount(BigDecimal.valueOf(100));
        paymentOverviewDto.setDate(LocalDateTime.now());
        paymentOverviewDto.setStatus(PaymentStatus.COMPLETED);

        // Mockujte PaymentService da vrati stranicu sa jednim PaymentOverviewDto
        Page<PaymentOverviewDto> paymentPage = new PageImpl<>(Collections.singletonList(paymentOverviewDto));
        when(paymentService.getPayments(
                any(String.class), any(LocalDateTime.class), any(LocalDateTime.class),
                any(BigDecimal.class), any(BigDecimal.class), any(PaymentStatus.class),
                any(String.class), any(String.class), any(Pageable.class)))
                .thenReturn(paymentPage);

        // Act & Assert
        mockMvc.perform(get("/api/payment")
                        .header("Authorization", token)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("minAmount", minAmount.toString())
                        .param("maxAmount", maxAmount.toString())
                        .param("paymentStatus", paymentStatus.toString())
                        .param("accountNumber", accountNumber)
                        .param("cardNumber", cardNumber)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].amount").value(100))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    @Test
    void getPaymentsWithoutFiltersTest() throws Exception {
        // Arrange
        String token = "Bearer valid-token";
        int page = 0;
        int size = 10;

        // Kreirajte testni PaymentOverviewDto
        PaymentOverviewDto paymentOverviewDto = new PaymentOverviewDto();
        paymentOverviewDto.setId(1L);
        paymentOverviewDto.setAmount(BigDecimal.valueOf(100));
        paymentOverviewDto.setDate(LocalDateTime.now());
        paymentOverviewDto.setStatus(PaymentStatus.COMPLETED);

        // Mockujte PaymentService da vrati stranicu sa jednim PaymentOverviewDto
        Page<PaymentOverviewDto> paymentPage = new PageImpl<>(Collections.singletonList(paymentOverviewDto));
        when(paymentService.getPayments(
                any(String.class), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(paymentPage);

        // Act & Assert
        mockMvc.perform(get("/api/payment")
                        .header("Authorization", token)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].amount").value(100))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

}
