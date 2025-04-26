package rs.raf.bank_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.client.Banka2Client;
import rs.raf.bank_service.domain.dto.Bank2PaymentResponseDto;
import rs.raf.bank_service.domain.dto.InterbankPaymentDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.exceptions.PaymentNotFoundException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterbankPaymentSender {

    private final Banka2Client banka2Client;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    @Transactional
    public void sendPaymentToBank2WithRetry(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getStatus().equals(PaymentStatus.PENDING_CONFIRMATION)) {
            log.warn("Payment is not in PENDING_CONFIRMATION state, skipping.");
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Attempt #{} to send payment {} to bank 2", attempt, paymentId);
                InterbankPaymentDto dto = mapToDto(payment);

                Bank2PaymentResponseDto response = banka2Client.sendPaymentToBank2(dto);

                if (!response.getSuccess()) {
                    throw new RuntimeException("Bank 2 returned failure: " + response.getMessage());
                }

                // Skidamo novac jer je Banka 2 potvrdila
                Account sender = payment.getSenderAccount();
                sender.setBalance(sender.getBalance().subtract(payment.getAmount()));
                sender.setAvailableBalance(sender.getBalance());
                accountRepository.save(sender);

                payment.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);

                log.info("Successfully completed interbank payment {} on attempt {}", paymentId, attempt);
                return;

            } catch (Exception ex) {
                log.error("Failed to send payment to bank 2 on attempt {}: {}", attempt, ex.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    failPaymentAndRollback(payment);
                    log.error("All retry attempts failed. Payment {} marked as CANCELED.", paymentId);
                }
            }
        }
    }

    private InterbankPaymentDto mapToDto(Payment payment) {
        return InterbankPaymentDto.builder()
                .fromAccountNumber(payment.getSenderAccount().getAccountNumber())
                .toAccountNumber(payment.getAccountNumberReceiver())
                .amount(payment.getAmount())
                .currencyCode(payment.getSenderAccount().getCurrency().getCode())
                .paymentCode(payment.getPaymentCode())
                .purpose(payment.getPurposeOfPayment())
                .referenceNumber(payment.getReferenceNumber())
                .build();
    }

    private void failPaymentAndRollback(Payment payment) {
        Account sender = payment.getSenderAccount();
        sender.setAvailableBalance(sender.getAvailableBalance().add(payment.getAmount()));
        accountRepository.save(sender);

        payment.setStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);
    }
}
