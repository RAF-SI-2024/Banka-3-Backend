package rs.raf.user_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.service.EmployeeService;
import rs.raf.user_service.entity.EmployeeDTO;
import rs.raf.user_service.entity.Employee;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void testFindById() {
        Employee employee = new Employee();
        employee.setUsername("marko123");
        employee.setPosition("Manager");
        employee.setDepartment("Finance");
        employee.setActive(true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeDTO result = employeeService.findById(1L);

        assertNotNull(result);
        assertEquals("marko123", result.getUsername());
        assertEquals("Manager", result.getPosition());
    }

    @Test
    void testFindAllWithPaginationAndFilters() {
        Employee employee = new Employee();
        employee.setUsername("jovan456");
        employee.setPosition("Developer");
        employee.setDepartment("IT");
        employee.setActive(true);

        Page<Employee> page = new PageImpl<>(Collections.singletonList(employee));

        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<EmployeeDTO> result = employeeService.findAll("Developer", "IT", true, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Developer", result.getContent().get(0).getPosition());
        assertEquals("IT", result.getContent().get(0).getDepartment());
        assertTrue(result.getContent().get(0).isActive());
    }

    @Test
    void testFindAllWithoutFilters() {
        Employee emp1 = new Employee();
        emp1.setUsername("ana789");
        emp1.setPosition("HR");
        emp1.setDepartment("Human Resources");
        emp1.setActive(true);

        Employee emp2 = new Employee();
        emp2.setUsername("ivan321");
        emp2.setPosition("Designer");
        emp2.setDepartment("Creative");
        emp2.setActive(false);

        Page<Employee> page = new PageImpl<>(List.of(emp1, emp2));

        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<EmployeeDTO> result = employeeService.findAll(null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void testFindByIdNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.findById(99L));

        assertEquals("Employee not found", exception.getMessage());
    }

    @Test
    void testDeleteEmployee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setUsername("marko123");
        employee.setPosition("Manager");
        employee.setDepartment("Finance");
        employee.setActive(true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Pozivamo metodu za brisanje
        employeeService.deleteEmployee(1L);

        // Proveravamo da li je delete metoda pozvana
        verify(employeeRepository, times(1)).delete(employee);
    }

    @Test
    void testDeleteEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.deleteEmployee(99L));

        assertEquals("Employee not found", exception.getMessage());
        verify(employeeRepository, never()).delete(any()); // Proveravamo da delete nije pozvan
    }

    @Test
    void testDeactivateEmployee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setUsername("marko123");
        employee.setPosition("Manager");
        employee.setDepartment("Finance");
        employee.setActive(true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.deactivateEmployee(1L);

        assertFalse(employee.isActive());
        verify(employeeRepository, times(1)).save(employee); // Proveravamo da je save pozvan
    }

    @Test
    void testDeactivateEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.deactivateEmployee(99L));

        assertEquals("Employee not found", exception.getMessage());
        verify(employeeRepository, never()).save(any()); // Proveravamo da save nije pozvan
    }

    @Test
    void testActivateEmployee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setUsername("marko123");
        employee.setPosition("Manager");
        employee.setDepartment("Finance");
        employee.setActive(false);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.activateEmployee(1L);

        assertTrue(employee.isActive());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void testActivateEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.activateEmployee(99L));

        assertEquals("Employee not found", exception.getMessage());
        verify(employeeRepository, never()).save(any());
    }
}
