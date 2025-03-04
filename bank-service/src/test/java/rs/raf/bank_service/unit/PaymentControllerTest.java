package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.bank_service.controller.PaymentController;
import rs.raf.bank_service.domain.dto.PaymentDto;
import rs.raf.bank_service.domain.dto.TransferDto;
import rs.raf.bank_service.service.PaymentService;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;  // Koristićemo ObjectMapper za pretvaranje objekata u JSON

    @Test
    public void testTransferFunds() throws Exception {
        TransferDto transferDto = new TransferDto();
        transferDto.setSenderAccountNumber("123456789012345678");
        transferDto.setReceiverAccountNumber("987654321098765432");
        transferDto.setAmount(BigDecimal.valueOf(200));

        when(paymentService.transferFunds(transferDto)).thenReturn(true);

        // Perform POST request and assert the response
        mockMvc.perform(post("/api/payment/transfer")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(transferDto)))  // Pretvaramo DTO u JSON
                .andExpect(status().isOk());
    }

    @Test
    public void testMakePayment() throws Exception {
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setSenderAccountNumber("123456789012345678");
        paymentDto.setReceiverAccountNumber("987654321098765432");
        paymentDto.setAmount(BigDecimal.valueOf(100));
        paymentDto.setPaymentCode("289");
        paymentDto.setPurposeOfPayment("Payment for services");

        when(paymentService.makePayment(paymentDto)).thenReturn(true);

        // Perform POST request and assert the response
        mockMvc.perform(post("/api/payment/new")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentDto)))  // Pretvaramo DTO u JSON
                .andExpect(status().isOk());
    }

/**
    @Test
    public void testTransferFunds_InsufficientFunds() throws Exception {
        TransferDto transferDto = new TransferDto();
        transferDto.setSenderAccountNumber("123456789012345678");
        transferDto.setReceiverAccountNumber("987654321098765432");
        transferDto.setAmount(BigDecimal.valueOf(2000));  // Ne dovoljna sredstva

        // Baca se izuzetak sa informacijama o stanju računa i iznosu transfera
        when(paymentService.transferFunds(transferDto))
                .thenThrow(new InsufficientFundsException(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)));

        // Perform POST request and assert the response
        mockMvc.perform(post("/api/payment/transfer")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(transferDto)))  // Pretvaramo DTO u JSON
                .andExpect(status().isBadRequest())  // Očekujemo BadRequest zbog nedovoljnih sredstava
                .andExpect(jsonPath("$.message").value("Insufficient funds: Available balance 1000 is less than transfer amount 2000"));
    }
*/
}
