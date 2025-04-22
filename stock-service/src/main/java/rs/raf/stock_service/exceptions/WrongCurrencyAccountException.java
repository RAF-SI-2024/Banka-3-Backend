package rs.raf.stock_service.exceptions;

public class WrongCurrencyAccountException extends RuntimeException{
    public WrongCurrencyAccountException(String currencyCode){
        super("Account not in " + currencyCode + ".");
    }
}
