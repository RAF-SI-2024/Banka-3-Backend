package rs.raf.user_service.unit;


import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
        when(verificationRequestRepository.findActiveRequest(requestId, 2L)).thenReturn(Optional.of(mockRequest));

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

        when(verificationRequestRepository.findActiveRequest(requestId, 1L)).thenReturn(Optional.empty());
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);
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
        mockRequest.setVerificationType(VerificationType.CHANGE_LIMIT);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(userIdFromToken);
        when(verificationRequestRepository.findActiveRequest(requestId, userIdFromToken)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                verificationRequestService.processApproval(requestId, authHeader)
        );
    }

    @Test
    void createVerificationRequest_Success() {
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
    @DisplayName("denyVerificationRequest - should throw VerificationNotFoundException if request not found")
    void testDenyVerificationRequest_NotFound() {
        lenient().when(verificationRequestRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(VerificationNotFoundException.class,
                () -> verificationRequestService.denyVerificationRequest(404L, "Bearer xyz"));
    }


    @Test
    @DisplayName("processApproval - should throw IllegalStateException if request not found")
    void testProcessApproval_NotFound() {
        lenient().when(verificationRequestRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () ->
                verificationRequestService.processApproval(1L, "Bearer abc"));
    }

    @Test
    @DisplayName("processApproval - should throw IllegalStateException if already processed")
    void testProcessApproval_AlreadyProcessed() {
        VerificationRequest request = new VerificationRequest();
        request.setId(2L);
        request.setStatus(VerificationStatus.APPROVED);
        lenient().when(verificationRequestRepository.findById(2L)).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () ->
                verificationRequestService.processApproval(2L, "Bearer abc"));

        verify(bankClient, never()).confirmPayment(anyLong());
        verify(bankClient, never()).confirmTransfer(anyLong());
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    void testUpdateRequestStatus_Found() {
        Long requestId = 1L;
        VerificationRequest request = new VerificationRequest();
        request.setId(requestId);
        request.setStatus(VerificationStatus.PENDING);

        when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(request)).thenReturn(request);

        boolean result = verificationRequestService.updateRequestStatus(requestId, VerificationStatus.APPROVED);

        assertTrue(result);
        assertEquals(VerificationStatus.APPROVED, request.getStatus());
        verify(verificationRequestRepository, times(1)).save(request);
    }

    @Test
    void testUpdateRequestStatus_NotFound() {
        Long requestId = 1L;
        when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        boolean result = verificationRequestService.updateRequestStatus(requestId, VerificationStatus.APPROVED);

        assertFalse(result);
    }

    @Test
    void testGetActiveRequests() {
        Long userId = 1L;
        VerificationRequest req1 = new VerificationRequest();
        req1.setId(1L);
        VerificationRequest req2 = new VerificationRequest();
        req2.setId(2L);
        List<VerificationRequest> activeRequests = List.of(req1, req2);

        when(verificationRequestRepository.findActiveRequests(userId)).thenReturn(activeRequests);

        List<VerificationRequest> result = verificationRequestService.getActiveRequests(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void testGetRequestHistory() {
        Long userId = 1L;
        VerificationRequest req = new VerificationRequest();
        req.setId(3L);
        List<VerificationRequest> history = List.of(req);

        when(verificationRequestRepository.findInactiveRequests(userId)).thenReturn(history);

        List<VerificationRequest> result = verificationRequestService.getRequestHistory(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
    }

    @Test
    void testDenyVerificationRequest_Success_ChangeLimit() {
        Long requestId = 1L;
        Long clientId = 100L;
        String authHeader = "Bearer token";

        VerificationRequest request = new VerificationRequest();
        request.setId(requestId);
        request.setUserId(clientId);
        request.setTargetId(10L);
        request.setStatus(VerificationStatus.PENDING);
        request.setVerificationType(VerificationType.CHANGE_LIMIT);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(verificationRequestRepository.findActiveRequest(requestId, clientId)).thenReturn(Optional.of(request));

        verificationRequestService.denyVerificationRequest(requestId, authHeader);

        assertEquals(VerificationStatus.DENIED, request.getStatus());
        verify(verificationRequestRepository, times(1)).save(request);
        verify(bankClient, times(1)).rejectChangeAccountLimit(request.getTargetId());
    }

    @Test
    void testDenyVerificationRequest_NonPending_ThrowsException() {
        Long requestId = 2L;
        Long clientId = 100L;
        String authHeader = "Bearer token";

        VerificationRequest request = new VerificationRequest();
        request.setId(requestId);
        request.setUserId(clientId);
        request.setTargetId(20L);
        request.setStatus(VerificationStatus.APPROVED); // Nije PENDING
        request.setVerificationType(VerificationType.PAYMENT);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(verificationRequestRepository.findActiveRequest(requestId, clientId)).thenReturn(Optional.of(request));

        assertThrows(RejectNonPendingRequestException.class,
                () -> verificationRequestService.denyVerificationRequest(requestId, authHeader));

        verify(verificationRequestRepository, never()).save(request);
        verify(bankClient, never()).rejectConfirmPayment(anyLong());
    }

    @Test
    void testCalledFromMobile_ReturnsTrue() {
        boolean result = verificationRequestService.calledFromMobile("MobileApp/1.0");
        assertTrue(result);
    }

    @Test
    void testCalledFromMobile_ReturnsFalse_Null() {
        boolean result = verificationRequestService.calledFromMobile(null);
        assertFalse(result);
    }

    @Test
    void testCalledFromMobile_ReturnsFalse_Other() {
        boolean result = verificationRequestService.calledFromMobile("DesktopApp/1.0");
        assertFalse(result);
    }


}