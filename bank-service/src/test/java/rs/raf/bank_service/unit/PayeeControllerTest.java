package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.PayeeController;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.DuplicatePayeeException;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.service.PayeeService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PayeeControllerTest {

    @Mock
    private PayeeService service;

    @InjectMocks
    private PayeeController controller;

    @Test
    public void testCreatePayee_Success() {
        // Arrange
        PayeeDto dto = new PayeeDto();
        dto.setAccountNumber("1234567890");
        String token = "validToken";
        Long clientId = 1L;

        when(service.getClientIdFromToken(token)).thenReturn(clientId);
        when(service.create(dto, clientId)).thenReturn(dto);

        // Act
        ResponseEntity<String> response = controller.createPayee(dto, token);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Payee created successfully.", response.getBody());
        verify(service, times(1)).getClientIdFromToken(token);
        verify(service, times(1)).create(dto, clientId);
    }

    @Test
    public void testGetPayeesByClientId_Success() {
        // Arrange
        String token = "validToken";
        Long clientId = 1L;
        PayeeDto payeeDto = new PayeeDto();
        payeeDto.setAccountNumber("1234567890");

        when(service.getClientIdFromToken(token)).thenReturn(clientId);
        when(service.getByClientId(clientId)).thenReturn(List.of(payeeDto));

        // Act
        ResponseEntity<List<PayeeDto>> response = controller.getPayeesByClientId(token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("1234567890", response.getBody().get(0).getAccountNumber());
        verify(service, times(1)).getClientIdFromToken(token);
        verify(service, times(1)).getByClientId(clientId);
    }


    @Test
    public void testGetPayeesByClientId_EmptyList() {
        // Arrange
        String token = "validToken";
        Long clientId = 1L;

        when(service.getClientIdFromToken(token)).thenReturn(clientId);
        when(service.getByClientId(clientId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<PayeeDto>> response = controller.getPayeesByClientId(token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(service, times(1)).getClientIdFromToken(token);
        verify(service, times(1)).getByClientId(clientId);
    }

    @Test
    public void testUpdatePayee_Success() {
        // Arrange
        Long id = 1L;
        PayeeDto dto = new PayeeDto();
        dto.setAccountNumber("0987654321");
        String token = "validToken";
        Long clientId = 1L;

        when(service.getClientIdFromToken(token)).thenReturn(clientId);
        when(service.update(id, dto, clientId)).thenReturn(dto);

        // Act
        ResponseEntity<String> response = controller.updatePayee(id, dto, token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Payee updated successfully.", response.getBody());
        verify(service, times(1)).getClientIdFromToken(token);
        verify(service, times(1)).update(id, dto, clientId);
    }

    @Test
    public void testUpdatePayee_PayeeNotFound() {
        // Arrange
        Long id = 1L;
        PayeeDto dto = new PayeeDto();
        String token = "validToken";
        Long clientId = 1L;

        when(service.getClientIdFromToken(token)).thenReturn(clientId);
        when(service.update(id, dto, clientId)).thenThrow(new PayeeNotFoundException(id));

        // Act
        ResponseEntity<String> response = controller.updatePayee(id, dto, token);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Cannot find payee with id: " + id, response.getBody());
        verify(service, times(1)).getClientIdFromToken(token);
        verify(service, times(1)).update(id, dto, clientId);
    }



    @Test
    public void testDeletePayee_Success() {
        // Arrange
        Long id = 1L;
        String token = "validToken";
        Long clientId = 1L;

        when(service.getClientIdFromToken(token)).thenReturn(clientId);
        doNothing().when(service).delete(id, clientId);

        // Act
        ResponseEntity<Void> response = controller.deletePayee(id, token);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service, times(1)).getClientIdFromToken(token);
        verify(service, times(1)).delete(id, clientId);
    }

    @Test
    public void testDeletePayee_PayeeNotFound() {
        // Arrange
        Long id = 1L;
        String token = "validToken";
        Long clientId = 1L;

        when(service.getClientIdFromToken(token)).thenReturn(clientId);
        doThrow(new PayeeNotFoundException(id)).when(service).delete(id, clientId);

        // Act
        ResponseEntity<Void> response = controller.deletePayee(id, token);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(service, times(1)).getClientIdFromToken(token);
        verify(service, times(1)).delete(id, clientId);
    }

}