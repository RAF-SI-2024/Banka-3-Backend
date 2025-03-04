package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.CreditTransactionController;
import rs.raf.bank_service.domain.dto.CreditTransactionDTO;
import rs.raf.bank_service.service.CreditTransactionService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditTransactionControllerTest {

    @Mock
    private CreditTransactionService creditTransactionService;

    @InjectMocks
    private CreditTransactionController creditTransactionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindByCreditID() {
        // Arrange
        when(creditTransactionService.getTransactionsByCreditId(anyLong()))
                .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<CreditTransactionDTO>> response = creditTransactionController.findByCreditID(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
        verify(creditTransactionService).getTransactionsByCreditId(1L);
    }
}
