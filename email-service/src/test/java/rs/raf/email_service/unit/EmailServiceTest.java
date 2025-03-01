package rs.raf.email_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import rs.raf.email_service.EmailService;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void testSendEmail() throws MessagingException {
        // Arrange
        String destination = "test@example.com";
        String subject = "Test Subject";
        String plainText = "Test Plain";
        String htmlContent = "Test Html";

        // Mock MimeMessage creation
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendEmail(destination, subject, plainText, htmlContent);

        // Assert
        // Verify message creation and sending
        verify(mailSender).createMimeMessage();

        // Capture the sent message
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        // Verify helper configuration (optional)
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}