package rs.raf.user_service.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.CreateEmployeeDTO;
import rs.raf.user_service.dto.UpdateEmployeeDTO;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.dto.EmployeeDTO;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.specification.EmployeeSearchSpecification;

import javax.persistence.EntityNotFoundException;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Operation(summary = "Find all employees", description = "Fetches employees with optional filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee list retrieved successfully")
    })
    public Page<EmployeeDTO> findAll(String position, String department, Boolean active, Pageable pageable) {
        Specification<Employee> spec = Specification.where(EmployeeSearchSpecification.hasPosition(position))
                .and(EmployeeSearchSpecification.hasDepartment(department))
                .and(EmployeeSearchSpecification.isActive(active));

        return employeeRepository.findAll(spec, pageable)
                .map(this::mapToDTO);
    }

    @Operation(summary = "Find employee by ID", description = "Fetches an employee by its unique ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public EmployeeDTO findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return mapToDTO(employee);
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
    public void createEmployee(CreateEmployeeDTO createEmployeeDTO) {
        if (employeeRepository.existsByUsername(createEmployeeDTO.getUsername()) ||
                employeeRepository.existsByEmail(createEmployeeDTO.getEmail()))
            throw new IllegalArgumentException();

        employeeRepository.save(createEmployeeDTO.mapToEmployee());
    }

    @Operation(summary = "Update an employee", description = "Updates an employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public void updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        employee.setLastName(updateEmployeeDTO.getLastName());
        employee.setGender(updateEmployeeDTO.getGender());
        employee.setPhone(updateEmployeeDTO.getPhone());
        employee.setAddress(updateEmployeeDTO.getAddress());
        employee.setPosition(updateEmployeeDTO.getPosition());
        employee.setDepartment(updateEmployeeDTO.getDepartment());

        employeeRepository.save(employee);
    }

    private EmployeeDTO mapToDTO(Employee employee) {
        return new EmployeeDTO(
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getUsername(),
                employee.getPosition(),
                employee.getDepartment(),
                employee.isActive()
        );
    }
}
