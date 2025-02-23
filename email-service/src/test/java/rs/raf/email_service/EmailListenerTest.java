package rs.raf.email_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailListener emailListener;

    @Test
    void testSendEmail() {
        // Arrange
        String destination = "test@example.com";
        String code = "123456";

        EmailRequestDto emailRequestDto = new EmailRequestDto();
        emailRequestDto.setCode(code);
        emailRequestDto.setDestination(destination);
        // Act
        emailListener.handleResetPassword(emailRequestDto);

        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }
}
