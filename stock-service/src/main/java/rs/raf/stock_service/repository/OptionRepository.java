package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OptionType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {
    List<Option> findByUnderlyingStockIdAndSettlementDate(Long stockId, LocalDate settlementDate);

    List<Option>findAllByUnderlyingStock(Stock stock);

    @Query("SELECT o FROM Option o")
    List<Option> findAllOptions();

    @Query("SELECT o FROM Option o WHERE o.optionType = :optionType")
    List<Option> findByOptionType(@Param("optionType") OptionType optionType);

    Optional<Option> findByTicker(String ticker);
}
