package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import rs.raf.bank_service.controller.PayeeController;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.service.PayeeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

public class PayeeControllerTest {

    @InjectMocks
    private PayeeController payeeController;

    @Mock
    private PayeeService payeeService;

    @Mock
    private BindingResult bindingResult;

    private PayeeDto testPayeeDto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testPayeeDto = new PayeeDto();
        testPayeeDto.setId(1L);
        testPayeeDto.setName("Test Firma");
        testPayeeDto.setAccountNumber("123456789");
    }

    @Test
    public void testCreatePayee_Success() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(payeeService.create(any(PayeeDto.class))).thenReturn(testPayeeDto);

        ResponseEntity<String> response = payeeController.createPayee("Bearer test-token", testPayeeDto, bindingResult);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("Payee created successfully.", response.getBody());
        verify(payeeService, times(1)).create(any(PayeeDto.class));
    }

    @Test
    public void testCreatePayee_InvalidData() {
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("payeeDto", "naziv", "Naziv ne može biti prazan")));

        ResponseEntity<String> response = payeeController.createPayee("Bearer test-token", new PayeeDto(), bindingResult);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("naziv: Naziv ne može biti prazan", response.getBody());
        verify(payeeService, never()).create(any());
    }

    @Test
    public void testUpdatePayee_Success() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(payeeService.update(anyLong(), any(PayeeDto.class))).thenReturn(testPayeeDto); // Ispravljeno

        ResponseEntity<String> response = payeeController.updatePayee(1L, testPayeeDto, bindingResult);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Payee updated successfully.", response.getBody());
        verify(payeeService, times(1)).update(anyLong(), any(PayeeDto.class));
    }

    @Test
    public void testUpdatePayee_NotFound() {
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new PayeeNotFoundException(testPayeeDto.getId())).when(payeeService).update(anyLong(), any(PayeeDto.class));

        ResponseEntity<String> response = payeeController.updatePayee(999L, testPayeeDto, bindingResult);

        assertEquals(404, response.getStatusCodeValue());
        assertEquals("Cannot find payee with id: " + testPayeeDto.getId(), response.getBody());
    }

    @Test
    public void testDeletePayee_Success() {
        doNothing().when(payeeService).delete(anyLong());

        ResponseEntity<String> response = payeeController.deletePayee(1L);

        assertEquals(204, response.getStatusCodeValue());
        verify(payeeService, times(1)).delete(anyLong());
    }

    @Test
    public void testDeletePayee_NotFound() {
        doThrow(new PayeeNotFoundException(testPayeeDto.getId())).when(payeeService).delete(anyLong());

        ResponseEntity<String> response = payeeController.deletePayee(999L);

        assertEquals(404, response.getStatusCodeValue());
        assertEquals("Cannot find payee with id: " + testPayeeDto.getId(), response.getBody());
    }
}
