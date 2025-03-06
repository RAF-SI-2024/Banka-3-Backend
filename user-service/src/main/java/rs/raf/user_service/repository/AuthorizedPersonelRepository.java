package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.domain.entity.AuthorizedPersonel;
import rs.raf.user_service.domain.entity.Company;

import java.util.List;

public interface AuthorizedPersonelRepository extends JpaRepository<AuthorizedPersonel, Long> {
    List<AuthorizedPersonel> findByCompany(Company company);

    List<AuthorizedPersonel> findByCompanyId(Long companyId);
}