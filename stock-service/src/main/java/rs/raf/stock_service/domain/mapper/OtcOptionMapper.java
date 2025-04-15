package rs.raf.stock_service.domain.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.OtcOptionDto;
import rs.raf.stock_service.domain.entity.OtcOption;
import rs.raf.stock_service.domain.enums.OtcOptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@AllArgsConstructor
public class OtcOptionMapper {
    public OtcOptionDto toDto(OtcOption option) {
//         seller = userRepository.findById(option.getSellerId())
//                .orElseThrow(() -> new RuntimeException("Prodavac nije pronađen"));
//        String sellerInfo = seller.getName() + ", " + seller.getBank();
        String sellerInfo = "Coming soon lol"; // uskoro dodati
        BigDecimal premium = option.getPremium();

        BigDecimal currentPrice = option.getUnderlyingStock().getPrice();
        BigDecimal profit = currentPrice.subtract(option.getStrikePrice())
                .multiply(BigDecimal.valueOf(option.getAmount()))
                .subtract(premium);

        return OtcOptionDto.builder()
                .id(option.getId())
                .stockSymbol(option.getUnderlyingStock().getTicker())
                .amount(option.getAmount())
                .strikePrice(option.getStrikePrice())
                .premium(premium)
                .settlementDate(option.getSettlementDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .sellerInfo(sellerInfo)
                .profit(profit)
                .status(option.getSettlementDate().isBefore(LocalDate.now()) ? OtcOptionStatus.EXPIRED : OtcOptionStatus.VALID)
                .used(option.isUsed())
                .currentPrice(currentPrice)
                .build();
    }
}
