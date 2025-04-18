package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentDto {
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

    private Long callbackId;
}
