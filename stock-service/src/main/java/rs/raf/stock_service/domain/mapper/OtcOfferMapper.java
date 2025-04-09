package rs.raf.stock_service.domain.mapper;

import lombok.*;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.OtcOfferDto;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.mapper.StockOptionMapper;


@Component
@AllArgsConstructor
public class OtcOfferMapper {

    private final StockMapper stockMapper;


    public OtcOfferDto toDto(OtcOffer offer, Long userId) {
        boolean canInteract = !offer.getLastModifiedById().equals(userId);

        return OtcOfferDto.builder()
                .id(offer.getId())
                .stock(stockMapper.toDto(offer.getStock()))
                .amount(offer.getAmount())
                .pricePerStock(offer.getPricePerStock())
                .premium(offer.getPremium())
                .settlementDate(offer.getSettlementDate())
                .status(offer.getStatus())
                .canInteract(canInteract)
                .build();
    }
}

