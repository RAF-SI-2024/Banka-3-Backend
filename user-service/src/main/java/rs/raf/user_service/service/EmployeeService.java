package rs.raf.user_service.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.CreateEmployeeDto;
import rs.raf.user_service.dto.EmailRequestDto;
import rs.raf.user_service.dto.EmployeeDto;
import rs.raf.user_service.dto.UpdateEmployeeDto;
import rs.raf.user_service.entity.AuthToken;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.exceptions.EmailAlreadyExistsException;
import rs.raf.user_service.exceptions.JmbgAlreadyExistsException;
import rs.raf.user_service.exceptions.UserAlreadyExistsException;
import rs.raf.user_service.mapper.EmployeeMapper;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.specification.EmployeeSearchSpecification;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.UUID;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, RabbitTemplate rabbitTemplate, AuthTokenRepository authTokenRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        if (employeeRepository.existsByEmail(createEmployeeDTO.getEmail()))
            throw new EmailAlreadyExistsException();
        if (employeeRepository.existsByUsername(createEmployeeDTO.getUsername()))
            throw new UserAlreadyExistsException();
        if (employeeRepository.findByJmbg(createEmployeeDTO.getJmbg()).isPresent())
            throw new JmbgAlreadyExistsException();


        Employee employee = EmployeeMapper.createDtoToEntity(createEmployeeDTO);
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

        employee.setLastName(updateEmployeeDTO.getLastName());
        employee.setGender(updateEmployeeDTO.getGender());
        employee.setPhone(updateEmployeeDTO.getPhone());
        employee.setAddress(updateEmployeeDTO.getAddress());
        employee.setPosition(updateEmployeeDTO.getPosition());
        employee.setDepartment(updateEmployeeDTO.getDepartment());

        employee = employeeRepository.save(employee);

        return EmployeeMapper.toDto(employee);
    }

}
