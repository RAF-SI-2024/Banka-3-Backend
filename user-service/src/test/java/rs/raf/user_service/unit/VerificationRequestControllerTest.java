package rs.raf.user_service.unit;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.VerificationRequestController;
import rs.raf.user_service.domain.dto.ClientDto;
import rs.raf.user_service.domain.dto.CreateVerificationRequestDto;
import rs.raf.user_service.domain.entity.VerificationRequest;
import rs.raf.user_service.service.ClientService;
import rs.raf.user_service.service.VerificationRequestService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationRequestControllerTest {

    @Mock
    private VerificationRequestService verificationRequestService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private VerificationRequestController controller;

    private final String mobileUserAgent = "MobileApp/1.0";

    @Test
    void testGetActiveRequests() {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);

        when(verificationRequestService.calledFromMobile(mobileUserAgent)).thenReturn(true);
        when(clientService.getCurrentClient()).thenReturn(clientDto);
        when(verificationRequestService.getActiveRequests(1L)).thenReturn(List.of(new VerificationRequest()));

        ResponseEntity<List<VerificationRequest>> response = controller.getActiveRequests(mobileUserAgent);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(verificationRequestService).getActiveRequests(1L);
    }

    @Test
    void testGetRequestHistory() {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(2L);

        when(verificationRequestService.calledFromMobile(mobileUserAgent)).thenReturn(true);
        when(clientService.getCurrentClient()).thenReturn(clientDto);
        when(verificationRequestService.getRequestHistory(2L)).thenReturn(List.of(new VerificationRequest()));

        ResponseEntity<List<VerificationRequest>> response = controller.getRequestHistory(mobileUserAgent);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(verificationRequestService).getRequestHistory(2L);
    }

    @Test
    void testDenyRequest_Success() {
        when(verificationRequestService.calledFromMobile(mobileUserAgent)).thenReturn(true);
        when(verificationRequestService.denyVerificationRequest(1L, "Bearer token")).thenReturn(true);

        ResponseEntity<String> response = controller.denyRequest(mobileUserAgent,1L, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Request denied", response.getBody());
    }

    @Test
    void testDenyRequest_Failure() {
        when(verificationRequestService.calledFromMobile(mobileUserAgent)).thenReturn(true);
        when(verificationRequestService.denyVerificationRequest(1L, "Bearer token")).thenReturn(false);

        ResponseEntity<String> response = controller.denyRequest(mobileUserAgent,1L, "Bearer token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request not found or already processed", response.getBody());
    }

    @Test
    void testCreateVerificationRequest() {
        CreateVerificationRequestDto dto = new CreateVerificationRequestDto();

        ResponseEntity<String> response = controller.createVerificationRequest(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(verificationRequestService).createVerificationRequest(dto);
    }

    @Test
    void testApproveRequest_Success() {
        when(verificationRequestService.calledFromMobile(mobileUserAgent)).thenReturn(true);
        when(verificationRequestService.processApproval(1L, "Bearer token")).thenReturn(true);

        ResponseEntity<String> response = controller.approveRequest(mobileUserAgent,1L, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Request approved and account limit updated", response.getBody());
    }

    @Test
    void testApproveRequest_Failure() {
        when(verificationRequestService.calledFromMobile(mobileUserAgent)).thenReturn(true);
        when(verificationRequestService.processApproval(1L, "Bearer token")).thenReturn(false);

        ResponseEntity<String> response = controller.approveRequest(mobileUserAgent,1L, "Bearer token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request not found or already processed", response.getBody());
    }
}
