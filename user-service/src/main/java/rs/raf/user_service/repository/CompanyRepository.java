package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
