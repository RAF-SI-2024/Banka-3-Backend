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
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.service.PayeeService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PayeeControllerTest {

    @Mock
    private PayeeService service;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private PayeeController controller;

    @Test
    public void testCreatePayee() {
        // Arrange
        PayeeDto dto = new PayeeDto();
        String token = "validToken";
        Long clientId = 1L;

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.extractUserId(token)).thenReturn(clientId.toString());

        // Act
        ResponseEntity<String> response = controller.createPayee(dto, "Bearer " + token);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Payee created successfully.", response.getBody());
    }

    @Test
    public void testCreatePayee_InvalidToken() {
        // Arrange
        PayeeDto dto = new PayeeDto();
        String token = "invalidToken";

        when(jwtTokenUtil.validateToken(token)).thenReturn(false);

        // Act
        ResponseEntity<String> response = controller.createPayee(dto, "Bearer " + token);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid token", response.getBody());
    }

    @Test
    public void testGetPayeesByClientId() {
        // Arrange
        String token = "validToken";
        Long clientId = 1L;
        PayeeDto payeeDto = new PayeeDto();
        payeeDto.setId(1L);

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.extractUserId(token)).thenReturn(clientId.toString());
        when(service.getByClientId(clientId)).thenReturn(List.of(payeeDto));

        // Act
        ResponseEntity<List<PayeeDto>> response = controller.getPayeesByClientId("Bearer " + token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(payeeDto.getId(), response.getBody().get(0).getId());
    }

    @Test
    public void testUpdatePayee() {
        // Arrange
        Long id = 1L;
        PayeeDto dto = new PayeeDto();
        String token = "validToken";
        Long clientId = 1L;

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.extractUserId(token)).thenReturn(clientId.toString());

        // Act
        ResponseEntity<String> response = controller.updatePayee(id, dto, "Bearer " + token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Payee updated successfully.", response.getBody());
    }

    @Test
    public void testUpdatePayee_PayeeNotFound() {
        // Arrange
        Long id = 1L;
        PayeeDto dto = new PayeeDto();
        String token = "validToken";
        Long clientId = 1L;

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.extractUserId(token)).thenReturn(clientId.toString());
        doThrow(new PayeeNotFoundException(id)).when(service).update(id, dto, clientId);

        // Act
        ResponseEntity<String> response = controller.updatePayee(id, dto, "Bearer " + token);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testDeletePayee() {
        // Arrange
        Long id = 1L;
        String token = "validToken";
        Long clientId = 1L;

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.extractUserId(token)).thenReturn(clientId.toString());

        // Act
        ResponseEntity<Void> response = controller.deletePayee(id, "Bearer " + token);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void testDeletePayee_PayeeNotFound() {
        // Arrange
        Long id = 1L;
        String token = "validToken";
        Long clientId = 1L;

        when(jwtTokenUtil.validateToken(token)).thenReturn(true);
        when(jwtTokenUtil.extractUserId(token)).thenReturn(clientId.toString());
        doThrow(new PayeeNotFoundException(id)).when(service).delete(id, clientId);

        // Act
        ResponseEntity<Void> response = controller.deletePayee(id, "Bearer " + token);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
