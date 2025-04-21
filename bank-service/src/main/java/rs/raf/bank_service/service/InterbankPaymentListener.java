package rs.raf.bank_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.configuration.RabbitMQConfig;
import rs.raf.bank_service.service.InterbankPaymentSender;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterbankPaymentListener {

    private final InterbankPaymentSender interbankPaymentSender;

    @RabbitListener(queues = RabbitMQConfig.INTERBANK_DELAY_QUEUE)
    @Transactional
    public void handleInterbankPayment(Long paymentId) {
        log.info("[Interbank] Received delayed payment for processing: {}", paymentId);
        try {
            interbankPaymentSender.sendPaymentToBank2WithRetry(paymentId);
        } catch (Exception e) {
            log.error("[Interbank] Fatal error while processing payment {}: {}", paymentId, e.getMessage(), e);
        }
    }
}
