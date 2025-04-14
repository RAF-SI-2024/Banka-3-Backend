package rs.raf.stock_service.exceptions;

import java.math.BigDecimal;

public class PortfolioAmountNotEnoughException  extends RuntimeException{
    public PortfolioAmountNotEnoughException(Integer portfolioAmount, Integer orderAmount) {
        super("Portfolio amount of " + portfolioAmount + " not enough for SELL order of " + orderAmount + ".");
    }
}
