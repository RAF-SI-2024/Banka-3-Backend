package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.domain.entity.Payee;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.DuplicatePayeeException;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.domain.mapper.PayeeMapper;
import rs.raf.bank_service.repository.PayeeRepository;
import rs.raf.bank_service.service.PayeeService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PayeeServiceTest {

    @Mock
    private PayeeRepository repository;

    @Mock
    private PayeeMapper mapper;

    @InjectMocks
    private PayeeService service;

    @Test
    public void testGetByClientId() {
        // Arrange
        Long clientId = 1L;
        Payee payee = new Payee();
        payee.setId(1L);
        payee.setClientId(clientId);
        PayeeDto payeeDto = new PayeeDto();
        payeeDto.setId(1L);

        when(repository.findByClientId(clientId)).thenReturn(List.of(payee));
        when(mapper.toDto(payee)).thenReturn(payeeDto);

        // Act
        List<PayeeDto> result = service.getByClientId(clientId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(payeeDto.getId(), result.get(0).getId());
    }

    @Test
    public void testCreate() {
        // Arrange
        Long clientId = 1L;
        PayeeDto dto = new PayeeDto();
        dto.setAccountNumber("1234567890");
        Payee payee = new Payee();
        payee.setId(1L);
        payee.setClientId(clientId);
        payee.setAccountNumber("1234567890");

        // Ispravljeno: when se poziva na repository, a ne na Optional
        when(repository.findByAccountNumberAndClientId(dto.getAccountNumber(), clientId)).thenReturn(Optional.empty());
        when(mapper.toEntity(dto)).thenReturn(payee);
        when(repository.save(payee)).thenReturn(payee);
        when(mapper.toDto(payee)).thenReturn(dto);

        // Act
        PayeeDto result = service.create(dto, clientId);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getAccountNumber(), result.getAccountNumber());
    }


    @Test
    public void testUpdate() {
        // Arrange
        Long id = 1L;
        Long clientId = 1L;
        PayeeDto dto = new PayeeDto();
        dto.setName("New Name");
        dto.setAccountNumber("0987654321");

        Payee payee = new Payee();
        payee.setId(id);
        payee.setClientId(clientId);

        when(repository.findById(id)).thenReturn(Optional.of(payee));
        when(repository.save(payee)).thenReturn(payee);
        when(mapper.toDto(payee)).thenReturn(dto);

        // Act
        PayeeDto result = service.update(id, dto, clientId);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getName(), result.getName());
        assertEquals(dto.getAccountNumber(), result.getAccountNumber());
    }

    @Test
    public void testUpdate_PayeeNotFound() {
        // Arrange
        Long id = 1L;
        Long clientId = 1L;
        PayeeDto dto = new PayeeDto();

        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PayeeNotFoundException.class, () -> service.update(id, dto, clientId));
    }

    @Test
    public void testUpdate_ClientNotFound() {
        // Arrange
        Long id = 1L;
        Long clientId = 2L;
        PayeeDto dto = new PayeeDto();
        Payee payee = new Payee();
        payee.setId(id);
        payee.setClientId(1L);

        when(repository.findById(id)).thenReturn(Optional.of(payee));

        // Act & Assert
        assertThrows(ClientNotFoundException.class, () -> service.update(id, dto, clientId));
    }

    @Test
    public void testDelete() {
        // Arrange
        Long id = 1L;
        Long clientId = 1L;
        Payee payee = new Payee();
        payee.setId(id);
        payee.setClientId(clientId);

        when(repository.findById(id)).thenReturn(Optional.of(payee));

        // Act
        service.delete(id, clientId);

        // Assert
        verify(repository, times(1)).delete(payee);
    }

    @Test
    public void testDelete_PayeeNotFound() {
        // Arrange
        Long id = 1L;
        Long clientId = 1L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PayeeNotFoundException.class, () -> service.delete(id, clientId));
    }

    @Test
    public void testDelete_ClientNotFound() {
        // Arrange
        Long id = 1L;
        Long clientId = 2L; // Različit od clientId u Payee
        Payee payee = new Payee();
        payee.setId(id);
        payee.setClientId(1L); // Različit od clientId u zahtevu

        when(repository.findById(id)).thenReturn(Optional.of(payee));

        // Act & Assert
        assertThrows(ClientNotFoundException.class, () -> service.delete(id, clientId));
    }

    @Test
    public void testCreate_DuplicatePayeeForSameClient() {
        // Arrange
        PayeeDto dto = new PayeeDto();
        dto.setAccountNumber("1234567890");
        Long clientId = 1L;

        Payee existingPayee = new Payee();
        existingPayee.setId(1L);
        existingPayee.setAccountNumber("1234567890");
        existingPayee.setClientId(clientId);

        when(repository.findByAccountNumberAndClientId(dto.getAccountNumber(), clientId)).thenReturn(Optional.of(existingPayee));

        // Act & Assert
        assertThrows(DuplicatePayeeException.class, () -> service.create(dto, clientId));
    }

}
