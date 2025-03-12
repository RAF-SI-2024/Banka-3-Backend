package rs.raf.user_service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.user_service.domain.dto.CreateVerificationRequestDto;
import rs.raf.user_service.domain.entity.VerificationRequest;
import rs.raf.user_service.domain.enums.VerificationStatus;
import rs.raf.user_service.domain.enums.VerificationType;
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
                .targetId(200L)
                .status(VerificationStatus.PENDING)
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    @Test
    void testCreateVerificationRequest() {
        Long userId = 2L;
        Long targetId = 10L;
        CreateVerificationRequestDto requestDto = CreateVerificationRequestDto.builder()
                .userId(userId)
                .targetId(targetId)
                .verificationType(VerificationType.LOGIN).build();
        verificationRequestService.createVerificationRequest(requestDto);
        verify(verificationRequestRepository, times(1)).save(any(VerificationRequest.class));
    }

    @Test
    void testGetActiveRequests() {
        when(verificationRequestRepository.findActiveRequests(100L))
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
