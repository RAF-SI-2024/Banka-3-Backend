package rs.raf.bank_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.configuration.RabbitMQConfig;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.enums.TransactionType;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionProcessor {

    private final PaymentService paymentService;
    private final LoanRequestService loanRequestService;
    private final LoanService loanService;
    private final ObjectMapper objectMapper;
    private final PaymentCallbackService paymentCallbackService;
    private final TransactionQueueService transactionQueueService;

    @RabbitListener(queues = "transaction-queue")
    @Transactional
    public void processTransaction(TransactionMessageDto message) {
        try {
            switch (message.getType()) {
                case CONFIRM_PAYMENT: {
                    Long paymentId = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    paymentService.confirmPayment(paymentId);
                    log.info("Processed payment/transfer confirmation for id: {}", paymentId);
                    break;
                }

                case REJECT_PAYMENT: {
                    Long paymentId = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    paymentService.rejectPayment(paymentId);
                    log.info("Processed payment/transfer reject for id: {}", paymentId);
                    break;
                }
                case CREATE_PAYMENT: {
                    CreatePaymentDto createPaymentDto = objectMapper.readValue(message.getPayloadJson(), CreatePaymentDto.class);
                    paymentService.createPaymentAndVerificationRequest(createPaymentDto, message.getUserId());
                    log.info("Processed payment creation: {}", createPaymentDto);
                    break;
                }
                case CREATE_TRANSFER: {
                    TransferDto transferDto = objectMapper.readValue(message.getPayloadJson(), TransferDto.class);
                    paymentService.createTransferAndVerificationRequest(transferDto, message.getUserId());
                    log.info("Processed transfer creation: {}", transferDto);
                    break;
                }

                case SYSTEM_PAYMENT: {
                    CreatePaymentDto createPaymentDto = null;
                    try {
                        createPaymentDto = objectMapper.readValue(message.getPayloadJson(), CreatePaymentDto.class);
                        PaymentDetailsDto paymentDetailsDto = paymentService.createAndExecuteSystemPayment(createPaymentDto, message.getUserId());
                        if (createPaymentDto.getCallbackId() != null) {
                            System.out.println(paymentDetailsDto);
                            paymentCallbackService.notifySuccess(createPaymentDto.getCallbackId());
                        }
                    } catch (Exception e) {
                        log.error("Error executing system payment", e);
                        if (createPaymentDto != null && createPaymentDto.getCallbackId() != null) {
                            paymentCallbackService.notifyFailure(createPaymentDto.getCallbackId());
                        }
                    }

                    break;
                }

                case APPROVE_LOAN: {
                    Long requestId = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    loanRequestService.approveLoan(requestId);
                    log.info("Processed loan approval for loan request id {}", requestId);
                    break;
                }
                case PAY_INSTALLMENT: {
                    Long loanid = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    loanService.payInstallment(loanid);
                    log.info("Processed PAY_INSTALLMENT for loan id {}", loanid);
                    break;
                }

                case PROCESS_EXTERNAL_PAYMENT: {
                    Long paymentId = objectMapper.readValue(message.getPayloadJson(), Long.class);
                    try {
                        paymentService.processExternalPayment(paymentId);
                        log.info("Processed external payment for id: {}", paymentId);
                    }
                    catch (Exception e) {
                        log.error("Error executing external payment", e);
                    }

                    break;
                }


                default: {
                    log.warn("Unknown transaction type: {}", message.getType());
                }
            }

        } catch (Exception e) {
            log.error("Failed to process transaction: {}", message, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.EXTERNAL_DELAY_QUEUE)
    @Transactional
    public void handleExternalPayment(Long paymentId) {
        log.info("[Interbank] Received delayed payment for processing: {}", paymentId);
        try {
            transactionQueueService.queueTransaction(TransactionType.PROCESS_EXTERNAL_PAYMENT, paymentId);
        } catch (Exception e) {
            log.error("[Interbank] Fatal error while processing payment {}: {}", paymentId, e.getMessage(), e);
        }
    }
}
