package rs.raf.email_service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailListener {

    private final EmailService emailService;

    public EmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "reset-password")
    public void handleResetPassword(EmailRequestDto dto) {
        String subject = "Reset Password";
        String text = "Your reset code is: " + dto.getCode();
        emailService.sendEmail(dto.getDestination(), subject, text);
    }

    @RabbitListener(queues = "set-password")
    public void handleSetPassword(EmailRequestDto dto) {
        String subject = "Set Your Password";
        String text = "Your password setup code is: " + dto.getCode();
        emailService.sendEmail(dto.getDestination(), subject, text);
    }

    @RabbitListener(queues = "activate-client-account")
    public void handleActivateAccount(EmailRequestDto dto) {
        System.out.println("ovde");
        System.out.println(dto.getDestination()+" "+dto.getCode());
        String subject = "Activate Your Account";
        String text = "Your activation code is: " + dto.getCode();
        emailService.sendEmail(dto.getDestination(), subject, text);
    }

}