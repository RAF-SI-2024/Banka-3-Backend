package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import rs.raf.user_service.controller.EmployeeController;
import rs.raf.user_service.dto.CreateEmployeeDto;
import rs.raf.user_service.dto.EmployeeDto;
import rs.raf.user_service.dto.UpdateEmployeeDto;
import rs.raf.user_service.service.EmployeeService;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class EmployeeControllerTest {

    @InjectMocks
    private EmployeeController employeeController;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    public void setup() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateEmployee() {
        // Mock the service layer to simulate success
        EmployeeDto mockEmployee = new EmployeeDto(); // Simuliraj kreiranog zaposlenog
        mockEmployee.setFirstName("Petar");
        when(employeeService.createEmployee(any(CreateEmployeeDto.class))).thenReturn(mockEmployee);

        // Mock a valid BindingResult (no errors)
        when(bindingResult.hasErrors()).thenReturn(false);

        // Call the controller method directly
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(1990, 1, 20, 0, 0, 0);

        ResponseEntity<?> response = employeeController.createEmployee(new CreateEmployeeDto("Petar",
                "Petrovic", calendar.getTime(), "M", "petar@raf.rs", true, "+38161123456",
                "Trg Republike 5", "petareperic90", "Menadzer", "Finansije"));

        // Verify that the service method was called and assert the response
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(mockEmployee.getFirstName(), ((EmployeeDto) response.getBody()).getFirstName());
        verify(employeeService, times(1)).createEmployee(any(CreateEmployeeDto.class));
    }

//    @Test
//    void testCreateEmployeeInvalidInput() {
//        // Mock the BindingResult to simulate validation errors
//        when(bindingResult.hasErrors()).thenReturn(true);
//        when(bindingResult.getFieldError("firstName")).thenReturn(new FieldError("createEmployeeDTO", "firstName",
//                "First name cannot be null"));
//
//        // Call the controller method directly
//        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
//        calendar.set(1990, 1, 20, 0, 0, 0);
//
//        ResponseEntity<?> response = employeeController.createEmployee(new CreateEmployeeDto("",
//                "Petrovic", calendar.getTime(), "M", "petar@raf.rs", true, "+38161123456",
//                "Trg Republike 5", "petareperic90", "Menadzer", "Finansije"));
//
//        // Verify that validation errors were detected
//        assertEquals(400, response.getStatusCodeValue());
//        verify(employeeService, never()).createEmployee(any());
//    }

    @Test
    public void testUpdateEmployee() {
        // Directly call the controller method and capture the response
        UpdateEmployeeDto updateEmployeeDTO = new UpdateEmployeeDto("Peric", "F", "+38161123457",
                "Trg Republike 6", "Programer", "Programiranje"
        );

        ResponseEntity<?> response = employeeController.updateEmployee(1L, updateEmployeeDTO);

        // Verify the response
        assertEquals(200, response.getStatusCodeValue());  // 200 OK status
        verify(employeeService, times(1)).updateEmployee(1L, updateEmployeeDTO);
    }

//    @Test
//    public void testUpdateEmployeeInvalidParameter() {
//        when(bindingResult.hasErrors()).thenReturn(true);
//        when(bindingResult.getFieldError("firstName")).thenReturn(new FieldError("createEmployeeDTO", "firstName",
//                "First name cannot be null"));
//
//        // Directly call the controller method and capture the response
//        UpdateEmployeeDto updateEmployeeDTO = new UpdateEmployeeDto("", "F", "+38161123457",
//                "Trg Republike 6", "Programer", "Programiranje"
//        );
//
//        ResponseEntity<?> response = employeeController.updateEmployee(1L, updateEmployeeDTO);
//
//        // Verify the response
//        assertEquals(400, response.getStatusCodeValue());  // 200 OK status
//        verify(employeeService, never()).updateEmployee(1L, updateEmployeeDTO);
//    }
}
