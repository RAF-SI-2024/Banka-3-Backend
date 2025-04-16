package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.OtcOption;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OtcOptionRepository extends JpaRepository<OtcOption, Long> {
    List<OtcOption> findAllByBuyerId(Long buyerId);

    @Query("SELECT o FROM OtcOption o WHERE o.buyerId = :buyerId AND (o.status = 'USED' OR o.settlementDate < :currentDate)")
    List<OtcOption> findAllInvalid(
            @Param("buyerId") Long buyerId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT o FROM OtcOption o WHERE o.buyerId = :buyerId AND o.status = 'VALID' AND o.settlementDate >= :currentDate")
    List<OtcOption> findAllValid(
            @Param("buyerId") Long buyerId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT o FROM OtcOption o WHERE o.status = 'VALID' AND o.settlementDate < :currentDate")
    List<OtcOption> findAllValidButExpired(@Param("currentDate") LocalDate currentDate);
}