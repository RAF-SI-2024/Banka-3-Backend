package rs.raf.user_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.dto.CreateEmployeeDto;
import rs.raf.user_service.dto.UpdateEmployeeDto;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.service.EmployeeService;
import rs.raf.user_service.dto.EmployeeDto;
import rs.raf.user_service.entity.Employee;

import javax.persistence.EntityNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

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

        EmployeeDto result = employeeService.findById(1L);

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

        Page<EmployeeDto> result = employeeService.findAll("Developer", "IT", true, PageRequest.of(0, 10));

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

        Page<EmployeeDto> result = employeeService.findAll(null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void testFindByIdNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.findById(99L));

        assertEquals(EntityNotFoundException.class, exception.getClass());
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

    @Test

    void testCreateEmployee() {
        String firstName = "Petar";
        String lastName = "Petrovic";
        String gender = "M";
        String email = "petar@raf.rs";
        String phone = "+38161123456";
        String address = "Trg Republike 5";
        String username = "petareperic90";
        String position = "Menadzer";
        String department = "Finansije";
        Boolean active = true;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(1990, 1, 20, 0, 0, 0);
        Date birthDate = calendar.getTime();

        employeeService.createEmployee(new CreateEmployeeDto(firstName, lastName, birthDate, gender, email, active,phone, address,
                username, position, department)
        );

        verify(employeeRepository, times(1)).save(argThat(employee ->
                employee.getFirstName().equals(firstName) &&
                employee.getLastName().equals(lastName) &&
                employee.getBirthDate().equals(birthDate) &&
                employee.getGender().equals(gender) &&
                employee.getEmail().equals(email) &&
                employee.getPhone().equals(phone) &&
                employee.getAddress().equals(address) &&
                employee.getUsername().equals(username) &&
                employee.getPosition().equals(position) &&
                employee.getDepartment().equals(department)
        ));
    }

    @Test
    void testUpdateEmployee() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(1990, 1, 20, 0, 0, 0);

        Employee employee = new Employee("Petar", "Petrovic", calendar.getTime(), "M",
                "petar@raf.rs", "+38161123456","Trg Republike 5", "petareperic90",
                "Menadzer", "Finansije", true
        );

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        String lastName = "Peric";
        String gender = "F";
        String phone = "+38161123457";
        String address = "Trg Republike 6";
        String position = "Programer";
        String department = "Programiranje";

        employeeService.updateEmployee(1L , new UpdateEmployeeDto(lastName, gender, phone, address, position, department));

        assertAll("Employee fields should be updated correctly",
                () -> assertEquals(lastName, employee.getLastName()),
                () -> assertEquals(gender, employee.getGender()),
                () -> assertEquals(phone, employee.getPhone()),
                () -> assertEquals(address, employee.getAddress()),
                () -> assertEquals(position, employee.getPosition()),
                () -> assertEquals(department, employee.getDepartment())
        );
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void testUpdateEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(EntityNotFoundException.class, () -> employeeService.updateEmployee(
                99L , new UpdateEmployeeDto("Peric", "F", "+38161123457",
                        "Trg Republike 6", "Programer", "Programiranje")
                )
        );

        assertEquals("Employee not found", exception.getMessage());
        verify(employeeRepository, never()).save(any());
    }
}
