package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.CreditRequestController;
import rs.raf.bank_service.domain.dto.CreditRequestDTO;
import rs.raf.bank_service.domain.entity.CreditRequest;
import rs.raf.bank_service.domain.enums.CreditRequestApproval;
import rs.raf.bank_service.service.CreditRequestService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditRequestControllerTest {

    @Mock
    private CreditRequestService creditRequestService;

    @InjectMocks
    private CreditRequestController creditRequestController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSubmitRequest() {
        // Arrange
        CreditRequest creditRequest = new CreditRequest();
        when(creditRequestService.submitCreditRequest(any(CreditRequest.class))).thenReturn(creditRequest);

        // Act
        ResponseEntity<CreditRequest> response = creditRequestController.submitRequest(creditRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(creditRequestService).submitCreditRequest(any(CreditRequest.class));
    }

    @Test
    void testGetPendingRequests() {
        // Arrange
        when(creditRequestService.getRequestsByStatus(any(CreditRequestApproval.class)))
                .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<CreditRequestDTO>> response = creditRequestController.getPendingRequests(CreditRequestApproval.PENDING);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
        verify(creditRequestService).getRequestsByStatus(CreditRequestApproval.PENDING);
    }

    @Test
    void testAcceptRequest() {
        // Arrange
        CreditRequest creditRequest = new CreditRequest();
        when(creditRequestService.acceptRequest(anyLong())).thenReturn(creditRequest);

        // Act
        ResponseEntity<CreditRequest> response = creditRequestController.acceptRequest(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(creditRequestService).acceptRequest(1L);
    }

    @Test
    void testDenyRequest() {
        // Arrange
        CreditRequest creditRequest = new CreditRequest();
        when(creditRequestService.denyRequest(anyLong())).thenReturn(creditRequest);

        // Act
        ResponseEntity<CreditRequest> response = creditRequestController.denyRequest(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(creditRequestService).denyRequest(1L);
    }

    @Test
    void testPendingRequest() {
        // Arrange
        CreditRequest creditRequest = new CreditRequest();
        when(creditRequestService.pendingRequest(anyLong())).thenReturn(creditRequest);

        // Act
        ResponseEntity<CreditRequest> response = creditRequestController.pendingRequest(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(creditRequestService).pendingRequest(1L);
    }
}
