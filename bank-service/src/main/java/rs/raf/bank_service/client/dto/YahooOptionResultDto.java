package rs.raf.bank_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooOptionResultDto {

    private String underlyingSymbol;
    // "AAPL" npr.

    private List<YahooOptionDetailsDto> options;

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public void setUnderlyingSymbol(String underlyingSymbol) {
        this.underlyingSymbol = underlyingSymbol;
    }

    public List<YahooOptionDetailsDto> getOptions() {
        return options;
    }

    public void setOptions(List<YahooOptionDetailsDto> options) {
        this.options = options;
    }
}
