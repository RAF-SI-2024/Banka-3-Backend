package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Client;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
    Optional<Client> findByJmbg(String jmbg);

}
