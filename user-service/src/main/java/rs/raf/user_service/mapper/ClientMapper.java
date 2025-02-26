package rs.raf.user_service.mapper;

import org.springframework.stereotype.Component;
import rs.raf.user_service.dto.ClientDTO;
import rs.raf.user_service.entity.Client;

@Component
public class ClientMapper {

    public ClientDTO toDto(Client client) {
        if (client == null) return null; //  Dodatna provera
        return new ClientDTO(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getUsername(),
                client.getPassword(),
                client.getAddress(),
                client.getPhone(),
                client.getGender(),
                client.getBirthDate()
        );
    }

    public Client toEntity(ClientDTO clientDTO) {
        System.out.println("[ClientMapper] Mapping ClientDTO to Client: " + clientDTO);
        if (clientDTO == null) {
            System.out.println("[ClientMapper] Dobijen je null ClientDTO.");
            return null;
        }
        Client client = new Client();
        client.setId(clientDTO.getId());
        client.setFirstName(clientDTO.getFirstName());
        client.setLastName(clientDTO.getLastName());
        client.setEmail(clientDTO.getEmail());
        client.setUsername(clientDTO.getUsername());  // ✅ Dodato mapiranje username-a
        client.setPassword(clientDTO.getPassword());
        client.setAddress(clientDTO.getAddress());
        client.setPhone(clientDTO.getPhone());
        client.setGender(clientDTO.getGender());
        client.setBirthDate(clientDTO.getBirthDate());
        System.out.println("[ClientMapper] Mapped Client: " + client);
        return client;
    }

}
