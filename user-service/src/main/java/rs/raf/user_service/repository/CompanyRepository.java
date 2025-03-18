package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import rs.raf.user_service.domain.entity.Company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByRegistrationNumber(String registrationNumber);

    List<Company> findByMajorityOwner_Id(Long id);

    Optional<Company> findByTaxId(String taxId);

}
