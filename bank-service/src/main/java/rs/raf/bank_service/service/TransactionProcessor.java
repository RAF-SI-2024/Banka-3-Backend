package rs.raf.bank_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.domain.dto.TransactionMessageDto;
import rs.raf.bank_service.domain.enums.TransactionType;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionProcessor {

    private final PaymentService paymentService;
    private final LoanRequestService loanRequestService;
    private final TransactionQueueService transactionQueueService;
    private final LoanService loanService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "transaction-queue")
    @Transactional
    public void processTransaction(TransactionMessageDto message) {
        try {
            switch (message.getType()) {
                case CONFIRM_PAYMENT:
                    Long paymentId = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    paymentService.confirmPayment(paymentId);
                    log.info("Processed payment confirmation for id: {}", paymentId);
                    break;

                case CONFIRM_TRANSFER:
                    Long transferId = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    paymentService.confirmTransferAndExecute(transferId);
                    log.info("Processed transfer confirmation for user {}", message.getUserId());
                    break;

                case APPROVE_LOAN:
                    Long requestId = objectMapper.readValue(message.getPayloadJson(), Long.class);

                    loanRequestService.approveLoan(requestId);
                    log.info("Processed loan approval for loan request id {}", requestId);

                    Long loanId = loanService.findLoanIdByLoanRequestId(requestId);
                    transactionQueueService.queueTransaction(TransactionType.PAY_INSTALLMENT, loanId);
                    log.info("Queued PAY_INSTALLMENT for loan id {}", loanId);
                    break;

                case PAY_INSTALLMENT:
                    Long loanid = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    loanService.payInstallment(loanid);
                    log.info("Processed PAY_INSTALLMENT for loan id {}", loanid);
                    break;

                default:
                    log.warn("Unknown transaction type: {}", message.getType());
            }

        } catch (Exception e) {
            log.error("Failed to process transaction: {}", message, e);
        }
    }
}
