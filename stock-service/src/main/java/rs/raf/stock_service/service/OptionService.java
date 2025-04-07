package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
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

        BigDecimal optionPrice;
        if (type == OptionType.CALL) {
            BigDecimal intrinsicValue = currentPrice.subtract(strikePrice).max(BigDecimal.ZERO);
            optionPrice = intrinsicValue.add(timeValue);
        } else if (type == OptionType.PUT) {
            BigDecimal intrinsicValue = strikePrice.subtract(currentPrice).max(BigDecimal.ZERO);
            optionPrice = intrinsicValue.add(timeValue);
        } else {
            return BigDecimal.ZERO;
        }

        // Ovo je fallback da ne bi imali 0.00 za svaki slucaj zbog ovih kalkulacija
        if (optionPrice.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            return BigDecimal.valueOf(0.01);
        }

        return optionPrice.setScale(2, RoundingMode.HALF_UP);
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
        dto.setPrice(option.getPrice());

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
        dto.setPrice(option.getPrice());
        return dto;
    }
}
