package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.OptionDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.repository.OptionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * - Definišu se strike cene: 5 ispod i 5 iznad zaokružene cene (ukupno 11 strike vrednosti).
     * - Prvi datum isteka je 6 dana od danas, zatim se generišu opcije svakih 6 dana dok razlika ne dostigne 30 dana,
     * a potom se dodaje još 6 datuma sa razmakom od 30 dana.
     * - Za svaku kombinaciju datuma i strike cene kreira se CALL i PUT opcija.
     * - Derived maintenance margin za opcije = 100 * 0.5 * currentPrice = 50 * currentPrice.
     *
     * @param stockListing simbol stocka, npr. "AAPL"
     * @param currentPrice trenutna cena deonice
     * @return Lista generisanih opcija kao OptionDto
     */
    public List<OptionDto> generateOptions(String stockListing, BigDecimal currentPrice) {
        List<OptionDto> options = new ArrayList<>();
        int roundedPrice = currentPrice.setScale(0, RoundingMode.HALF_UP).intValue();
        int lowerBound = roundedPrice - 5;
        int upperBound = roundedPrice + 5;

        LocalDate firstExpiry = LocalDate.now().plusDays(6);
        List<LocalDate> expiryDates = new ArrayList<>();

        LocalDate expiry = firstExpiry;
        while (!expiry.isAfter(firstExpiry.plusDays(30))) {
            expiryDates.add(expiry);
            expiry = expiry.plusDays(6);
        }

        for (int i = 1; i <= 6; i++) {
            expiryDates.add(expiry.plusDays((long) (i - 1) * 30));
        }

        // margin = 50 * currentPrice
        BigDecimal margin = currentPrice.multiply(new BigDecimal("50"));

        for (LocalDate exp : expiryDates) {
            for (int strike = lowerBound; strike <= upperBound; strike++) {
                BigDecimal strikePrice = BigDecimal.valueOf(strike);
                long daysUntilExpiry = LocalDate.now().until(exp).getDays();

                OptionDto callOption = createOption(stockListing, OptionType.CALL, currentPrice, strikePrice, exp, margin, daysUntilExpiry);
                options.add(callOption);

                OptionDto putOption = createOption(stockListing, OptionType.PUT, currentPrice, strikePrice, exp, margin, daysUntilExpiry);
                options.add(putOption);
            }
        }
        return options;
    }

    private OptionDto createOption(String stockListing, OptionType type, BigDecimal currentPrice,
                                   BigDecimal strikePrice, LocalDate expiry, BigDecimal margin, long daysUntilExpiry) {
        OptionDto option = new OptionDto();
        option.setStockListing(stockListing);
        option.setOptionType(type);
        option.setStrikePrice(strikePrice);
        option.setContractSize(BigDecimal.valueOf(100));
        option.setSettlementDate(expiry);
        option.setMaintenanceMargin(margin);
        option.setPrice(calculateOptionPrice(type, currentPrice, strikePrice, daysUntilExpiry));

        String ticker = stockListing + expiry.getYear() % 100 +
                String.format("%02d", expiry.getMonthValue()) +
                String.format("%02d", expiry.getDayOfMonth()) +
                (type == OptionType.CALL ? "C" : "P") +
                String.format("%08d", strikePrice.multiply(BigDecimal.valueOf(100)).intValue());
        option.setTicker(ticker);

        return option;
    }

    private BigDecimal calculateOptionPrice(OptionType type, BigDecimal currentPrice, BigDecimal strikePrice, long daysUntilExpiry) {
        BigDecimal timeValue = currentPrice.multiply(BigDecimal.valueOf(0.05))
                .multiply(BigDecimal.valueOf(Math.sqrt((double) daysUntilExpiry / 365)));

        if (type == OptionType.CALL) {
            BigDecimal intrinsicValue = currentPrice.subtract(strikePrice).max(BigDecimal.ZERO);
            return intrinsicValue.add(timeValue).setScale(2, RoundingMode.HALF_UP);
        } else if (type == OptionType.PUT) {
            BigDecimal intrinsicValue = strikePrice.subtract(currentPrice).max(BigDecimal.ZERO);
            return intrinsicValue.add(timeValue).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }


    public OptionDto getOptionByTicker(String ticker) {
        Option option = optionRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Option not found for ticker: " + ticker));

        OptionDto dto = new OptionDto();
        dto.setStockListing(option.getUnderlyingStock().getTicker());
        dto.setOptionType(option.getOptionType());
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
        dto.setOptionType(option.getOptionType());
        dto.setStrikePrice(option.getStrikePrice());
        dto.setContractSize(option.getContractSize());
        dto.setSettlementDate(option.getSettlementDate());
        dto.setMaintenanceMargin(option.getMaintenanceMargin());
        return dto;
    }
}
