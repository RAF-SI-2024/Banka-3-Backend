package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;



public interface CardRepository extends JpaRepository<Card, Long> {
    Long countByAccount(Account account);
}
