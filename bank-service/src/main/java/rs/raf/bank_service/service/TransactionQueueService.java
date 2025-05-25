package rs.raf.bank_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.configuration.RabbitMQConfig;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.TransactionMessageDto;
import rs.raf.bank_service.domain.enums.TransactionType;

@Service
@RequiredArgsConstructor
public class TransactionQueueService {

    private static final String QUEUE_NAME = "transaction-queue";
    private static final String DELAY_QUEUE_NAME = RabbitMQConfig.EXTERNAL_DELAY_QUEUE;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final LoanRequestService loanRequestService;

    public boolean queueTransaction(TransactionType type, Object dto, Long userId) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(dto);
            TransactionMessageDto message = new TransactionMessageDto(type, jsonPayload, userId, System.currentTimeMillis());
            rabbitTemplate.convertAndSend(
                    type.equals(TransactionType.PROCESS_EXTERNAL_PAYMENT) ? DELAY_QUEUE_NAME : QUEUE_NAME, message
            );
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public boolean queueTransaction(TransactionType type, Object dto) {
        return queueTransaction(type, dto, null);
    }

    public LoanDto queueLoan(TransactionType type, Long loanRequestId) {
        if (type.equals(TransactionType.APPROVE_LOAN)) {
            LoanDto loanDto = loanRequestService.returnLoanDto(loanRequestId);

            this.queueTransaction(TransactionType.APPROVE_LOAN, loanRequestId);

            return loanDto;
        }

        throw new UnsupportedOperationException("Unsupported typed transaction: " + type);
    }

}
