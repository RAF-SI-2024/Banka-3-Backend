package rs.raf.bank_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.TransactionMessageDto;

@Service
@RequiredArgsConstructor
public class TransactionQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final LoanRequestService loanRequestService;

    private static final String QUEUE_NAME = "transaction-queue";

    public boolean queueTransaction(String type, Object dto, Long userId) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(dto);
            TransactionMessageDto message = new TransactionMessageDto(type, jsonPayload, userId, System.currentTimeMillis());
            rabbitTemplate.convertAndSend(QUEUE_NAME, message);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public boolean queueTransaction(String type, Object dto) {
       return queueTransaction(type, dto, null);
    }

    public LoanDto queueLoan(String type, Long loanRequestId) {
        if (type.equals("APPROVE_LOAN")) {
            LoanDto loanDto = loanRequestService.returnLoanDto(loanRequestId);

            this.queueTransaction("APPROVE_LOAN", loanRequestId);

            return loanDto;
        }

        throw new UnsupportedOperationException("Unsupported typed transaction: " + type);
    }

}
