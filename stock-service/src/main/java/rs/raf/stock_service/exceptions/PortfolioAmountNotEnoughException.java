package rs.raf.stock_service.exceptions;

import java.math.BigDecimal;

public class PortfolioAmountNotEnoughException  extends RuntimeException{
    public PortfolioAmountNotEnoughException(Integer portfolioAmount, Integer orderAmount) {
        super("Portfolio available amount of " + portfolioAmount + " not enough to cover amount of " + orderAmount + ".");
    }
}
