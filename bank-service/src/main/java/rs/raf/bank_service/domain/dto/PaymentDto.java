package rs.raf.bank_service.domain.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class PaymentDto {

    @NotBlank(message = "Sender name is required.")
    private String senderName;

    @NotBlank(message = "Sender account number is required.")
    private String senderAccountNumber;

    @NotBlank(message = "Receiver account number is required.")
    private String receiverAccountNumber;

    @NotNull(message = "Amount is required.")
    @Positive(message = "Amount must be positive.")
    private BigDecimal amount;

    @NotNull(message = "Payment code is required.")
    @NotBlank(message = "Payment code cannot be empty.")
    private String paymentCode;

    @NotNull(message = "Purpose of payment is required.")
    @NotBlank(message = "Purpose of payment cannot be empty.")
    private String purposeOfPayment;

    private String referenceNumber;
}
