package rs.raf.email_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.email_service.EmailListener;
import rs.raf.email_service.EmailRequestDto;
import rs.raf.email_service.EmailService;
import rs.raf.email_service.data.EmailType;
import rs.raf.email_service.utils.EmailUtils;

import javax.mail.MessagingException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailListener emailListener;

    private EmailRequestDto dto;

    @BeforeEach
    void setUp() {
        dto = new EmailRequestDto();
        dto.setDestination("test@example.com");
        dto.setCode("123456");
    }

    @Test
    void testHandleResetPassword() throws MessagingException {
        emailListener.handleResetPassword(dto);
        verify(emailService).sendEmail(
                dto.getDestination(),
                EmailUtils.getEmailSubject(EmailType.RESET_PASSWORD),
                EmailUtils.getEmailPlainContent(EmailType.RESET_PASSWORD, dto.getCode()),
                EmailUtils.getEmailContent(EmailType.RESET_PASSWORD, dto.getCode())
        );
    }

    @Test
    void testHandleSetPassword() throws MessagingException {
        emailListener.handleSetPassword(dto);
        verify(emailService).sendEmail(
                dto.getDestination(),
                EmailUtils.getEmailSubject(EmailType.SET_PASSWORD),
                EmailUtils.getEmailPlainContent(EmailType.SET_PASSWORD, dto.getCode()),
                EmailUtils.getEmailContent(EmailType.SET_PASSWORD, dto.getCode())
        );
    }

    @Test
    void testHandleActivateAccount() throws MessagingException {
        emailListener.handleActivateAccount(dto);
        verify(emailService).sendEmail(
                dto.getDestination(),
                EmailUtils.getEmailSubject(EmailType.ACTIVATE_ACCOUNT),
                EmailUtils.getEmailPlainContent(EmailType.ACTIVATE_ACCOUNT, dto.getCode()),
                EmailUtils.getEmailContent(EmailType.ACTIVATE_ACCOUNT, dto.getCode())
        );
    }

    @Test
    void testHandleRequestCard() throws MessagingException {
        emailListener.handleRequestCard(dto);
        verify(emailService).sendEmail(
                dto.getDestination(),
                EmailUtils.getEmailSubject(EmailType.REQUEST_CARD),
                EmailUtils.getEmailPlainContent(EmailType.REQUEST_CARD, dto.getCode()),
                EmailUtils.getEmailContent(EmailType.REQUEST_CARD, dto.getCode())
        );
    }

    @Test
    void testHandleCardStatusChange() throws MessagingException {
        emailListener.handleCardStatusChange(dto);
        verify(emailService).sendEmail(
                dto.getDestination(),
                "Card Status Changed",
                "Your card status is now: " + dto.getCode(),
                "Your card status has been changed to: " + dto.getCode()
        );
    }

    @Test
    void testHandleCardCreation() throws MessagingException {
        emailListener.handleCardCreation(dto);
        verify(emailService).sendEmail(
                dto.getDestination(),
                "Card Created Successfully",
                "Your new card has been created successfully.",
                "Your new card has been created successfully."
        );
    }

    @Test
    void testHandleInsufficientFunds() throws MessagingException {
        emailListener.handleInsufficientFunds(dto);
        verify(emailService).sendEmail(
                dto.getDestination(),
                "Insufficient funds",
                "Please pay your loans.",
                "Please pay your loans."
        );
    }
}
