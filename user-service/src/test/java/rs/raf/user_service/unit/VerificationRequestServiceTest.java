package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.user_service.client.BankClient;
import rs.raf.user_service.domain.dto.CreateVerificationRequestDto;
import rs.raf.user_service.domain.entity.VerificationRequest;
import rs.raf.user_service.domain.enums.VerificationStatus;
import rs.raf.user_service.domain.enums.VerificationType;
import rs.raf.user_service.exceptions.RejectNonPendingRequestException;
import rs.raf.user_service.exceptions.VerificationNotFoundException;
import rs.raf.user_service.repository.VerificationRequestRepository;
import rs.raf.user_service.service.VerificationRequestService;
import rs.raf.user_service.utils.JwtTokenUtil;

import java.time.LocalDateTime;
import java.util.List;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --------------------------------------------------------------------------------
    // processApproval(...)
    // --------------------------------------------------------------------------------
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
        mockRequest.setVerificationType(VerificationType.CHANGE_LIMIT);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userIdFromToken);
        when(verificationRequestRepository.findActiveRequest(requestId, userIdFromToken))
                .thenReturn(Optional.of(mockRequest));

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

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);
        when(verificationRequestRepository.findActiveRequest(requestId, 1L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                verificationRequestService.processApproval(requestId, authHeader)
        );
    }
/*
    @Test
    void createVerificationRequest_Success() {
        Long userId = 2L;
        Long targetId = 10L;

        CreateVerificationRequestDto requestDto = CreateVerificationRequestDto.builder()
                .userId(userId)
                .targetId(targetId)
                .verificationType(VerificationType.LOGIN)
                .build();

        verificationRequestService.createVerificationRequest(requestDto);

        verify(verificationRequestRepository, times(1)).save(any(VerificationRequest.class));
    }
*/
    // --------------------------------------------------------------------------------
    // getActiveRequests(...)
    // --------------------------------------------------------------------------------
    /*
    @Test
    void testGetActiveRequests() {
        Long userId = 3L;
        VerificationRequest vr1 = new VerificationRequest();
        vr1.setId(100L);
        vr1.setUserId(userId);
        vr1.setStatus(VerificationStatus.PENDING);

        VerificationRequest vr2 = new VerificationRequest();
        vr2.setId(101L);
        vr2.setUserId(userId);
        vr2.setStatus(VerificationStatus.PENDING);

        when(verificationRequestRepository.findActiveRequests(userId)).thenReturn(List.of(vr1, vr2));

        List<VerificationRequest> result = verificationRequestService.getActiveRequests(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(verificationRequestRepository, times(1)).findActiveRequests(userId);
    }
*/
    // --------------------------------------------------------------------------------
    // getRequestHistory(...)
    // --------------------------------------------------------------------------------
    /*
    @Test
    void testGetRequestHistory() {
        Long userId = 5L;
        VerificationRequest vr1 = new VerificationRequest();
        vr1.setId(200L);
        vr1.setUserId(userId);
        vr1.setStatus(VerificationStatus.APPROVED);

        when(verificationRequestRepository.findInactiveRequests(userId)).thenReturn(List.of(vr1));

        List<VerificationRequest> result = verificationRequestService.getRequestHistory(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(VerificationStatus.APPROVED, result.get(0).getStatus());
        verify(verificationRequestRepository, times(1)).findInactiveRequests(userId);
    }
*/
    // --------------------------------------------------------------------------------
    // updateRequestStatus(...)
    // --------------------------------------------------------------------------------
    /*
    @Test
    void testUpdateRequestStatus_Success() {
        Long reqId = 10L;
        VerificationRequest vr = new VerificationRequest();
        vr.setId(reqId);
        vr.setStatus(VerificationStatus.PENDING);

        when(verificationRequestRepository.findById(reqId)).thenReturn(Optional.of(vr));

        boolean updated = verificationRequestService.updateRequestStatus(reqId, VerificationStatus.APPROVED);

        assertTrue(updated);
        assertEquals(VerificationStatus.APPROVED, vr.getStatus());
        verify(verificationRequestRepository, times(1)).save(vr);
    }
*/
    @Test
    void testUpdateRequestStatus_NotFound() {
        when(verificationRequestRepository.findById(999L)).thenReturn(Optional.empty());

        boolean updated = verificationRequestService.updateRequestStatus(999L, VerificationStatus.DENIED);
        assertFalse(updated);
        verify(verificationRequestRepository, never()).save(any());
    }

    // --------------------------------------------------------------------------------
    // denyVerificationRequest(...)
    // --------------------------------------------------------------------------------
    @Test
    void testDenyVerificationRequest_Success() {
        Long requestId = 111L;
        String authHeader = "Bearer valid.jwt.token";
        Long userId = 12L;

        VerificationRequest mockRequest = new VerificationRequest();
        mockRequest.setId(requestId);
        mockRequest.setUserId(userId);
        mockRequest.setTargetId(20L);
        mockRequest.setStatus(VerificationStatus.PENDING);
        mockRequest.setVerificationType(VerificationType.PAYMENT);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(verificationRequestRepository.findActiveRequest(requestId, userId))
                .thenReturn(Optional.of(mockRequest));

        verificationRequestService.denyVerificationRequest(requestId, authHeader);

        assertEquals(VerificationStatus.DENIED, mockRequest.getStatus());
        verify(verificationRequestRepository, times(1)).save(mockRequest);
        verify(bankClient, times(1)).rejectConfirmPayment(20L);
    }

    @Test
    void testDenyVerificationRequest_NotFound() {
        Long requestId = 222L;
        String authHeader = "Bearer token";
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(50L);
        when(verificationRequestRepository.findActiveRequest(requestId, 50L)).thenReturn(Optional.empty());

        assertThrows(VerificationNotFoundException.class, () ->
                verificationRequestService.denyVerificationRequest(requestId, authHeader)
        );
        verify(verificationRequestRepository, never()).save(any());
        verify(bankClient, never()).rejectConfirmPayment(anyLong());
    }
/*
    @Test
    void testDenyVerificationRequest_NotPending() {
        Long requestId = 333L;
        Long userId = 70L;
        String authHeader = "Bearer token";

        VerificationRequest mockRequest = new VerificationRequest();
        mockRequest.setId(requestId);
        mockRequest.setUserId(userId);
        mockRequest.setStatus(VerificationStatus.APPROVED);
        mockRequest.setVerificationType(VerificationType.PAYMENT);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userId);
        when(verificationRequestRepository.findActiveRequest(requestId, userId)).thenReturn(Optional.of(mockRequest));

        assertThrows(RejectNonPendingRequestException.class, () ->
                verificationRequestService.denyVerificationRequest(requestId, authHeader)
        );
        verify(verificationRequestRepository, never()).save(any());
        verify(bankClient, never()).rejectConfirmPayment(anyLong());
    }
*/
    // --------------------------------------------------------------------------------
    // calledFromMobile(...)
    // --------------------------------------------------------------------------------
    @Test
    void testCalledFromMobile_True() {
        assertTrue(verificationRequestService.calledFromMobile("MobileApp/1.0"));
    }

    @Test
    void testCalledFromMobile_False_NullUserAgent() {
        assertFalse(verificationRequestService.calledFromMobile(null));
    }

    @Test
    void testCalledFromMobile_False_OtherUserAgent() {
        assertFalse(verificationRequestService.calledFromMobile("Mozilla/5.0"));
    }
}
