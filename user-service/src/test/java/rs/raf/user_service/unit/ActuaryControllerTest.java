package rs.raf.user_service.unit;

import io.cucumber.core.internal.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.user_service.controller.ActuaryController;
import rs.raf.user_service.controller.EmployeeController;
import rs.raf.user_service.domain.dto.ActuaryDto;
import rs.raf.user_service.domain.dto.ChangeAgentLimitDto;
import rs.raf.user_service.domain.dto.EmployeeDto;
import rs.raf.user_service.domain.dto.SetApprovalDto;
import rs.raf.user_service.exceptions.ActuaryLimitNotFoundException;
import rs.raf.user_service.exceptions.EmployeeNotFoundException;
import rs.raf.user_service.exceptions.UserNotAgentException;
import rs.raf.user_service.service.ActuaryService;
import java.math.BigDecimal;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.http.RequestEntity.put;

public class ActuaryControllerTest {

    @Mock
    private ActuaryService actuaryService;

    @InjectMocks
    private ActuaryController actuaryController;

    @InjectMocks
    private EmployeeController employeeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void changeAgentLimit_Success() {
        Long id = 1L;
        ChangeAgentLimitDto dto = new ChangeAgentLimitDto(new BigDecimal("5000"));

        ResponseEntity<?> response = actuaryController.changeAgentLimit(id, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(actuaryService, times(1)).changeAgentLimit(id, dto.getNewLimit());
    }

    @Test
    void changeAgentLimit_Failure() {
        Long id = 1L;
        ChangeAgentLimitDto dto = new ChangeAgentLimitDto(new BigDecimal("5000"));

        doThrow(new ActuaryLimitNotFoundException(1L)).when(actuaryService).changeAgentLimit(id, dto.getNewLimit());

        ResponseEntity<?> response = actuaryController.changeAgentLimit(id, dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Actuary limit with employeeId: "+id+ " is not found.", response.getBody());
    }

    @Test
    void resetDailyLimit_Success() {
        Long id = 1L;

        ResponseEntity<?> response = actuaryController.resetDailyLimit(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(actuaryService, times(1)).resetDailyLimit(id);
    }

    @Test
    void resetDailyLimit_Failure() {
        Long id = 1L;
        doThrow(new EmployeeNotFoundException(1L)).when(actuaryService).resetDailyLimit(id);

        ResponseEntity<?> response = actuaryController.resetDailyLimit(id);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot find employee with id: "+id, response.getBody());
    }

    @Test
    void getAllAgents_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<EmployeeDto> page = new PageImpl<>(Collections.emptyList());

        when(actuaryService.findAgents(null, null, null, null, pageable)).thenReturn(page);

        ResponseEntity<Page<EmployeeDto>> response = actuaryController.getAllAgents(null, null, null, null, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalElements());
    }

    @Test
    void setApprovalValue_Success() {
        Long id = 1L;
        SetApprovalDto setApprovalDto = new SetApprovalDto(true); // true znači da je potrebno odobrenje

        doNothing().when(actuaryService).setApproval(id, setApprovalDto.getNeedApproval());

        ResponseEntity<?> response = actuaryController.setApprovalValue(id, setApprovalDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(actuaryService, times(1)).setApproval(id, setApprovalDto.getNeedApproval());
    }

    @Test
    void setApprovalValue_ActuaryLimitNotFoundException() {
        Long id = 1L;
        SetApprovalDto setApprovalDto = new SetApprovalDto(true);

        doThrow(new ActuaryLimitNotFoundException(id)).when(actuaryService).setApproval(id, setApprovalDto.getNeedApproval());

        ResponseEntity<?> response = actuaryController.setApprovalValue(id, setApprovalDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Actuary limit with employeeId: " + id + " is not found.", response.getBody());
    }

    @Test
    void setApprovalValue_EmployeeNotFoundException() {
        Long id = 1L;
        SetApprovalDto setApprovalDto = new SetApprovalDto(true);

        doThrow(new EmployeeNotFoundException(id)).when(actuaryService).setApproval(id, setApprovalDto.getNeedApproval());

        ResponseEntity<?> response = actuaryController.setApprovalValue(id, setApprovalDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot find employee with id: " + id, response.getBody());
    }

    @Test
    void setApprovalValue_UserNotAgentException() {
        Long id = 1L;
        SetApprovalDto setApprovalDto = new SetApprovalDto(true);

        doThrow(new UserNotAgentException(id)).when(actuaryService).setApproval(id, setApprovalDto.getNeedApproval());

        ResponseEntity<?> response = actuaryController.setApprovalValue(id, setApprovalDto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User with id: " + id + " is not an agent.", response.getBody());
    }

}
