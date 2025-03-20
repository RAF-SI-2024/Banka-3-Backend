package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.client.YahooOptionApiClient;
import rs.raf.bank_service.client.dto.*;
import rs.raf.bank_service.domain.entity.OptionContract;
import rs.raf.bank_service.domain.enums.OptionType;
import rs.raf.bank_service.repository.OptionContractRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class OptionService {

    private final OptionContractRepository optionContractRepository;
    private final YahooOptionApiClient yahooOptionApiClient;

    /**
     * Preuzima “options chain” za dati ticker,
     * mapira svaku “call”/“put” u OptionContract i snima u bazu.
     */
    @Transactional
    public List<OptionContract> fetchOptionsFromYahoo(String ticker) {
        YahooOptionChainResponseDto responseDto = yahooOptionApiClient.fetchOptions(ticker);

        if (responseDto == null
                || responseDto.getOptionChain() == null
                || responseDto.getOptionChain().getResult() == null
                || responseDto.getOptionChain().getResult().isEmpty()) {
            return new ArrayList<>();
        }

        List<OptionContract> toSave = new ArrayList<>();
        YahooOptionResultDto resultDto = responseDto.getOptionChain().getResult().get(0);

        if (resultDto.getOptions() == null) {
            return new ArrayList<>();
        }

        String underlyingSymbol = resultDto.getUnderlyingSymbol(); // npr. "AAPL"

        for (YahooOptionDetailsDto detailsDto : resultDto.getOptions()) {
            // calls
            if (detailsDto.getCalls() != null) {
                for (YahooOptionQuoteDto call : detailsDto.getCalls()) {
                    OptionContract callContract = mapQuoteToEntity(underlyingSymbol, call, OptionType.CALL);
                    toSave.add(callContract);
                }
            }
            // puts
            if (detailsDto.getPuts() != null) {
                for (YahooOptionQuoteDto put : detailsDto.getPuts()) {
                    OptionContract putContract = mapQuoteToEntity(underlyingSymbol, put, OptionType.PUT);
                    toSave.add(putContract);
                }
            }
        }

        return optionContractRepository.saveAll(toSave);
    }

    private OptionContract mapQuoteToEntity(String symbol, YahooOptionQuoteDto quote, OptionType type) {
        OptionContract oc = new OptionContract();
        oc.setStockTicker(symbol);
        oc.setOptionType(type);
        oc.setOptionSymbol(quote.getContractSymbol());

        oc.setStrikePrice(BigDecimal.valueOf(quote.getStrike()));
        oc.setPremium(BigDecimal.valueOf(quote.getLastPrice()));
        oc.setOpenInterest(quote.getOpenInterest());

        oc.setImpliedVolatility(
                BigDecimal.valueOf(quote.getImpliedVolatility())
        );

        LocalDate expDate = Instant.ofEpochSecond(quote.getExpiration())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        oc.setExpirationDate(expDate);

        LocalDate ltd = Instant.ofEpochSecond(quote.getLastTradeDate())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        oc.setLastTradeDate(ltd);

        return oc;
    }

    public List<OptionContract> getAllOptions() {
        return optionContractRepository.findAll();
    }

    public OptionContract getBySymbol(String symbol) {
        return optionContractRepository.findByOptionSymbol(symbol);
    }
}
