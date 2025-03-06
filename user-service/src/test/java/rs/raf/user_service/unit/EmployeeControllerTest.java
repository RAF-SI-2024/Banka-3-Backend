package rs.raf.user_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import rs.raf.user_service.controller.EmployeeController;
import rs.raf.user_service.domain.dto.CreateEmployeeDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.dto.UpdateEmployeeDto;
import rs.raf.user_service.exceptions.EmailAlreadyExistsException;
import rs.raf.user_service.exceptions.JmbgAlreadyExistsException;
import rs.raf.user_service.exceptions.UserAlreadyExistsException;
import rs.raf.user_service.service.EmployeeService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeControllerTest {


    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private EmployeeDto employeeDto;
    private CreateEmployeeDto createEmployeeDto;
    private UpdateEmployeeDto updateEmployeeDto;
    @InjectMocks
    private EmployeeController employeeController;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();
        this.objectMapper = new ObjectMapper();

        Date birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("1990-05-15");

        employeeDto = new EmployeeDto(1l,"Marko01","Menadzer", "Finansije", true, "Marko", "Markovic", "marko@raf.rs", "1234567890123", birthDate, "M", "0611158275", "Adresa 1");

        createEmployeeDto = new CreateEmployeeDto();
        createEmployeeDto.setFirstName("Marko");
        createEmployeeDto.setLastName("Markovic");
        createEmployeeDto.setActive(true);
        createEmployeeDto.setDepartment("Finansije");
        createEmployeeDto.setPosition("Menadzer");
        createEmployeeDto.setEmail("marko@raf.rs");
        createEmployeeDto.setAddress("Adresa 1");
        createEmployeeDto.setPhone("0611158275");
        createEmployeeDto.setGender("M");
        createEmployeeDto.setBirthDate(birthDate);
        createEmployeeDto.setJmbg("1234567890123");
        createEmployeeDto.setUsername("marko12");
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
                "Trg Republike 5", "petareperic90", "Menadzer", "Finansije", "1234567890123"));

        // Verify that the service method was called and assert the response
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(mockEmployee.getFirstName(), ((EmployeeDto) response.getBody()).getFirstName());
        verify(employeeService, times(1)).createEmployee(any(CreateEmployeeDto.class));
    }

    @Test
    public void testAddEmployee_Success() throws Exception {
        when(employeeService.createEmployee(any(CreateEmployeeDto.class))).thenReturn(employeeDto);

        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is(employeeDto.getFirstName())))
                .andExpect(jsonPath("$.email", is(employeeDto.getEmail())));
        verify(employeeService, times(1)).createEmployee(any(CreateEmployeeDto.class));
    }

    @Test
    public void testAddClient_UserAlreadyExists() throws Exception {
        when(employeeService.createEmployee(any(CreateEmployeeDto.class))).thenThrow(new UserAlreadyExistsException());

        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeDto)))
                .andExpect(jsonPath("$.error").value("User with this username already exists"))
                .andExpect(status().isBadRequest());

        verify(employeeService, times(1)).createEmployee(any(CreateEmployeeDto.class));
    }

    @Test
    public void testAddClient_JmbgAlreadyExists() throws Exception {
        when(employeeService.createEmployee(any(CreateEmployeeDto.class))).thenThrow(new JmbgAlreadyExistsException());

        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeDto)))
                .andDo(print())
                .andExpect(jsonPath("$.error").value("User with this jmbg already exists"))
                .andExpect(status().isBadRequest());

        verify(employeeService, times(1)).createEmployee(any(CreateEmployeeDto.class));
    }

    @Test
    public void testAddClient_EmailAlreadyExists() throws Exception {
        when(employeeService.createEmployee(any(CreateEmployeeDto.class))).thenThrow(new EmailAlreadyExistsException());

        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeDto)))
                .andExpect(jsonPath("$.error").value("User with this email already exists"))
                .andExpect(status().isBadRequest());

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
