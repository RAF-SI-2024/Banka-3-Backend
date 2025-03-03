package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Card;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByAccountClientId(Long clientId);

    Optional<Card> findByCardNumber(String cardNumber);

    Optional<Card> findByCardNumberAndAccountClientId(String cardNumber, Long clientId);
}