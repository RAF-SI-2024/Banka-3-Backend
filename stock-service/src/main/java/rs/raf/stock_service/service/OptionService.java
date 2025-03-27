package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.OptionDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.repository.OptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OptionService {

    private OptionRepository optionRepository;

    /**
     * Generiše opcije za dati stock koristeći ručni model.
     * - Za trenutnu cenu se zaokružuje na najbliži ceo broj.
     * - Definišu se strike cene: 5 ispod i 5 iznad zaokružene cene (ukupno 11 strike vrijednosti).
     * - Prvi datum isteka je 6 dana od danas, zatim se generišu opcije svakih 6 dana dok razlika ne dostigne 30 dana,
     * a potom se dodaje još 6 datuma sa razmakom od 30 dana.
     * - Za svaku kombinaciju datuma i strike cijene kreira se CALL i PUT opcija.
     * - Derived maintenance margin za opcije = 100 * 0.5 * currentPrice = 50 * currentPrice.
     *
     * @param stockListing simbol stocka, npr. "AAPL"
     * @param currentPrice trenutna cijena dionice
     * @return Lista generiranih opcija kao OptionDto
     */
    public List<OptionDto> generateOptions(String stockListing, BigDecimal currentPrice) {
        List<OptionDto> options = new ArrayList<>();
        int roundedPrice = currentPrice.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
        int lowerBound = roundedPrice - 5;
        int upperBound = roundedPrice + 5;

        // Prvi datum isteka je 6 dana od danas
        LocalDate firstExpiry = LocalDate.now().plusDays(6);
        List<LocalDate> expiryDates = new ArrayList<>();

        // Generiramo opcije svakih 6 dana dok razlika ne dostigne 30 dana
        LocalDate expiry = firstExpiry;
        while (!expiry.isAfter(firstExpiry.plusDays(30))) {
            expiryDates.add(expiry);
            expiry = expiry.plusDays(6);
        }
        // Dodajemo dodatnih 6 datuma sa razmakom od 30 dana
        for (int i = 1; i <= 6; i++) {
            expiryDates.add(expiry.plusDays((long) (i - 1) * 30));
        }

        // Derived maintenance margin = 50 * currentPrice
        BigDecimal derivedMargin = currentPrice.multiply(new BigDecimal("50"));

        for (LocalDate exp : expiryDates) {
            for (int strike = lowerBound; strike <= upperBound; strike++) {
                OptionDto callOption = new OptionDto();
                callOption.setStockListing(stockListing);
                callOption.setOptionType("Call");
                callOption.setStrikePrice(BigDecimal.valueOf(strike));
                callOption.setContractSize(BigDecimal.valueOf(100));
                callOption.setSettlementDate(exp);
                callOption.setMaintenanceMargin(derivedMargin);
                options.add(callOption);

                OptionDto putOption = new OptionDto();
                putOption.setStockListing(stockListing);
                putOption.setOptionType("Put");
                putOption.setStrikePrice(BigDecimal.valueOf(strike));
                putOption.setContractSize(BigDecimal.valueOf(100));
                putOption.setSettlementDate(exp);
                putOption.setMaintenanceMargin(derivedMargin);
                options.add(putOption);
            }
        }
        return options;
    }

    public OptionDto getOptionByTicker(String ticker) {
        Option option = optionRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Option not found for ticker: " + ticker));

        OptionDto dto = new OptionDto();
        dto.setStockListing(option.getUnderlyingStock().getTicker());
        dto.setOptionType(option.getOptionType().name());
        dto.setStrikePrice(option.getStrikePrice());
        dto.setContractSize(option.getContractSize());
        dto.setSettlementDate(option.getSettlementDate());
        dto.setMaintenanceMargin(option.getMaintenanceMargin());

        return dto;
    }

    public List<OptionDto> getAllOptions() {
        return optionRepository.findAllOptions().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<OptionDto> getOptionsByType(OptionType type) {
        return optionRepository.findByOptionType(type).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private OptionDto toDto(Option option) {
        OptionDto dto = new OptionDto();
        dto.setStockListing(option.getUnderlyingStock().getTicker());
        dto.setOptionType(option.getOptionType().name());
        dto.setStrikePrice(option.getStrikePrice());
        dto.setContractSize(option.getContractSize());
        dto.setSettlementDate(option.getSettlementDate());
        dto.setMaintenanceMargin(option.getMaintenanceMargin());
        return dto;
    }
}
