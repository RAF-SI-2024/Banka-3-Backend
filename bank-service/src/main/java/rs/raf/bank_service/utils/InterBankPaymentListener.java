package rs.raf.bank_service.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.configuration.RabbitMQConfig;

@Component
@RequiredArgsConstructor
public class InterBankPaymentListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.INTERBANK_PROCESS_QUEUE)
    public void handleDelayedInterbankPayment(Long paymentId) {
        System.out.println("⬅️ Izvrsavanje interbank transfera nakon 15 minuta za ID: " + paymentId);
        paymentService.confirmTransferAndExecute(paymentId);
    }
}
