package rs.raf.user_service.mapper;


import org.springframework.stereotype.Component;
import rs.raf.user_service.dto.ClientDto;
import rs.raf.user_service.dto.CreateClientDto;
import rs.raf.user_service.dto.UpdateClientDto;
import rs.raf.user_service.entity.Client;

@Component
public class ClientMapper {

    // ✅ Mapiranje iz Client u ClientDTO (sa svim poljima)
    public ClientDto toDto(Client client) {
        if (client == null) return null;
        return new ClientDto(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getAddress(),
                client.getPhone(),
                client.getGender(),
                client.getBirthDate(),
                client.getJmbg()
        );
    }

    // ✅ Mapiranje iz ClientDTO u Client
    public Client toEntity(ClientDto clientDTO) {
        if (clientDTO == null) return null;
        Client client = new Client();
        client.setId(clientDTO.getId());
        client.setFirstName(clientDTO.getFirstName());
        client.setLastName(clientDTO.getLastName());
        client.setEmail(clientDTO.getEmail());
        client.setAddress(clientDTO.getAddress());
        client.setPhone(clientDTO.getPhone());
        client.setGender(clientDTO.getGender());
        client.setBirthDate(clientDTO.getBirthDate());
        client.setJmbg(clientDTO.getJmbg());
        return client;
    }

    // ✅ Nova metoda: Mapiranje iz CreateClientDTO u Client (prilikom kreiranja)
    public Client fromCreateDto(CreateClientDto createClientDTO) {
        if (createClientDTO == null) return null;
        Client client = new Client();
        client.setFirstName(createClientDTO.getFirstName());
        client.setLastName(createClientDTO.getLastName());
        client.setEmail(createClientDTO.getEmail());
        client.setAddress(createClientDTO.getAddress());
        client.setPhone(createClientDTO.getPhone());
        client.setGender(createClientDTO.getGender());
        client.setBirthDate(createClientDTO.getBirthDate());
        client.setPassword("");  // ✅ Lozinka ostaje prazna prilikom kreiranja
        client.setJmbg(createClientDTO.getJmbg());
        return client;
    }

    // ✅ Nova metoda: Ažuriranje entiteta na osnovu UpdateClientDTO
    public void fromUpdateDto(UpdateClientDto updateClientDTO, Client client) {
        if (updateClientDTO == null || client == null) return;
        client.setLastName(updateClientDTO.getLastName());
        client.setAddress(updateClientDTO.getAddress());
        client.setPhone(updateClientDTO.getPhone());
        client.setGender(updateClientDTO.getGender());
    }
}

