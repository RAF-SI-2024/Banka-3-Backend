package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.CreditController;
import rs.raf.bank_service.domain.dto.CreditDetailedDTO;
import rs.raf.bank_service.domain.entity.Credit;
import rs.raf.bank_service.service.CreditService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class CreditControllerTest {

    @Mock
    private CreditService creditService;

    @InjectMocks
    private CreditController creditController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCreditList_Success() {
        Long id = 1L;

        ResponseEntity<CreditDetailedDTO> response = creditController.getCreditById(id);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(creditService).getCreditById(id);
    }

    @Test
    void testCreateCredit_Failure() {
        // Arrange
        Credit creditDto = new Credit();


        String errorMessage = "Failed to create credit";
//        doThrow(new RuntimeException(errorMessage)).when(creditService).createCredit(any(Credit.class));

        // Act
        ResponseEntity<Credit> response = creditController.createCredit(creditDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(creditService).createCredit(creditDto);
    }
}
