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
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.employee_search.EmployeeSearchRepository;
import rs.raf.user_service.employee_search.EmployeeSearchService;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeSearchRepository employeeRepository;

    @InjectMocks
    private EmployeeSearchService employeeService;

    @Test
    void testFindById() {

        Employee employee = new Employee();
        employee.setUsername("marko123");
        employee.setPosition("Manager");
        employee.setDepartment("Finance");
        employee.setActive(true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Employee result = employeeService.findById(1L);

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

        //when(employeeRepository.findAll(any(), any())).thenReturn(page);
        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);


        Page<Employee> result = employeeService.findAll("Developer", "IT", true, PageRequest.of(0, 10));


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

        Page<Employee> page = new PageImpl<>(java.util.List.of(emp1, emp2));

        //when(employeeRepository.findAll(any(), any())).thenReturn(page);
        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Employee> result = employeeService.findAll(null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void testFindByIdNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.findById(99L));

        assertEquals("Employee not found", exception.getMessage());
    }
}
