package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import rs.raf.user_service.client.BankClient;
import rs.raf.user_service.client.StockClient;
import rs.raf.user_service.domain.dto.ActuaryDto;
import rs.raf.user_service.domain.dto.ActuaryLimitDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.entity.ActuaryLimit;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.domain.mapper.EmployeeMapper;
import rs.raf.user_service.exceptions.ActuaryLimitNotFoundException;
import rs.raf.user_service.exceptions.EmployeeNotFoundException;
import rs.raf.user_service.exceptions.UserNotAgentException;
import rs.raf.user_service.repository.ActuaryLimitRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.service.ActuaryService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class ActuaryServiceTest {

    @Mock
    private ActuaryLimitRepository actuaryLimitRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private StockClient stockClient;

    @InjectMocks
    private ActuaryService actuaryService;

    private Employee agentEmployee;
    private Employee nonAgentEmployee;
    private Role agentRole;
    private Role adminRole;
    private ActuaryLimit actuaryLimit;

    @BeforeEach
    void setUp() {
        // Inicijalizacija test podataka
        agentRole = new Role();
        agentRole.setName("AGENT");

        adminRole = new Role();
        adminRole.setName("ADMIN");

        agentEmployee = new Employee();
        agentEmployee.setId(1L);
        agentEmployee.setRole(agentRole);

        nonAgentEmployee = new Employee();
        nonAgentEmployee.setId(2L);
        nonAgentEmployee.setRole(adminRole);

        actuaryLimit = new ActuaryLimit();
        actuaryLimit.setEmployee(agentEmployee);  // Umesto setEmployeeId(...)
        actuaryLimit.setLimitAmount(BigDecimal.valueOf(1000));
        actuaryLimit.setUsedLimit(BigDecimal.ZERO);
        actuaryLimit.setNeedsApproval(false);
    }

    // findAll(...) ------------------------------------------------------------------------------------------------

    @Test
    void testFindAll_Success() {
        // Podesimo mock da vrati neku listu zaposlenih (agentEmployee)
        Page<Employee> employeesPage = new PageImpl<>(List.of(agentEmployee));
        Pageable pageable = PageRequest.of(0, 10);
        when(employeeRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(employeesPage);

        // Poziv servisa
        Page<EmployeeDto> result = actuaryService.findAgents("John", "Doe", "agent@example.com", "AGENT", pageable);

        // Provera rezultata
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        // Proveravamo da li je metoda za pronalazak pozvana
        verify(employeeRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    // changeAgentLimit(...) ---------------------------------------------------------------------------------------

    @Test
    void testChangeAgentLimit_Success() {
        Long employeeId = 1L;
        BigDecimal newLimit = BigDecimal.valueOf(2000);

        // Podesimo mockove: nađemo zaposlenog i ActuaryLimit
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agentEmployee));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(actuaryLimit));

        // Izvršimo metodu
        actuaryService.changeAgentLimit(employeeId, newLimit);

        // Proverimo da li je ActuaryLimit sačuvan sa novim limitom
        assertEquals(newLimit, actuaryLimit.getLimitAmount());
        verify(actuaryLimitRepository, times(1)).save(actuaryLimit);
    }

    @Test
    void testChangeAgentLimit_EmployeeNotFound() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () ->
                actuaryService.changeAgentLimit(employeeId, BigDecimal.valueOf(1000))
        );
        verify(actuaryLimitRepository, never()).findByEmployeeId(anyLong());
    }

    @Test
    void testChangeAgentLimit_UserNotAgent() {
        Long employeeId = 2L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(nonAgentEmployee));

        assertThrows(UserNotAgentException.class, () ->
                actuaryService.changeAgentLimit(employeeId, BigDecimal.valueOf(500))
        );
        verify(actuaryLimitRepository, never()).findByEmployeeId(anyLong());
    }

    @Test
    void testChangeAgentLimit_ActuaryLimitNotFound() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agentEmployee));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ActuaryLimitNotFoundException.class, () ->
                actuaryService.changeAgentLimit(employeeId, BigDecimal.valueOf(300))
        );
        verify(actuaryLimitRepository, never()).save(any(ActuaryLimit.class));
    }

    // resetDailyLimit(...) ---------------------------------------------------------------------------------------

    @Test
    void testResetDailyLimit_Success() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agentEmployee));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(actuaryLimit));

        actuaryService.resetDailyLimit(employeeId);

        assertEquals(BigDecimal.ZERO, actuaryLimit.getUsedLimit());
        verify(actuaryLimitRepository, times(1)).save(actuaryLimit);
    }

    @Test
    void testResetDailyLimit_EmployeeNotFound() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () ->
                actuaryService.resetDailyLimit(employeeId)
        );
        verify(actuaryLimitRepository, never()).findByEmployeeId(anyLong());
    }

    @Test
    void testResetDailyLimit_UserNotAgent() {
        Long employeeId = 2L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(nonAgentEmployee));

        assertThrows(UserNotAgentException.class, () ->
                actuaryService.resetDailyLimit(employeeId)
        );
        verify(actuaryLimitRepository, never()).findByEmployeeId(anyLong());
    }

    @Test
    void testResetDailyLimit_ActuaryLimitNotFound() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agentEmployee));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ActuaryLimitNotFoundException.class, () ->
                actuaryService.resetDailyLimit(employeeId)
        );
        verify(actuaryLimitRepository, never()).save(any(ActuaryLimit.class));
    }

    // setApproval(...) -------------------------------------------------------------------------------------------

    @Test
    void testSetApproval_Success() {
        Long employeeId = 1L;
        boolean approvalValue = true;

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agentEmployee));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(actuaryLimit));

        actuaryService.setApproval(employeeId, approvalValue);

        assertTrue(actuaryLimit.isNeedsApproval());
        verify(actuaryLimitRepository, times(1)).save(actuaryLimit);
    }

    @Test
    void testSetApproval_EmployeeNotFound() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () ->
                actuaryService.setApproval(employeeId, true)
        );
        verify(actuaryLimitRepository, never()).findByEmployeeId(anyLong());
    }

    @Test
    void testSetApproval_UserNotAgent() {
        Long employeeId = 2L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(nonAgentEmployee));

        assertThrows(UserNotAgentException.class, () ->
                actuaryService.setApproval(employeeId, false)
        );
        verify(actuaryLimitRepository, never()).findByEmployeeId(anyLong());
    }

    @Test
    void testSetApproval_ActuaryLimitNotFound() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agentEmployee));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ActuaryLimitNotFoundException.class, () ->
                actuaryService.setApproval(employeeId, true)
        );
        verify(actuaryLimitRepository, never()).save(any(ActuaryLimit.class));
    }

    // getAgentLimit(...) -----------------------------------------------------------------------------------------

    @Test
    void testGetAgentLimit_Success() {
        Long employeeId = 1L;
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(actuaryLimit));

        ActuaryLimitDto dto = actuaryService.getAgentLimit(employeeId);

        assertNotNull(dto);
        assertEquals(actuaryLimit.getLimitAmount(), dto.getLimitAmount());
        assertEquals(actuaryLimit.getUsedLimit(), dto.getUsedLimit());
        assertEquals(actuaryLimit.isNeedsApproval(), dto.isNeedsApproval());
        verify(actuaryLimitRepository, times(1)).findByEmployeeId(employeeId);
    }

    @Test
    void testGetAgentLimit_NotFound() {
        Long employeeId = 999L;
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ActuaryLimitNotFoundException.class, () ->
                actuaryService.getAgentLimit(employeeId)
        );
    }

    // resetDailyLimits() -----------------------------------------------------------------------------------------

    @Test
    void testResetDailyLimits_Success() {
        // Ovde testiramo "scheduled" metodu koja svakodnevno resetuje limit
        ActuaryLimit limit1 = new ActuaryLimit();
        limit1.setUsedLimit(BigDecimal.valueOf(100));

        ActuaryLimit limit2 = new ActuaryLimit();
        limit2.setUsedLimit(BigDecimal.valueOf(200));

        List<ActuaryLimit> limits = List.of(limit1, limit2);
        when(actuaryLimitRepository.findAll()).thenReturn(limits);

        actuaryService.resetDailyLimits();

        assertEquals(BigDecimal.ZERO, limit1.getUsedLimit());
        assertEquals(BigDecimal.ZERO, limit2.getUsedLimit());
        // Proveravamo da li je sve snimljeno
        verify(actuaryLimitRepository, times(1)).saveAll(limits);
    }

}
