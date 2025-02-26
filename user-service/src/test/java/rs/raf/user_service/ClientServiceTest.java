package rs.raf.user_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.raf.user_service.dto.ClientDTO;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.mapper.ClientMapper;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.service.ClientService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testListClients() {
        Client client = new Client();
        client.setId(1L);
        Page<Client> page = new PageImpl<>(List.of(client));
        when(clientRepository.findAll(any(PageRequest.class))).thenReturn(page);

        when(clientMapper.toDto(any(Client.class))).thenReturn(new ClientDTO());
        List<ClientDTO> clients = clientService.listClients(0, 5);

        assertNotNull(clients);
        assertEquals(1, clients.size());
    }

    @Test
    public void testGetClientById_Success() {
        Client client = new Client();
        client.setId(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(new ClientDTO());

        ClientDTO clientDTO = clientService.getClientById(1L);
        assertNotNull(clientDTO);
    }

    @Test
    public void testGetClientById_NotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> clientService.getClientById(1L));
    }

    @Test
    public void testAddClient_Success() throws ParseException {
        //  Kreiramo potpun ClientDTO objekat
        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setFirstName("Mihailo");
        clientDTO.setLastName("Petrović");
        clientDTO.setUsername("userMihailo");
        clientDTO.setEmail("mihailo@example.com");
        clientDTO.setPassword("Password12"); //  Validna lozinka (8-32 karaktera, 2 broja, veliko i malo slovo)
        clientDTO.setAddress("Bulevar Kralja Aleksandra 73");
        clientDTO.setPhone("0612345678");
        clientDTO.setGender("M");
        clientDTO.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse("1995-08-20"));

// Kreiramo Client entitet koji će se vratiti iz mappera i repozitorijuma
        Client client = new Client();
        client.setId(1L);
        client.setUsername(clientDTO.getUsername());
        client.setFirstName(clientDTO.getFirstName());
        client.setLastName(clientDTO.getLastName());
        client.setEmail(clientDTO.getEmail());
        client.setPassword(clientDTO.getPassword());
        client.setAddress(clientDTO.getAddress());
        client.setPhone(clientDTO.getPhone());
        client.setGender(clientDTO.getGender());
        client.setBirthDate(clientDTO.getBirthDate());

        // Mock-ovanje mapiranja i čuvanja
        when(clientMapper.toEntity(clientDTO)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toDto(client)).thenReturn(clientDTO);

        // Poziv metode za testiranje
        ClientDTO result = clientService.addClient(clientDTO);

        // Ispis za proveru
        System.out.println("[Test] Rezultat metode addClient: " + result);

        // Provere rezultata
        assertNotNull(result);
        assertEquals(clientDTO.getFirstName(), result.getFirstName());
        assertEquals(clientDTO.getLastName(), result.getLastName());
        assertEquals(clientDTO.getEmail(), result.getEmail());
        assertEquals(clientDTO.getAddress(), result.getAddress());
    }

    @Test
    public void testUpdateClient_Success() {
        Client client = new Client();
        client.setId(1L);
        ClientDTO clientDTO = new ClientDTO();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toDto(client)).thenReturn(clientDTO);

        ClientDTO updatedClient = clientService.updateClient(1L, clientDTO);
        assertNotNull(updatedClient);
    }

    @Test
    public void testDeleteClient_Success() {
        when(clientRepository.existsById(1L)).thenReturn(true);
        doNothing().when(clientRepository).deleteById(1L);

        assertDoesNotThrow(() -> clientService.deleteClient(1L));
    }

    @Test
    public void testDeleteClient_NotFound() {
        when(clientRepository.existsById(1L)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> clientService.deleteClient(1L));
    }

    @Test
    public void testAddClient_InvalidPassword() {
        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setFirstName("Petar");
        clientDTO.setLastName("Perić");
        clientDTO.setEmail("petar@example.com");
        clientDTO.setPassword("pass"); // Nevalidna lozinka

        assertThrows(IllegalArgumentException.class, () -> clientService.addClient(clientDTO));
    }

    @Test
    public void testUpdateClient_InvalidPassword() {
        Client existingClient = new Client();
        existingClient.setId(1L);
        ClientDTO updateDTO = new ClientDTO();
        updateDTO.setPassword("weak"); //  Nevalidna lozinka

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));

        assertThrows(IllegalArgumentException.class, () -> clientService.updateClient(1L, updateDTO));
    }

    @Test
    public void testListClients_Empty() {
        Page<Client> emptyPage = new PageImpl<>(Collections.emptyList());
        when(clientRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        List<ClientDTO> clients = clientService.listClients(0, 5);
        assertNotNull(clients);
        assertTrue(clients.isEmpty());
    }
}
