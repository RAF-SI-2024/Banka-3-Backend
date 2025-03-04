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
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.service.CreditService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void testGetCreditList() {
        Long id = 1L;

        ResponseEntity<CreditDetailedDTO> response = creditController.getCreditById(id);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(creditService).getCreditById(id);
    }

    @Test
    void testGetDetailedCredit() {
        // Arrange
        Long id = 1L;


        ResponseEntity<CreditDetailedDTO> response = creditController.getCreditById(id);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(creditService).getCreditById(id);
    }

    @Test
    void testCreateCredit() {
        // Arrange
        CreditDetailedDTO creditDto = new CreditDetailedDTO(1L, "a", "a", new BigDecimal(1), 1, new BigDecimal(1), LocalDate.now(), LocalDate.of(3000, 1, 1), new BigDecimal(1), LocalDate.of(2030, 1, 1), new BigDecimal(1), new Currency());


        ResponseEntity<CreditDetailedDTO> response = creditController.createCredit(creditDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(creditService).createCredit(creditDto);
    }
}
