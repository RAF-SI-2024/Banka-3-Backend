package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.domain.entity.Payee;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.exceptions.DuplicatePayeeException;
import rs.raf.bank_service.mapper.PayeeMapper;
import rs.raf.bank_service.repository.PayeeRepository;
import rs.raf.bank_service.service.PayeeService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayeeServiceTest {

    @Mock
    private PayeeRepository payeeRepository;

    @Mock
    private PayeeMapper payeeMapper;

    @InjectMocks
    private PayeeService payeeService;

    private Payee payee;
    private PayeeDto payeeDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        payee = new Payee();
        payee.setId(1L);
        payee.setName("John Doe");
        payee.setAccountNumber("123456789");

        payeeDto = new PayeeDto();
        payeeDto.setId(1L);
        payeeDto.setName("John Doe");
        payeeDto.setAccountNumber("123456789");
    }

    @Test
    void getAll_ShouldReturnPayeeList() {
        when(payeeRepository.findAll()).thenReturn(List.of(payee));
        when(payeeMapper.toDto(any(Payee.class))).thenReturn(payeeDto);

        List<PayeeDto> result = payeeService.getAll();

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        verify(payeeRepository, times(1)).findAll();
    }

    @Test
    void create_ShouldCreateNewPayee() {
        when(payeeRepository.findByAccountNumber(payeeDto.getAccountNumber())).thenReturn(Optional.empty());
        when(payeeMapper.toEntity(payeeDto)).thenReturn(payee);
        when(payeeRepository.save(any(Payee.class))).thenReturn(payee);
        when(payeeMapper.toDto(payee)).thenReturn(payeeDto);

        PayeeDto result = payeeService.create(payeeDto);

        assertEquals("John Doe", result.getName());
        verify(payeeRepository, times(1)).save(any(Payee.class));
    }

    @Test
    void create_ShouldThrowException_WhenDuplicateAccountNumber() {
        when(payeeRepository.findByAccountNumber(payeeDto.getAccountNumber())).thenReturn(Optional.of(payee));

        assertThrows(DuplicatePayeeException.class, () -> payeeService.create(payeeDto));

        verify(payeeRepository, never()).save(any(Payee.class));
    }

    @Test
    void update_ShouldUpdateExistingPayee() {
        when(payeeRepository.findById(1L)).thenReturn(Optional.of(payee));
        when(payeeRepository.save(any(Payee.class))).thenReturn(payee);
        when(payeeMapper.toDto(payee)).thenReturn(payeeDto);

        PayeeDto result = payeeService.update(1L, payeeDto);

        assertEquals("John Doe", result.getName());
        verify(payeeRepository, times(1)).save(any(Payee.class));
    }

    @Test
    void update_ShouldThrowException_WhenPayeeNotFound() {
        when(payeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PayeeNotFoundException.class, () -> payeeService.update(1L, payeeDto));

        verify(payeeRepository, never()).save(any(Payee.class));
    }

    @Test
    void delete_ShouldDeleteExistingPayee() {
        when(payeeRepository.findById(1L)).thenReturn(Optional.of(payee));

        payeeService.delete(1L);

        verify(payeeRepository, times(1)).delete(payee);
    }

    @Test
    void delete_ShouldThrowException_WhenPayeeNotFound() {
        when(payeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PayeeNotFoundException.class, () -> payeeService.delete(1L));

        verify(payeeRepository, never()).delete(any(Payee.class));
    }
}
