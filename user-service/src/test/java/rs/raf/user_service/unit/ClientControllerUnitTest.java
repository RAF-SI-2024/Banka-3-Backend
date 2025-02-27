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
import rs.raf.user_service.dto.ClientDto;
import rs.raf.user_service.dto.CreateClientDto;
import rs.raf.user_service.dto.UpdateClientDto;
import rs.raf.user_service.service.ClientService;

import java.text.SimpleDateFormat;
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

    private ClientDto clientDTO;
    private CreateClientDto createClientDTO;
    private UpdateClientDto updateClientDTO;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
        this.objectMapper = new ObjectMapper();

        Date birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("1990-05-15");

        clientDTO = new ClientDto(1L, "Marko", "Markovic", "marko@example.com", "", "Adresa 1", "0611158275", "M", birthDate);

        createClientDTO = new CreateClientDto();
        createClientDTO.setFirstName("Marko");
        createClientDTO.setLastName("Markovic");
        createClientDTO.setEmail("marko@example.com");
        createClientDTO.setAddress("Adresa 1");
        createClientDTO.setPhone("0611158275");
        createClientDTO.setGender("M");
        createClientDTO.setBirthDate(birthDate);

        updateClientDTO = new UpdateClientDto();
        updateClientDTO.setFirstName("MarkoUpdated");
        updateClientDTO.setLastName("MarkovicUpdated");
        updateClientDTO.setAddress("Nova Adresa");
        updateClientDTO.setPhone("0611159999");
        updateClientDTO.setGender("M");
        updateClientDTO.setBirthDate(birthDate);
    }

    @Test
    public void testGetAllClients() throws Exception {
        List<ClientDto> clients = Arrays.asList(clientDTO);
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
        when(clientService.addClient(any(CreateClientDto.class))).thenReturn(clientDTO);

        mockMvc.perform(post("/api/admin/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createClientDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is(clientDTO.getFirstName())))
                .andExpect(jsonPath("$.email", is(clientDTO.getEmail())))
                .andExpect(jsonPath("$.password", is(""))); // ✅ Lozinka prazna

        verify(clientService, times(1)).addClient(any(CreateClientDto.class));
    }

    @Test
    public void testUpdateClient_Success() throws Exception {
        clientDTO.setFirstName(updateClientDTO.getFirstName());
        clientDTO.setLastName(updateClientDTO.getLastName());
        clientDTO.setAddress(updateClientDTO.getAddress());
        clientDTO.setPhone(updateClientDTO.getPhone());

        when(clientService.updateClient(eq(1L), any(UpdateClientDto.class))).thenReturn(clientDTO);

        mockMvc.perform(put("/api/admin/clients/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateClientDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is(updateClientDTO.getFirstName())))
                .andExpect(jsonPath("$.lastName", is(updateClientDTO.getLastName())))
                .andExpect(jsonPath("$.address", is(updateClientDTO.getAddress())))
                .andExpect(jsonPath("$.phone", is(updateClientDTO.getPhone())));

        verify(clientService, times(1)).updateClient(eq(1L), any(UpdateClientDto.class));
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
