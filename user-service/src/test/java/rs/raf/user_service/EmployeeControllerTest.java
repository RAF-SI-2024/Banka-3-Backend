package rs.raf.user_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import rs.raf.user_service.controller.EmployeeController;
import rs.raf.user_service.dto.CreateEmployeeDTO;
import rs.raf.user_service.dto.UpdateEmployeeDTO;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.service.EmployeeService;

import java.util.Calendar;
import java.util.Date;
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
        doNothing().when(employeeService).createEmployee(any(CreateEmployeeDTO.class));

        // Mock a valid BindingResult (no errors)
        when(bindingResult.hasErrors()).thenReturn(false);

        // Call the controller method directly
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(1990, 1, 20, 0, 0, 0);

        ResponseEntity<Void> response = employeeController.createEmployee(new CreateEmployeeDTO("Petar",
                "Petrovic", calendar.getTime(),"M", "petar@raf.rs", "+38161123456",
                "Trg Republike 5", "petareperic90", "Menadzer", "Finansije"), bindingResult);

        // Verify that the service method was called and assert the response
        assertEquals(201, response.getStatusCodeValue());
        verify(employeeService, times(1)).createEmployee(any(CreateEmployeeDTO.class));
    }

    @Test
    void testCreateEmployeeInvalidInput() {
        // Mock the BindingResult to simulate validation errors
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldError("firstName")).thenReturn(new FieldError("createEmployeeDTO", "firstName",
                "First name cannot be null"));

        // Call the controller method directly
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(1990, 1, 20, 0, 0, 0);

        ResponseEntity<Void> response = employeeController.createEmployee(new CreateEmployeeDTO("",
                "Petrovic", calendar.getTime(),"M", "petar@raf.rs", "+38161123456",
                "Trg Republike 5", "petareperic90", "Menadzer", "Finansije"), bindingResult);

        // Verify that validation errors were detected
        assertEquals(400, response.getStatusCodeValue());
        verify(employeeService, never()).createEmployee(any());
    }

    @Test
    public void testUpdateEmployee() {
        when(bindingResult.hasErrors()).thenReturn(false);

        // Directly call the controller method and capture the response
        UpdateEmployeeDTO updateEmployeeDTO = new UpdateEmployeeDTO("Peric", "F", "+38161123457",
                "Trg Republike 6", "Programer", "Programiranje"
        );

        ResponseEntity<Void> response = employeeController.updateEmployee(1L , updateEmployeeDTO, bindingResult);

        // Verify the response
        assertEquals(200, response.getStatusCodeValue());  // 200 OK status
        verify(employeeService, times(1)).updateEmployee(1L, updateEmployeeDTO);
    }

    @Test
    public void testUpdateEmployeeInvalidParameter() {
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldError("firstName")).thenReturn(new FieldError("createEmployeeDTO", "firstName",
                "First name cannot be null"));

        // Directly call the controller method and capture the response
        UpdateEmployeeDTO updateEmployeeDTO = new UpdateEmployeeDTO("", "F", "+38161123457",
                "Trg Republike 6", "Programer", "Programiranje"
        );

        ResponseEntity<Void> response = employeeController.updateEmployee(1L , updateEmployeeDTO, bindingResult);

        // Verify the response
        assertEquals(400, response.getStatusCodeValue());  // 200 OK status
        verify(employeeService, never()).updateEmployee(1L, updateEmployeeDTO);
    }
}
