package rs.raf.user_service.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.EmployeeDTO;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.specification.EmployeeSearchSpecification;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /*
    public Employee findById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    public Page<Employee> findAll(String position, String department, Boolean active, Pageable pageable) {
        Specification<Employee> spec = Specification.where(EmployeeSearchSpecification.hasPosition(position))
                .and(EmployeeSearchSpecification.hasDepartment(department))
                .and(EmployeeSearchSpecification.isActive(active));

        return employeeRepository.findAll(spec, pageable);
    }

     */

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
