package rs.raf.user_service.service;

import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.ClientDto;
import rs.raf.user_service.dto.CreateClientDto;
import rs.raf.user_service.dto.EmailRequestDto;
import rs.raf.user_service.dto.UpdateClientDto;
import rs.raf.user_service.entity.AuthToken;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.exceptions.EmailAlreadyExistsException;
import rs.raf.user_service.exceptions.JmbgAlreadyExistsException;
import rs.raf.user_service.exceptions.UserAlreadyExistsException;
import rs.raf.user_service.mapper.ClientMapper;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.specification.ClientSearchSpecification;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RabbitTemplate rabbitTemplate;

    public Page<ClientDto> listClients(Pageable pageable) {
        Page<Client> clientsPage = clientRepository.findAll(pageable);
        return clientsPage.map(clientMapper::toDto);
    }

    public ClientDto getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found with ID: " + id));
        return clientMapper.toDto(client);
    }

    public ClientDto addClient(CreateClientDto createClientDto) {
        Client client = clientMapper.fromCreateDto(createClientDto);
        client.setPassword("");

        if (userRepository.existsByEmail(createClientDto.getEmail()))
            throw new EmailAlreadyExistsException();
        if (userRepository.existsByUsername(createClientDto.getUsername()))
            throw new UserAlreadyExistsException();
        if (userRepository.findByJmbg(createClientDto.getJmbg()).isPresent())
            throw new JmbgAlreadyExistsException();
        try {
            Client savedClient = clientRepository.save(client);

            UUID token = UUID.fromString(UUID.randomUUID().toString());
            EmailRequestDto emailRequestDto = new EmailRequestDto(token.toString(), client.getEmail());


            rabbitTemplate.convertAndSend("set-password", emailRequestDto);


            Long createdAt = Instant.now().toEpochMilli();
            Long expiresAt = createdAt + 86400000;//24h
            AuthToken authToken = new AuthToken(createdAt, expiresAt, token.toString(), "set-password", client.getId());
            authTokenRepository.save(authToken);

            return clientMapper.toDto(savedClient);
        } catch (ConstraintViolationException e) {
            throw new EmailAlreadyExistsException();
        }

    }

    // Ažuriranje samo dozvoljenih polja (email i druge vrednosti se ne diraju)
    public ClientDto updateClient(Long id, UpdateClientDto updateClientDto) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found with ID: " + id));

        if (!existingClient.getEmail().equals(updateClientDto.getEmail())) {
            clientRepository.findByEmail(updateClientDto.getEmail())
                    .ifPresent(c -> {
                        throw new EmailAlreadyExistsException();
                    });
            existingClient.setEmail(updateClientDto.getEmail());
        }

        existingClient.setLastName(updateClientDto.getLastName());
        existingClient.setAddress(updateClientDto.getAddress());
        existingClient.setPhone(updateClientDto.getPhone());
        existingClient.setGender(updateClientDto.getGender());

        Client updatedClient = clientRepository.save(existingClient);
        System.out.println("[updateClient] Klijent ažuriran: " + updatedClient);

        return clientMapper.toDto(updatedClient);
    }

    public Page<ClientDto> listClientsWithFilters(String firstName, String lastName, String email, Pageable pageable) {
        Specification<Client> spec = Specification
                .where(ClientSearchSpecification.firstNameContains(firstName))
                .and(ClientSearchSpecification.lastNameContains(lastName))
                .and(ClientSearchSpecification.emailContains(email));

        Page<Client> clientsPage = clientRepository.findAll(spec, pageable);
        return clientsPage.map(clientMapper::toDto);
    }

    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new NoSuchElementException("Client not found with ID: " + id);
        }
        clientRepository.deleteById(id);
        System.out.println("[deleteClient] Klijent sa ID " + id + " uspešno obrisan.");
    }

    public ClientDto findByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with email: " + email));
        return clientMapper.toDto(client);
    }


    public ClientDto getCurrentClient() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Client client = clientRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("Client not found with email: " + email));

        return clientMapper.toDto(client);
    }
}
