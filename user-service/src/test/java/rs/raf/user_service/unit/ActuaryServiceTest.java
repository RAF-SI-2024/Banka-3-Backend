package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.user_service.domain.entity.ActuaryLimit;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.exceptions.ActuaryLimitNotFoundException;
import rs.raf.user_service.exceptions.EmployeeNotFoundException;
import rs.raf.user_service.exceptions.UserNotAgentException;
import rs.raf.user_service.repository.ActuaryLimitRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.service.ActuaryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class ActuaryServiceTest {

    @Mock
    private ActuaryLimitRepository actuaryLimitRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private ActuaryService actuaryService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
        @Test
    void changeAgentLimit_Success() {
        Long employeeId = 1L;
        BigDecimal newLimit = new BigDecimal("5000");

        Employee agent = new Employee();
        Role role = new Role();
        role.setName("AGENT");
        agent.setRole(role);

        ActuaryLimit actuaryLimit = new ActuaryLimit();
        actuaryLimit.setEmployee(agent);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agent));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(actuaryLimit));

        actuaryService.changeAgentLimit(employeeId, newLimit);

        assertEquals(newLimit, actuaryLimit.getLimitAmount());
        verify(actuaryLimitRepository, times(1)).save(actuaryLimit);
    }

    @Test
    void changeAgentLimit_ThrowsEmployeeNotFound() {
        Long employeeId = 1L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> actuaryService.changeAgentLimit(employeeId, BigDecimal.TEN));
    }

    @Test
    void changeAgentLimit_ThrowsUserNotAgentException() {
        Long employeeId = 1L;
        Employee nonAgent = new Employee();
        Role role = new Role();
        role.setName("ADMIN");
        nonAgent.setRole(role);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(nonAgent));

        assertThrows(UserNotAgentException.class, () -> actuaryService.changeAgentLimit(employeeId, BigDecimal.TEN));
    }

    @Test
    void resetDailyLimit_Success() {
        Long employeeId = 1L;
        Employee agent = new Employee();
        Role role = new Role();
        role.setName("AGENT");
        agent.setRole(role);

        ActuaryLimit actuaryLimit = new ActuaryLimit();
        actuaryLimit.setEmployee(agent);
        actuaryLimit.setUsedLimit(new BigDecimal("3000"));

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agent));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(actuaryLimit));

        actuaryService.resetDailyLimit(employeeId);

        assertEquals(BigDecimal.ZERO, actuaryLimit.getUsedLimit());
        verify(actuaryLimitRepository, times(1)).save(actuaryLimit);
    }

    @Test
    void resetDailyLimit_ThrowsActuaryLimitNotFound() {
        Long employeeId = 1L;
        Employee agent = new Employee();
        Role role = new Role();
        role.setName("AGENT");
        agent.setRole(role);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agent));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ActuaryLimitNotFoundException.class, () -> actuaryService.resetDailyLimit(employeeId));
    }

    @Test
    void resetDailyLimits_Success() {
        ActuaryLimit limit1 = new ActuaryLimit();
        ActuaryLimit limit2 = new ActuaryLimit();
        limit1.setUsedLimit(new BigDecimal("1000"));
        limit2.setUsedLimit(new BigDecimal("500"));

        List<ActuaryLimit> limits = List.of(limit1, limit2);

        when(actuaryLimitRepository.findAll()).thenReturn(limits);

        actuaryService.resetDailyLimits();

        assertEquals(BigDecimal.ZERO, limit1.getUsedLimit());
        assertEquals(BigDecimal.ZERO, limit2.getUsedLimit());
        verify(actuaryLimitRepository, times(1)).saveAll(limits);
    }
    @Test
    void setApproval_Success() {
        Long employeeId = 1L;
        boolean approvalValue = true;

        Employee agent = new Employee();
        Role role = new Role();
        role.setName("AGENT");
        agent.setRole(role);

        ActuaryLimit actuaryLimit = new ActuaryLimit();
        actuaryLimit.setEmployee(agent);
        actuaryLimit.setNeedsApproval(false);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agent));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(actuaryLimit));

        actuaryService.setApproval(employeeId, approvalValue);

        assertEquals(approvalValue, actuaryLimit.isNeedsApproval());
        verify(actuaryLimitRepository, times(1)).save(actuaryLimit);
    }
    @Test
    void setApproval_ThrowsEmployeeNotFoundException() {
        Long employeeId = 1L;
        boolean approvalValue = true;

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class, () -> actuaryService.setApproval(employeeId, approvalValue));
    }

    @Test
    void setApproval_ThrowsUserNotAgentException() {
        Long employeeId = 1L;
        boolean approvalValue = true;

        // Kreiranje objekta Employee sa rodom "ADMIN", ne AGENT
        Employee nonAgent = new Employee();
        Role role = new Role();
        role.setName("ADMIN");
        nonAgent.setRole(role);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(nonAgent));
        assertThrows(UserNotAgentException.class, () -> actuaryService.setApproval(employeeId, approvalValue));
    }

    @Test
    void setApproval_ThrowsActuaryLimitNotFoundException() {
        Long employeeId = 1L;
        boolean approvalValue = true;

        // Kreiranje objekta Employee sa rodom "AGENT"
        Employee agent = new Employee();
        Role role = new Role();
        role.setName("AGENT");
        agent.setRole(role);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(agent));
        when(actuaryLimitRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());
        assertThrows(ActuaryLimitNotFoundException.class, () -> actuaryService.setApproval(employeeId, approvalValue));
    }
}
