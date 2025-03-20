package rs.raf.bank_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooOptionQuoteDto {

    private String contractSymbol;  // npr. "AAPL230317C00160000"
    private double strike;
    private double lastPrice;
    private int openInterest;
    private double impliedVolatility;

    private long expiration;
    private long lastTradeDate;

    public String getContractSymbol() { return contractSymbol; }
    public void setContractSymbol(String contractSymbol) { this.contractSymbol = contractSymbol; }

    public double getStrike() { return strike; }
    public void setStrike(double strike) { this.strike = strike; }

    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }

    public int getOpenInterest() { return openInterest; }
    public void setOpenInterest(int openInterest) { this.openInterest = openInterest; }

    public double getImpliedVolatility() { return impliedVolatility; }
    public void setImpliedVolatility(double impliedVolatility) { this.impliedVolatility = impliedVolatility; }

    public long getExpiration() { return expiration; }
    public void setExpiration(long expiration) { this.expiration = expiration; }

    public long getLastTradeDate() { return lastTradeDate; }
    public void setLastTradeDate(long lastTradeDate) { this.lastTradeDate = lastTradeDate; }
}
