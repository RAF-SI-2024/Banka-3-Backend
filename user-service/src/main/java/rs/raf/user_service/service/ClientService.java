package rs.raf.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.ClientDTO;
import rs.raf.user_service.dto.CreateClientDTO;
import rs.raf.user_service.dto.UpdateClientDTO;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.mapper.ClientMapper;
import rs.raf.user_service.repository.ClientRepository;

import java.util.List;
import java.util.NoSuchElementException;
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

    public List<ClientDTO> listClients(int page, int size) {
        Page<Client> clientsPage = clientRepository.findAll(PageRequest.of(page, size));
        return clientsPage.stream().map(clientMapper::toDto).collect(Collectors.toList());
    }

    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + id));
        return clientMapper.toDto(client);
    }

    // Kreiranje klijenta bez lozinke (password ostaje prazan)
    public ClientDTO addClient(CreateClientDTO createClientDto) {
        System.out.println("[addClient] Pozvana metoda sa podacima: " + createClientDto);

        Client client = clientMapper.fromCreateDto(createClientDto);
        client.setPassword("");  // Lozinka ostaje prazna
        System.out.println("[addClient] Lozinka ostavljena prazna prilikom kreiranja.");

        Client savedClient = clientRepository.save(client);
        System.out.println("[addClient] Klijent sačuvan u bazi: " + savedClient);

        return clientMapper.toDto(savedClient);
    }

    // Ažuriranje samo dozvoljenih polja (email i druge vrednosti se ne diraju)
    public ClientDTO updateClient(Long id, UpdateClientDTO updateClientDto) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + id));

        existingClient.setFirstName(updateClientDto.getFirstName());
        existingClient.setLastName(updateClientDto.getLastName());
        existingClient.setAddress(updateClientDto.getAddress());
        existingClient.setPhone(updateClientDto.getPhone());
        existingClient.setGender(updateClientDto.getGender());
        existingClient.setBirthDate(updateClientDto.getBirthDate());

        Client updatedClient = clientRepository.save(existingClient);
        System.out.println("[updateClient] Klijent ažuriran: " + updatedClient);

        return clientMapper.toDto(updatedClient);
    }

    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new NoSuchElementException("Client not found with ID: " + id);
        }
        clientRepository.deleteById(id);
        System.out.println("[deleteClient] Klijent sa ID " + id + " uspešno obrisan.");
    }
}
