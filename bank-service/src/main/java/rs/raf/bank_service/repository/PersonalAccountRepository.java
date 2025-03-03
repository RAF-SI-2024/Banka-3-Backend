package rs.raf.bank_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.PersonalAccount;

public interface PersonalAccountRepository extends JpaRepository<PersonalAccount, Long> {
}
