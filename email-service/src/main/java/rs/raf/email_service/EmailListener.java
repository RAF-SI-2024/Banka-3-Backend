package rs.raf.email_service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.raf.email_service.data.EmailType;
import rs.raf.email_service.utils.EmailUtils;

import javax.mail.MessagingException;

@Component
public class EmailListener {

    private final EmailService emailService;

    public EmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "reset-password")
    public void handleResetPassword(EmailRequestDto dto) throws MessagingException {
        String subject = EmailUtils.getEmailSubject(EmailType.RESET_PASSWORD);
        String content = EmailUtils.getEmailContent(EmailType.RESET_PASSWORD, dto.getCode());
        String plain = EmailUtils.getEmailPlainContent(EmailType.RESET_PASSWORD, dto.getCode());
        emailService.sendEmail(dto.getDestination(), subject, plain, content);
    }

    @RabbitListener(queues = "set-password")
    public void handleSetPassword(EmailRequestDto dto) throws MessagingException {
        String subject = EmailUtils.getEmailSubject(EmailType.SET_PASSWORD);
        String content = EmailUtils.getEmailContent(EmailType.SET_PASSWORD, dto.getCode());
        String plain = EmailUtils.getEmailPlainContent(EmailType.SET_PASSWORD, dto.getCode());
        emailService.sendEmail(dto.getDestination(), subject, plain, content);
    }

    @RabbitListener(queues = "activate-client-account")
    public void handleActivateAccount(EmailRequestDto dto) throws MessagingException {
        String subject = EmailUtils.getEmailSubject(EmailType.ACTIVATE_ACCOUNT);
        String content = EmailUtils.getEmailContent(EmailType.ACTIVATE_ACCOUNT, dto.getCode());
        String plain = EmailUtils.getEmailPlainContent(EmailType.ACTIVATE_ACCOUNT, dto.getCode());
        emailService.sendEmail(dto.getDestination(), subject, plain, content);
    }

    @RabbitListener(queues = "request-card")
    public void handleRequestCard(EmailRequestDto dto) throws MessagingException {
        String subject = EmailUtils.getEmailSubject(EmailType.REQUEST_CARD);
        String content = EmailUtils.getEmailContent(EmailType.REQUEST_CARD, dto.getCode());
        String plain = EmailUtils.getEmailPlainContent(EmailType.REQUEST_CARD, dto.getCode());
        emailService.sendEmail(dto.getDestination(), subject, plain, content);
    }

    @RabbitListener(queues = "card-status-change")
    public void handleCardStatusChange(EmailRequestDto dto) throws MessagingException {
        String subject = "Card Status Changed";
        String content = "Your card status has been changed to: " + dto.getCode();
        String plain = "Your card status is now: " + dto.getCode();
        emailService.sendEmail(dto.getDestination(), subject, plain, content);
    }

    @RabbitListener(queues = "card-creation")
    public void handleCardCreation(EmailRequestDto dto) throws MessagingException {
        String subject = "Card Created Successfully";
        String content = "Your new card has been created successfully.";
        String plain = "Your new card has been created successfully.";
        emailService.sendEmail(dto.getDestination(), subject, plain, content);
    }
}