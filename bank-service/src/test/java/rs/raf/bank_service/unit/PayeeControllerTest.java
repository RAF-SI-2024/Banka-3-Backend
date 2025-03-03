package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import rs.raf.bank_service.controller.PayeeController;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.exceptions.PayeesNotFoundByClientIdException;
import rs.raf.bank_service.service.PayeeService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayeeController.class)
@AutoConfigureMockMvc
public class PayeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayeeService payeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private PayeeDto testPayeeDto;
    private long clientId;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        clientId = 1L;  // Dodeljujem neki clientId za testove

        testPayeeDto = new PayeeDto();
        testPayeeDto.setId(1L);
        testPayeeDto.setName("Test Firma");
        testPayeeDto.setAccountNumber("123456789");
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testCreatePayee_Success() throws Exception {
        when(payeeService.create(any(PayeeDto.class))).thenReturn(testPayeeDto);

        mockMvc.perform(post("/api/payees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPayeeDto)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Payee created successfully."));

        verify(payeeService, times(1)).create(any(PayeeDto.class));
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testCreatePayee_InvalidData() throws Exception {
        PayeeDto invalidPayee = new PayeeDto();
        invalidPayee.setName("");  // Nevalidno ime
        invalidPayee.setAccountNumber("");  // Nevalidan broj računa

        mockMvc.perform(post("/api/payees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(invalidPayee)))
                .andExpect(status().isBadRequest());  // Očekujemo status 400
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testUpdatePayee_Success() throws Exception {
        when(payeeService.update(anyLong(), any(PayeeDto.class))).thenReturn(testPayeeDto);

        mockMvc.perform(put("/api/payees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPayeeDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Payee updated successfully."));

        verify(payeeService, times(1)).update(anyLong(), any(PayeeDto.class));
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testUpdatePayee_NotFound() throws Exception {
        doThrow(new PayeeNotFoundException(999L)).when(payeeService).update(anyLong(), any(PayeeDto.class));

        mockMvc.perform(put("/api/payees/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPayeeDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Cannot find payee with id: 999"));
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testDeletePayee_Success() throws Exception {
        doNothing().when(payeeService).delete(anyLong());

        mockMvc.perform(delete("/api/payees/1"))
                .andExpect(status().isNoContent());

        verify(payeeService, times(1)).delete(anyLong());
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testDeletePayee_NotFound() throws Exception {
        doThrow(new PayeeNotFoundException(999L)).when(payeeService).delete(anyLong());

        mockMvc.perform(delete("/api/payees/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testGetPayeesByClientId_Success() throws Exception {
        List<PayeeDto> payees = Arrays.asList(testPayeeDto);
        when(payeeService.getByClientId(clientId)).thenReturn(payees);

        mockMvc.perform(get("/api/payees/client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(payees)));

        verify(payeeService, times(1)).getByClientId(clientId);
    }

    @Test
    @WithMockUser(authorities = "client")
    public void testGetPayeesByClientId_NotFound() throws Exception {
        Long clientId = 1L;  // Postavi konkretan clientId za test

        // Simuliraj da servis baca PayeesNotFoundByClientIdException
        when(payeeService.getByClientId(clientId)).thenThrow(new PayeesNotFoundByClientIdException(clientId));

        // MockMvc poziv
        mockMvc.perform(get("/api/payees/client/" + clientId))
                .andExpect(status().isNotFound())  // Provera da je status 404
                .andExpect(content().string("Cannot find payee/s with client ID: " + clientId));  // Provera poruke

        // Verifikacija poziva servisa
        verify(payeeService, times(1)).getByClientId(clientId);
    }

    private String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
