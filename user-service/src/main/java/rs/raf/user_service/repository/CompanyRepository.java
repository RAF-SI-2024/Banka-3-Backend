package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import rs.raf.user_service.entity.AuthToken;
import rs.raf.user_service.entity.Client;
=======
>>>>>>> upstream/main
import rs.raf.user_service.entity.Company;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByRegistrationNumber(String registrationNumber);
<<<<<<< HEAD
=======

>>>>>>> upstream/main
    Optional<Company> findByTaxId(String taxId);

}
