package rs.raf.bank_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooOptionChainResponseDto {

    private YahooOptionChainDto optionChain;

    public YahooOptionChainDto getOptionChain() {
        return optionChain;
    }

    public void setOptionChain(YahooOptionChainDto optionChain) {
        this.optionChain = optionChain;
    }
}
