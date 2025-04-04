package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.user_service.domain.dto.CreateEmployeeDto;
import rs.raf.user_service.domain.dto.EmailRequestDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.dto.UpdateEmployeeDto;
import rs.raf.user_service.domain.entity.ActuaryLimit;
import rs.raf.user_service.domain.entity.AuthToken;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.exceptions.EmailAlreadyExistsException;
import rs.raf.user_service.exceptions.JmbgAlreadyExistsException;
import rs.raf.user_service.exceptions.RoleNotFoundException;
import rs.raf.user_service.exceptions.UserAlreadyExistsException;
import rs.raf.user_service.repository.*;
import rs.raf.user_service.service.EmployeeService;

import javax.persistence.EntityNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ActuaryLimitRepository actuaryLimitRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Dodajemo default mock za rolu "EMPLOYEE", da se kod ne skrši u testovima
        // gde očekujemo da stignemo do email/username/jmbg provera, umesto da ranije
        // baci RoleNotFoundException.
        Role defaultEmployeeRole = new Role(2L, "EMPLOYEE", new HashSet<>());
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(defaultEmployeeRole));
    }

    // --------------------------------------------------------------------------------
    // findById(...)
    // --------------------------------------------------------------------------------
    @Test
    void testFindById() {
        Employee employee = new Employee();
        employee.setUsername("marko123");
        employee.setPosition("Manager");
        employee.setDepartment("Finance");
        employee.setActive(true);
        employee.setRole(new Role(1L, "EMPLOYEE", new HashSet<>()));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeDto result = employeeService.findById(1L);

        assertNotNull(result);
        assertEquals("marko123", result.getUsername());
        assertEquals("Manager", result.getPosition());
    }

    @Test
    void testFindByIdNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.findById(99L));

        // Metod findById baca EntityNotFoundException => ovde hvatamo RuntimeException
        // i proveravamo da je to upravo tip EntityNotFoundException
        assertEquals(EntityNotFoundException.class, exception.getClass());
    }

    // --------------------------------------------------------------------------------
    // findAll(...)
    // --------------------------------------------------------------------------------
    @Test
    void testFindAllWithPaginationAndFilters() {
        Employee employee = new Employee();
        employee.setUsername("jovan456");
        employee.setFirstName("Jovan");
        employee.setLastName("Jovanovic");
        employee.setEmail("jovan456@example.com");
        employee.setPosition("Developer");
        employee.setDepartment("IT");
        employee.setActive(true);
        employee.setRole(new Role(1L, "EMPLOYEE", new HashSet<>()));

        Page<Employee> page = new PageImpl<>(Collections.singletonList(employee));

        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<EmployeeDto> result = employeeService.findAll("Jovan", "Jovanovic", "jovan456@example.com", "Developer", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Developer", result.getContent().get(0).getPosition());
        assertEquals("IT", result.getContent().get(0).getDepartment());
        assertTrue(result.getContent().get(0).isActive());
    }

    @Test
    void testFindAllWithoutFilters() {
        Role role = new Role(1L, "EMPLOYEE", new HashSet<>());

        Employee emp1 = new Employee();
        emp1.setUsername("ana789");
        emp1.setPosition("HR");
        emp1.setDepartment("Human Resources");
        emp1.setActive(true);
        emp1.setRole(role);

        Employee emp2 = new Employee();
        emp2.setUsername("ivan321");
        emp2.setPosition("Designer");
        emp2.setDepartment("Creative");
        emp2.setActive(false);
        emp2.setRole(role);

        Page<Employee> page = new PageImpl<>(List.of(emp1, emp2));

        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<EmployeeDto> result = employeeService.findAll(null, null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    // --------------------------------------------------------------------------------
    // deleteEmployee(...)
    // --------------------------------------------------------------------------------
    @Test
    void testDeleteEmployee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setUsername("marko123");
        employee.setPosition("Manager");
        employee.setDepartment("Finance");
        employee.setActive(true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.deleteEmployee(1L);

        verify(employeeRepository, times(1)).delete(employee);
    }

    @Test
    void testDeleteEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.deleteEmployee(99L));

        assertEquals("Employee not found", exception.getMessage());
        verify(employeeRepository, never()).delete(any());
    }

    // --------------------------------------------------------------------------------
    // deactivateEmployee(...)
    // --------------------------------------------------------------------------------
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
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void testDeactivateEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> employeeService.deactivateEmployee(99L));

        assertEquals("Employee not found", exception.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    // --------------------------------------------------------------------------------
    // activateEmployee(...)
    // --------------------------------------------------------------------------------
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

    // --------------------------------------------------------------------------------
    // createEmployee(...)
    // --------------------------------------------------------------------------------
    @Test
    void testCreateEmployee_Success() {
        String firstName = "Petar";
        String lastName = "Petrovic";
        String gender = "M";
        String email = "petarw@raf.rs";
        String phone = "+38161123456";
        String address = "Trg Republike 5";
        String username = "petareperic90";
        String position = "Menadzer";
        String department = "Finansije";
        String jmbg = "1234567890123";
        String roleName = "EMPLOYEE";
        Boolean active = true;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(1990, 1, 20, 0, 0, 0);
        Date birthDate = calendar.getTime();

        CreateEmployeeDto dto = new CreateEmployeeDto(firstName, lastName, birthDate, gender, email, active, phone, address,
                username, position, department, jmbg, roleName);

        // Već smo mockovali roleRepository.findByName("EMPLOYEE") => Optional.of(...),
        // tako da ovde prođe.
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.findByJmbg(jmbg)).thenReturn(Optional.empty());

        EmployeeDto result = employeeService.createEmployee(dto);

        assertNotNull(result);
        assertEquals("Petar", result.getFirstName());
        assertEquals("Petrovic", result.getLastName());
        verify(employeeRepository, times(1)).save(any(Employee.class));
        // Proveravamo da li je token kreiran i poslat Rabbit-u
        verify(authTokenRepository, times(1)).save(any(AuthToken.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("set-password"), any(EmailRequestDto.class));
    }

    @Test
    void testCreateEmployee_EmailExists() {
        CreateEmployeeDto dto = new CreateEmployeeDto();
        dto.setEmail("existing@test.com");
        dto.setUsername("uniqueUsername");
        dto.setJmbg("1234567890123");
        dto.setRole("EMPLOYEE");

        // Da dođe do if-a za email => setujemo true
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        // Ostalo false/empty
        when(userRepository.existsByUsername("uniqueUsername")).thenReturn(false);
        when(userRepository.findByJmbg("1234567890123")).thenReturn(Optional.empty());

        assertThrows(EmailAlreadyExistsException.class, () -> employeeService.createEmployee(dto));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_UsernameExists() {
        CreateEmployeeDto dto = new CreateEmployeeDto();
        dto.setEmail("unique@test.com");
        dto.setUsername("alreadyUsedUsername");
        dto.setJmbg("1234567890123");
        dto.setRole("EMPLOYEE");

        when(userRepository.existsByEmail("unique@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("alreadyUsedUsername")).thenReturn(true);
        when(userRepository.findByJmbg("1234567890123")).thenReturn(Optional.empty());

        assertThrows(UserAlreadyExistsException.class, () -> employeeService.createEmployee(dto));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_JmbgExists() {
        CreateEmployeeDto dto = new CreateEmployeeDto();
        dto.setEmail("unique@test.com");
        dto.setUsername("uniqueUsername");
        dto.setJmbg("1234567890123");
        dto.setRole("EMPLOYEE");

        when(userRepository.existsByEmail("unique@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("uniqueUsername")).thenReturn(false);
        when(userRepository.findByJmbg("1234567890123")).thenReturn(Optional.of(new Employee()));

        assertThrows(JmbgAlreadyExistsException.class, () -> employeeService.createEmployee(dto));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployee_RoleNotFound() {
        CreateEmployeeDto dto = new CreateEmployeeDto();
        dto.setEmail("role@test.com");
        dto.setUsername("roleUsername");
        dto.setJmbg("1234567890123");
        dto.setRole("NON_EXISTENT_ROLE");

        when(userRepository.existsByEmail("role@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("roleUsername")).thenReturn(false);
        when(userRepository.findByJmbg("1234567890123")).thenReturn(Optional.empty());

        // Ovom stubu rušimo rolu
        when(roleRepository.findByName("NON_EXISTENT_ROLE")).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> employeeService.createEmployee(dto));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // --------------------------------------------------------------------------------
    // updateEmployee(...)
    // --------------------------------------------------------------------------------
    @Test
    void testUpdateEmployee() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(1990, 1, 20, 0, 0, 0);

        Employee employee = new Employee("Petar", "Petrovic", calendar.getTime(), "M",
                "petar@raf.rs", "+38161123456", "Trg Republike 5", "petareperic90",
                "Menadzer", "Finansije", true, "1234567890123",
                new Role(1L, "EMPLOYEE", new HashSet<>())
        );

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        // Već smo mockovali "EMPLOYEE" default, ali ovde dodatno
        when(roleRepository.findByName("EMPLOYEE"))
                .thenReturn(Optional.of(new Role(1L, "EMPLOYEE", new HashSet<>())));

        String lastName = "Peric";
        String gender = "F";
        String phone = "+38161123457";
        String address = "Trg Republike 6";
        String position = "Programer";
        String department = "Programiranje";
        String role = "EMPLOYEE";

        employeeService.updateEmployee(1L, new UpdateEmployeeDto(lastName, gender, phone, address, position, department, role));

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
                99L, new UpdateEmployeeDto("Peric", "F", "+38161123457",
                        "Trg Republike 6", "Programer", "Programiranje", "EMPLOYEE")
        ));

        assertEquals("Employee not found", exception.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void testUpdateEmployee_RoleNotFound() {
        Employee existingEmployee = new Employee();
        existingEmployee.setId(1L);
        existingEmployee.setRole(new Role(1L, "EMPLOYEE", new HashSet<>()));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existingEmployee));
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        UpdateEmployeeDto dto = new UpdateEmployeeDto("TestLast", "M", "123", "Address", "Pos", "Dept", "UNKNOWN");

        assertThrows(RoleNotFoundException.class, () -> employeeService.updateEmployee(1L, dto));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testUpdateEmployee_ChangeToAgent() {
        // Prethodno employee nije agent -> role = EMPLOYEE
        Role oldRole = new Role(1L, "EMPLOYEE", new HashSet<>());
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setRole(oldRole);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Role newRole = new Role(2L, "AGENT", new HashSet<>());
        when(roleRepository.findByName("AGENT")).thenReturn(Optional.of(newRole));

        UpdateEmployeeDto updateDto = new UpdateEmployeeDto("LastName", "F", "12345", "Address", "Position", "Dept", "AGENT");

        when(actuaryLimitRepository.save(any(ActuaryLimit.class))).thenAnswer(inv -> inv.getArgument(0));

        employeeService.updateEmployee(1L, updateDto);

        verify(actuaryLimitRepository, times(1)).save(any(ActuaryLimit.class));
        assertEquals("AGENT", employee.getRole().getName());
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    void testUpdateEmployee_ChangeFromAgent() {
        // Prethodno employee jeste AGENT
        Role oldRole = new Role(2L, "AGENT", new HashSet<>());
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setRole(oldRole);

        ActuaryLimit actuaryLimit = new ActuaryLimit();
        actuaryLimit.setEmployee(employee);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(new Role(1L, "EMPLOYEE", new HashSet<>())));
        when(actuaryLimitRepository.findByEmployeeId(1L)).thenReturn(Optional.of(actuaryLimit));

        UpdateEmployeeDto dto = new UpdateEmployeeDto("SomeLast", "M", "phone", "addr", "pos", "dept", "EMPLOYEE");
        employeeService.updateEmployee(1L, dto);

        verify(actuaryLimitRepository, times(1)).delete(actuaryLimit);
        assertEquals("EMPLOYEE", employee.getRole().getName());
        verify(employeeRepository, times(1)).save(employee);
    }

    // --------------------------------------------------------------------------------
    // findByEmail(...)
    // --------------------------------------------------------------------------------
    @Test
    void testFindByEmail_Success() {
        String email = "emp@example.com";
        Employee employee = new Employee();
        employee.setEmail(email);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        EmployeeDto dto = employeeService.findByEmail(email);

        assertNotNull(dto);
        assertEquals(email, dto.getEmail());
        verify(employeeRepository, times(1)).findByEmail(email);
    }

    @Test
    void testFindByEmail_NotFound() {
        String email = "notfound@example.com";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());

        Exception ex = assertThrows(EntityNotFoundException.class, () -> employeeService.findByEmail(email));
        assertEquals("Employee not found with email: notfound@example.com", ex.getMessage());
        verify(employeeRepository, times(1)).findByEmail(email);
    }
}
