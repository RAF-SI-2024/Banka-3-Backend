package rs.raf.bank_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooOptionDetailsDto {

    private long expirationDate;

    private List<YahooOptionQuoteDto> calls;
    private List<YahooOptionQuoteDto> puts;

    public long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public List<YahooOptionQuoteDto> getCalls() {
        return calls;
    }

    public void setCalls(List<YahooOptionQuoteDto> calls) {
        this.calls = calls;
    }

    public List<YahooOptionQuoteDto> getPuts() {
        return puts;
    }

    public void setPuts(List<YahooOptionQuoteDto> puts) {
        this.puts = puts;
    }
}
