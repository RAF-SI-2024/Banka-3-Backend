package rs.raf.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.ClientDTO;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.mapper.ClientMapper;
import rs.raf.user_service.repository.ClientRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Autowired
    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=(.*\\d){2,}).{8,32}$";
        if (!Pattern.matches(passwordRegex, password)) {
            throw new IllegalArgumentException("Password does not meet complexity requirements.");
        }
    }

    public List<ClientDTO> listClients(int page, int size) {
        Page<Client> clientsPage = clientRepository.findAll(PageRequest.of(page, size));
        return clientsPage.stream().map(clientMapper::toDto).collect(Collectors.toList());
    }

    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + id));
        return clientMapper.toDto(client);
    }

    public ClientDTO addClient(ClientDTO clientDTO) {
        System.out.println("[addClient] Pozvana metoda sa podacima: " + clientDTO);

        validatePassword(clientDTO.getPassword());
        System.out.println("[addClient] Validacija lozinke uspešna.");

        Client client = clientMapper.toEntity(clientDTO);
        System.out.println("[addClient] Mapiran Client entity: " + client);

        Client savedClient = clientRepository.save(client);
        System.out.println("[addClient] Klijent sačuvan u bazi: " + savedClient);

        ClientDTO result = clientMapper.toDto(savedClient);
        System.out.println("[addClient] Vraćen ClientDTO: " + result);

        return result;
    }

    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + id));

        if (clientDTO.getPassword() != null) validatePassword(clientDTO.getPassword());
        existingClient.setFirstName(clientDTO.getFirstName());
        existingClient.setLastName(clientDTO.getLastName());
        existingClient.setEmail(clientDTO.getEmail());

        Client updatedClient = clientRepository.save(existingClient);
        return clientMapper.toDto(updatedClient);
    }

    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new NoSuchElementException("Client not found with ID: " + id);
        }
        clientRepository.deleteById(id);
    }

}
