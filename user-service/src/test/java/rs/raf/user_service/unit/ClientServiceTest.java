package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.raf.user_service.domain.dto.*;
import rs.raf.user_service.domain.entity.*;
import rs.raf.user_service.domain.mapper.ClientMapper;
import rs.raf.user_service.exceptions.EmailAlreadyExistsException;
import rs.raf.user_service.exceptions.JmbgAlreadyExistsException;
import rs.raf.user_service.exceptions.UserAlreadyExistsException;
import rs.raf.user_service.repository.*;
import rs.raf.user_service.service.ClientService;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ClientService clientService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Da bismo izbegli NoSuchElementException (jer u addClient(...) koristimo:
        // roleRepository.findByName("CLIENT").get())
        // Ovde defaultno mockujemo postojanje ROLE "CLIENT":
        Role clientRole = new Role();
        clientRole.setId(1L);
        clientRole.setName("CLIENT");
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
    }

    // --------------------------------------------------------------------------------
    // listClients(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testListClients() {
        Client client = new Client();
        client.setId(1L);
        Page<Client> page = new PageImpl<>(List.of(client));
        when(clientRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(clientMapper.toDto(any(Client.class))).thenReturn(new ClientDto());

        List<ClientDto> clients = clientService.listClients(PageRequest.of(0, 5)).getContent();
        assertNotNull(clients);
        assertEquals(1, clients.size());
        verify(clientRepository).findAll(any(Pageable.class));
    }

    @Test
    public void testListClients_Empty() {
        Page<Client> emptyPage = new PageImpl<>(Collections.emptyList());
        when(clientRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        List<ClientDto> clients = clientService.listClients(PageRequest.of(0, 5)).getContent();
        assertNotNull(clients);
        assertTrue(clients.isEmpty());
    }

    // --------------------------------------------------------------------------------
    // getClientById(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testGetClientById_Success() {
        Client client = new Client();
        client.setId(1L);
        ClientDto dto = new ClientDto();
        dto.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(dto);

        ClientDto clientDTO = clientService.getClientById(1L);
        assertNotNull(clientDTO);
        assertEquals(1L, clientDTO.getId());
        verify(clientRepository).findById(1L);
    }

    @Test
    public void testGetClientById_NotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> clientService.getClientById(1L));
    }

    // --------------------------------------------------------------------------------
    // addClient(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testAddClient_Success() throws ParseException {
        CreateClientDto createClientDTO = new CreateClientDto();
        createClientDTO.setFirstName("Mihailo");
        createClientDTO.setLastName("Petrović");
        createClientDTO.setEmail("mihailo@example.com");
        createClientDTO.setAddress("Bulevar Kralja Aleksandra 73");
        createClientDTO.setPhone("0612345678");
        createClientDTO.setGender("M");
        createClientDTO.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse("1995-08-20"));
        createClientDTO.setJmbg("1234567890123");
        createClientDTO.setUsername("mika123");

        Client client = new Client();
        client.setId(1L);
        client.setFirstName(createClientDTO.getFirstName());
        client.setLastName(createClientDTO.getLastName());
        client.setEmail(createClientDTO.getEmail());
        client.setAddress(createClientDTO.getAddress());
        client.setPhone(createClientDTO.getPhone());
        client.setGender(createClientDTO.getGender());
        client.setBirthDate(createClientDTO.getBirthDate());
        client.setJmbg(createClientDTO.getJmbg());
        client.setUsername(createClientDTO.getUsername());
        client.setPassword(""); // Po zahtevu je prazna

        ClientDto expectedDTO = new ClientDto(
                client.getId(), client.getFirstName(), client.getLastName(),
                client.getEmail(), client.getAddress(),
                client.getPhone(), client.getGender(), client.getBirthDate(),
                client.getJmbg(), client.getUsername());

        // "CLIENT" rola je već mockovana u @BeforeEach
        when(clientMapper.fromCreateDto(createClientDTO)).thenReturn(client);
        when(userRepository.existsByEmail(createClientDTO.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(createClientDTO.getUsername())).thenReturn(false);
        when(userRepository.findByJmbg(createClientDTO.getJmbg())).thenReturn(Optional.empty());
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toDto(client)).thenReturn(expectedDTO);

        ClientDto result = clientService.addClient(createClientDTO);

        assertNotNull(result);
        assertEquals("Mihailo", result.getFirstName());
        assertEquals("Petrović", result.getLastName());
        assertEquals("mihailo@example.com", result.getEmail());

        verify(rabbitTemplate).convertAndSend(eq("set-password"), any(EmailRequestDto.class));
        verify(authTokenRepository).save(any(AuthToken.class));
    }

    @Test
    public void testAddClient_ExistingEmail() {
        CreateClientDto createDto = new CreateClientDto();
        createDto.setEmail("existing@example.com");
        createDto.setUsername("uniqueUsername");
        createDto.setJmbg("12345");

        // Email vec postoji
        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(true);
        when(userRepository.existsByUsername(createDto.getUsername())).thenReturn(false);
        when(userRepository.findByJmbg("12345")).thenReturn(Optional.empty());

        assertThrows(EmailAlreadyExistsException.class, () -> clientService.addClient(createDto));
        verify(clientRepository, never()).save(any());
    }

    @Test
    public void testAddClient_ExistingUsername() {
        CreateClientDto createDto = new CreateClientDto();
        createDto.setEmail("unique@example.com");
        createDto.setUsername("existingUsername");
        createDto.setJmbg("12345");

        when(userRepository.existsByEmail("unique@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existingUsername")).thenReturn(true);
        when(userRepository.findByJmbg("12345")).thenReturn(Optional.empty());

        assertThrows(UserAlreadyExistsException.class, () -> clientService.addClient(createDto));
        verify(clientRepository, never()).save(any());
    }

    @Test
    public void testAddClient_ExistingJmbg() {
        CreateClientDto createDto = new CreateClientDto();
        createDto.setEmail("unique@example.com");
        createDto.setUsername("uniqueUsername");
        createDto.setJmbg("1234567890123");

        when(userRepository.existsByEmail("unique@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("uniqueUsername")).thenReturn(false);
        when(userRepository.findByJmbg("1234567890123")).thenReturn(Optional.of(new Client()));

        assertThrows(JmbgAlreadyExistsException.class, () -> clientService.addClient(createDto));
        verify(clientRepository, never()).save(any());
    }

    @Test
    public void testAddClient_ConstraintViolation() {
        // Simulacija slučaja ako dođe do greške validacije u bazi
        CreateClientDto createDto = new CreateClientDto();
        createDto.setEmail("test@example.com");
        createDto.setUsername("username");
        createDto.setJmbg("1234567890123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("username")).thenReturn(false);
        when(userRepository.findByJmbg("1234567890123")).thenReturn(Optional.empty());

        Client client = new Client();
        client.setEmail("test@example.com");
        when(clientMapper.fromCreateDto(any(CreateClientDto.class))).thenReturn(client);

        // Ovde code prilikom .save baci ConstraintViolationException => code hvata i
        // baca EmailAlreadyExistsException (po tvojoj metodi)
        when(clientRepository.save(client)).thenThrow(ConstraintViolationException.class);

        assertThrows(EmailAlreadyExistsException.class, () -> clientService.addClient(createDto));
    }

    // --------------------------------------------------------------------------------
    // updateClient(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testUpdateClient_Success() throws ParseException {
        UpdateClientDto updateClientDTO = new UpdateClientDto();
        updateClientDTO.setLastName("Perić");
        updateClientDTO.setAddress("Nova Adresa 100");
        updateClientDTO.setPhone("0601234567");
        updateClientDTO.setGender("M");

        Client existingClient = new Client();
        existingClient.setId(1L);
        existingClient.setEmail("stari@example.com");

        // Kad ga "ažuriramo", ostaju isti email i dr. polja
        Client updatedClient = new Client();
        updatedClient.setId(1L);
        updatedClient.setLastName("Perić");
        updatedClient.setAddress("Nova Adresa 100");
        updatedClient.setPhone("0601234567");
        updatedClient.setGender("M");
        updatedClient.setEmail("stari@example.com");

        ClientDto expectedDTO = new ClientDto();
        expectedDTO.setId(1L);
        expectedDTO.setLastName("Perić");
        expectedDTO.setAddress("Nova Adresa 100");
        expectedDTO.setPhone("0601234567");
        expectedDTO.setGender("M");
        expectedDTO.setEmail("stari@example.com");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(existingClient)).thenReturn(updatedClient);
        when(clientMapper.toDto(updatedClient)).thenReturn(expectedDTO);

        ClientDto result = clientService.updateClient(1L, updateClientDTO);

        assertNotNull(result);
        assertEquals("Perić", result.getLastName());
        assertEquals("stari@example.com", result.getEmail());
        assertEquals("Nova Adresa 100", result.getAddress());
    }

    @Test
    public void testUpdateClient_NotFound() {
        UpdateClientDto updateClientDTO = new UpdateClientDto();
        updateClientDTO.setLastName("Perić");
        updateClientDTO.setAddress("Nova Adresa 100");
        updateClientDTO.setPhone("0601234567");
        updateClientDTO.setGender("M");

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clientService.updateClient(999L, updateClientDTO));
        verify(clientRepository, never()).save(any(Client.class));
    }

    // --------------------------------------------------------------------------------
    // listClientsWithFilters(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testListClientsWithFilters() {
        Pageable pageable = PageRequest.of(0, 2);
        Client client1 = new Client();
        client1.setId(1L);
        client1.setEmail("john@example.com");

        Client client2 = new Client();
        client2.setId(2L);
        client2.setEmail("jane@example.com");

        Page<Client> page = new PageImpl<>(List.of(client1, client2));

        // Kod potrebe kasting: (Specification<Client>)
        when(clientRepository.findAll((Specification<Client>) any(), eq(pageable)))
                .thenReturn(page);

        ClientDto dto1 = new ClientDto();
        dto1.setId(1L);
        dto1.setEmail("john@example.com");

        ClientDto dto2 = new ClientDto();
        dto2.setId(2L);
        dto2.setEmail("jane@example.com");

        when(clientMapper.toDto(client1)).thenReturn(dto1);
        when(clientMapper.toDto(client2)).thenReturn(dto2);

        Page<ClientDto> result = clientService.listClientsWithFilters("john", "doe", "example", pageable);
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("john@example.com", result.getContent().get(0).getEmail());
        assertEquals("jane@example.com", result.getContent().get(1).getEmail());

        verify(clientRepository).findAll((Specification<Client>) any(), eq(pageable));
    }

    // --------------------------------------------------------------------------------
    // deleteClient(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testDeleteClient_Success() {
        when(clientRepository.existsById(1L)).thenReturn(true);
        doNothing().when(clientRepository).deleteById(1L);
        assertDoesNotThrow(() -> clientService.deleteClient(1L));
        verify(clientRepository).existsById(1L);
        verify(clientRepository).deleteById(1L);
    }

    @Test
    public void testDeleteClient_NotFound() {
        when(clientRepository.existsById(1L)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> clientService.deleteClient(1L));
        verify(clientRepository).existsById(1L);
        verify(clientRepository, never()).deleteById(anyLong());
    }

    // --------------------------------------------------------------------------------
    // findByEmail(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testFindByEmail_Success() {
        String email = "test@example.com";
        Client client = new Client();
        client.setEmail(email);

        ClientDto dto = new ClientDto();
        dto.setEmail(email);

        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(dto);

        ClientDto result = clientService.findByEmail(email);
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(clientRepository).findByEmail(email);
    }

    @Test
    public void testFindByEmail_NotFound() {
        String email = "notfound@example.com";
        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clientService.findByEmail(email));
        verify(clientRepository).findByEmail(email);
    }

    // --------------------------------------------------------------------------------
    // getCurrentClient(...)
    // --------------------------------------------------------------------------------
    @Test
    public void testGetCurrentClient_Success() {
        String email = "current@example.com";
        Client client = new Client();
        client.setEmail(email);

        ClientDto dto = new ClientDto();
        dto.setEmail(email);

        // 1) Mock-ujemo SecurityContext i Authentication
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        // 2) Kažemo SecurityContextHolder-u da vrati naš mock
        SecurityContextHolder.setContext(securityContext);

        // 3) Kažemo securityContext da ima authentication
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);

        // 4) Kažemo clientRepository da pronađe klijenta na osnovu email-a
        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(dto);

        ClientDto result = clientService.getCurrentClient();
        assertNotNull(result);
        assertEquals(email, result.getEmail());

        verify(clientRepository).findByEmail(email);
    }

    @Test
    public void testGetCurrentClient_NotFound() {
        String email = "unknown@example.com";

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);

        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clientService.getCurrentClient());
        verify(clientRepository).findByEmail(email);
    }
}
