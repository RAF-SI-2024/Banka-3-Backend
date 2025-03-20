package rs.raf.bank_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooOptionChainDto {

    private List<YahooOptionResultDto> result;
    private Object error; // moze biti i null

    public List<YahooOptionResultDto> getResult() {
        return result;
    }

    public void setResult(List<YahooOptionResultDto> result) {
        this.result = result;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }
}
