package rs.raf.bank_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CmeFuturesResponseDto {

    private String symbol;
    private String underlying;
    private String settlementDate;
    private Integer contractSize;
    private String unit;
    private Double price;

    // get/set
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getUnderlying() { return underlying; }
    public void setUnderlying(String underlying) { this.underlying = underlying; }

    public String getSettlementDate() { return settlementDate; }
    public void setSettlementDate(String settlementDate) { this.settlementDate = settlementDate; }

    public Integer getContractSize() { return contractSize; }
    public void setContractSize(Integer contractSize) { this.contractSize = contractSize; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
