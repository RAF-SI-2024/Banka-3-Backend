package rs.raf.user_service;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.user_service.controller.ClientController;
import rs.raf.user_service.dto.ClientDTO;
import rs.raf.user_service.service.ClientService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClientControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private ClientDTO clientDTO;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
        this.objectMapper = new ObjectMapper();

        clientDTO = new ClientDTO(1L, "Marko", "Markovic", "marko@example.com", "userMarko", "Password12", "Adresa 1", "0611158275", "M", new Date());
    }

    @Test
    public void testGetAllClients() throws Exception {
        List<ClientDTO> clients = Arrays.asList(clientDTO);
        when(clientService.listClients(0, 10)).thenReturn(clients);

        mockMvc.perform(get("/api/admin/clients")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName", is(clientDTO.getFirstName())));

        verify(clientService, times(1)).listClients(0, 10);
    }

    @Test
    public void testGetClientById_Success() throws Exception {
        when(clientService.getClientById(1L)).thenReturn(clientDTO);

        mockMvc.perform(get("/api/admin/clients/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(clientDTO.getEmail())));

        verify(clientService, times(1)).getClientById(1L);
    }

    @Test
    public void testGetClientById_NotFound() throws Exception {
        when(clientService.getClientById(1L)).thenThrow(new NoSuchElementException("Client not found with ID: 1"));

        mockMvc.perform(get("/api/admin/clients/{id}", 1L))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).getClientById(1L);
    }

    @Test
    public void testAddClient_Success() throws Exception {
        when(clientService.addClient(any(ClientDTO.class))).thenReturn(clientDTO);

        mockMvc.perform(post("/api/admin/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is(clientDTO.getFirstName())));

        verify(clientService, times(1)).addClient(any(ClientDTO.class));
    }

    @Test
    public void testUpdateClient_Success() throws Exception {
        clientDTO.setFirstName("UpdatedName");
        when(clientService.updateClient(eq(1L), any(ClientDTO.class))).thenReturn(clientDTO);

        mockMvc.perform(put("/api/admin/clients/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("UpdatedName")));

        verify(clientService, times(1)).updateClient(eq(1L), any(ClientDTO.class));
    }

    @Test
    public void testDeleteClient_Success() throws Exception {
        doNothing().when(clientService).deleteClient(1L);

        mockMvc.perform(delete("/api/admin/clients/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(clientService, times(1)).deleteClient(1L);
    }

    @Test
    public void testDeleteClient_NotFound() throws Exception {
        doThrow(new NoSuchElementException("Client not found with ID: 1")).when(clientService).deleteClient(1L);

        mockMvc.perform(delete("/api/admin/clients/{id}", 1L))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).deleteClient(1L);
    }
}
