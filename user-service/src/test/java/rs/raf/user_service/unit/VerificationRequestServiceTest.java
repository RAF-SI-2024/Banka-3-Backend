package rs.raf.user_service.unit;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.user_service.bankClient.BankClient;
import rs.raf.user_service.dto.VerificationRequestDto;
import rs.raf.user_service.entity.VerificationRequest;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.repository.VerificationRequestRepository;
import rs.raf.user_service.service.VerificationRequestService;
import rs.raf.user_service.utils.JwtTokenUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VerificationRequestServiceTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private BankClient bankClient;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private VerificationRequestService verificationRequestService;

    @Test
    void processApproval_Success() {
        Long requestId = 1L;
        Long userIdFromToken = 2L;
        String authHeader = "Bearer valid.jwt.token";

        VerificationRequest mockRequest = new VerificationRequest();
        mockRequest.setId(requestId);
        mockRequest.setUserId(userIdFromToken);
        mockRequest.setTargetId(10L);
        mockRequest.setStatus(VerificationStatus.PENDING);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userIdFromToken);
        when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(mockRequest));

        boolean result = verificationRequestService.processApproval(requestId, authHeader);

        assertTrue(result);
        assertEquals(VerificationStatus.APPROVED, mockRequest.getStatus());
        verify(verificationRequestRepository, times(1)).save(mockRequest);
        verify(bankClient, times(1)).changeAccountLimit(mockRequest.getTargetId());
    }

    @Test
    void processApproval_RequestNotFound() {
        Long requestId = 1L;
        String authHeader = "Bearer valid.jwt.token";

        when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                verificationRequestService.processApproval(requestId, authHeader)
        );
    }

    @Test
    void processApproval_UnauthorizedUser() {
        Long requestId = 1L;
        Long userIdFromToken = 2L;
        Long differentUserId = 3L;
        String authHeader = "Bearer valid.jwt.token";

        VerificationRequest mockRequest = new VerificationRequest();
        mockRequest.setId(requestId);
        mockRequest.setUserId(differentUserId);
        mockRequest.setTargetId(10L);
        mockRequest.setStatus(VerificationStatus.PENDING);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userIdFromToken);
        when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(mockRequest));

        assertThrows(SecurityException.class, () ->
                verificationRequestService.processApproval(requestId, authHeader)
        );
    }

    @Test
    void createVerificationRequest_Success() {
        Long userId = 2L;
        String email = "user@example.com";
        Long targetId = 10L;

        VerificationRequestDto requestDto = VerificationRequestDto.builder()
                .userId(userId)
                .email(email)
                .targetId(targetId)
                .attempts(0).build();

        verificationRequestService.createVerificationRequest(userId, email, targetId);

        verify(verificationRequestRepository, times(1)).save(any(VerificationRequest.class));
    }
}
