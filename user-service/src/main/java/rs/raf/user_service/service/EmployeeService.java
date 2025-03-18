package rs.raf.user_service.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.user_service.domain.dto.CreateEmployeeDto;
import rs.raf.user_service.domain.dto.EmailRequestDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.dto.UpdateEmployeeDto;
import rs.raf.user_service.domain.entity.ActuaryLimit;
import rs.raf.user_service.domain.entity.AuthToken;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.exceptions.*;
import rs.raf.user_service.domain.mapper.EmployeeMapper;
import rs.raf.user_service.repository.*;
import rs.raf.user_service.specification.EmployeeSearchSpecification;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RoleRepository roleRepository;
    private final ActuaryLimitRepository actuaryLimitRepository;


    @Operation(summary = "Find all employees", description = "Fetches employees with optional filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee list retrieved successfully")
    })

    public Page<EmployeeDto> findAll(String firstName, String lastName, String email, String position, Pageable pageable) {
        Specification<Employee> spec = Specification.where(EmployeeSearchSpecification.startsWithFirstName(firstName))
                .and(EmployeeSearchSpecification.startsWithLastName(lastName))
                .and(EmployeeSearchSpecification.startsWithEmail(email))
                .and(EmployeeSearchSpecification.startsWithPosition(position));

        return employeeRepository.findAll(spec, pageable)
                .map(EmployeeMapper::toDto);

    }

    @Operation(summary = "Find employee by ID", description = "Fetches an employee by its unique ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })

    public EmployeeDto findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(EntityNotFoundException::new);
        return EmployeeMapper.toDto(employee);
    }

    @Operation(summary = "Delete an employee", description = "Deletes an employee by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        employeeRepository.delete(employee);
    }

    @Operation(summary = "Deactivate an employee", description = "Deactivates an employee by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    @Operation(summary = "Activate an employee", description = "Activates an employee by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee activated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public void activateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        employee.setActive(true);
        employeeRepository.save(employee);
    }

    @Operation(summary = "Create an employee", description = "Creates an employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee created successfully"),
            @ApiResponse(responseCode = "400", description = "Employee username or email already exists")
    })
    public EmployeeDto createEmployee(CreateEmployeeDto createEmployeeDTO) throws EmailAlreadyExistsException {
        if (userRepository.existsByEmail(createEmployeeDTO.getEmail()))
            throw new EmailAlreadyExistsException();
        if (userRepository.existsByUsername(createEmployeeDTO.getUsername()))
            throw new UserAlreadyExistsException();
        if (userRepository.findByJmbg(createEmployeeDTO.getJmbg()).isPresent())
            throw new JmbgAlreadyExistsException();

        Role role = roleRepository.findByName(createEmployeeDTO.getRole()).orElseThrow(RoleNotFoundException::new);

        Employee employee = EmployeeMapper.createDtoToEntity(createEmployeeDTO);
        employee.setRole(role);
        employeeRepository.save(employee);

        UUID token = UUID.fromString(UUID.randomUUID().toString());
        EmailRequestDto emailRequestDto = new EmailRequestDto(token.toString(), employee.getEmail());


        rabbitTemplate.convertAndSend("set-password", emailRequestDto);


        Long createdAt = Instant.now().toEpochMilli();
        Long expiresAt = createdAt + 86400000;//24h
        AuthToken authToken = new AuthToken(createdAt, expiresAt, token.toString(), "set-password", employee.getId());
        authTokenRepository.save(authToken);

        return EmployeeMapper.toDto(employee);
    }

    @Operation(summary = "Update an employee", description = "Updates an employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public EmployeeDto updateEmployee(Long id, UpdateEmployeeDto updateEmployeeDTO) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        Role role = roleRepository.findByName(updateEmployeeDTO.getRole()).orElseThrow(RoleNotFoundException::new);

        employee.setLastName(updateEmployeeDTO.getLastName());
        employee.setGender(updateEmployeeDTO.getGender());
        employee.setPhone(updateEmployeeDTO.getPhone());
        employee.setAddress(updateEmployeeDTO.getAddress());
        employee.setPosition(updateEmployeeDTO.getPosition());
        employee.setDepartment(updateEmployeeDTO.getDepartment());

        if (role.getName().equals("AGENT") && !Objects.equals(employee.getRole().getName(), "AGENT")) {
            ActuaryLimit actuaryLimit = new ActuaryLimit(new BigDecimal(100000), new BigDecimal(0), true, employee);
            actuaryLimitRepository.save(actuaryLimit);
        }
        if (!role.getName().equals("AGENT") && employee.getRole().getName().equals("AGENT")) {
            ActuaryLimit actuaryLimit = actuaryLimitRepository.findByEmployeeId(id).orElseThrow(RuntimeException::new);
            actuaryLimitRepository.delete(actuaryLimit);
        }

        employee.setRole(role);
        employee = employeeRepository.save(employee);

        return EmployeeMapper.toDto(employee);
    }

    public EmployeeDto findByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with email: " + email));
        return EmployeeMapper.toDto(employee);
    }



}
