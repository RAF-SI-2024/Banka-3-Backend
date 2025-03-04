package rs.raf.user_service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.user_service.entity.VerificationRequest;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.repository.VerificationRequestRepository;
import rs.raf.user_service.service.VerificationRequestService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class VerificationRequestTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @InjectMocks
    private VerificationRequestService verificationRequestService;

    private VerificationRequest request;

    @BeforeEach
    void setUp() {
        request = VerificationRequest.builder()
                .id(1L)
                .userId(100L)
                .email("test@example.com")
                .targetId(200L)
                .status(VerificationStatus.PENDING)
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    @Test
    void testCreateVerificationRequest() {
        verificationRequestService.createVerificationRequest(100L, "test@example.com", 200L);
        verify(verificationRequestRepository, times(1)).save(any(VerificationRequest.class));
    }

    @Test
    void testGetActiveRequests() {
        when(verificationRequestRepository.findByUserIdAndStatus(100L, VerificationStatus.PENDING))
                .thenReturn(Arrays.asList(request));

        List<VerificationRequest> requests = verificationRequestService.getActiveRequests(100L);

        assertFalse(requests.isEmpty());
        assertEquals(1, requests.size());
        assertEquals(100L, requests.get(0).getUserId());
    }

    @Test
    void testUpdateRequestStatus_Success() {
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        boolean result = verificationRequestService.updateRequestStatus(1L, VerificationStatus.APPROVED);

        assertTrue(result);
        assertEquals(VerificationStatus.APPROVED, request.getStatus());
        verify(verificationRequestRepository, times(1)).save(request);
    }

    @Test
    void testUpdateRequestStatus_Fail() {
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = verificationRequestService.updateRequestStatus(1L, VerificationStatus.APPROVED);

        assertFalse(result);
        verify(verificationRequestRepository, never()).save(any());
    }
}
