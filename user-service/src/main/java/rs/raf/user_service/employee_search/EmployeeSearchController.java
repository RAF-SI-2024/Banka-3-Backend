package rs.raf.user_service.employee_search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.entity.Employee;

@RestController
@RequestMapping("/api/admin/employees")
public class EmployeeSearchController {

    private final EmployeeSearchService employeeService;

    public EmployeeSearchController(EmployeeSearchService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable Long id) {
        return employeeService.findById(id);
    }

    @GetMapping
    public Page<Employee> getAllEmployees(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return employeeService.findAll(position, department, active, pageable);
    }
}
