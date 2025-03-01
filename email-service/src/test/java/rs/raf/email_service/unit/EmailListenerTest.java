package rs.raf.email_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.email_service.EmailListener;
import rs.raf.email_service.EmailRequestDto;
import rs.raf.email_service.EmailService;

import javax.mail.MessagingException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailListener emailListener;

    @Test
    void testSendEmail() throws MessagingException {
        // Arrange
        String destination = "test@example.com";
        String code = "123456";

        EmailRequestDto emailRequestDto = new EmailRequestDto();
        emailRequestDto.setCode(code);
        emailRequestDto.setDestination(destination);
        // Act
        emailListener.handleResetPassword(emailRequestDto);

        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }
}
