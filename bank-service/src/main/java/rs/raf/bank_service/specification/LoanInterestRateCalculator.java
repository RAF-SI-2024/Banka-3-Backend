package rs.raf.bank_service.specification;

import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.exceptions.UnkownLoanTypeException;

import java.math.BigDecimal;

public class LoanInterestRateCalculator {

    public static BigDecimal calculateNominalRate(LoanRequest request) {
        return getBaseRate(request.getAmount());
    }

    public static BigDecimal calculateEffectiveRate(LoanRequest request) {
        return getBaseRate(request.getAmount()).add(getMarginByLoanType(request.getType()));
    }

    private static BigDecimal getBaseRate(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("500000")) <= 0) return new BigDecimal("6.25");
        if (amount.compareTo(new BigDecimal("1000000")) <= 0) return new BigDecimal("6.00");
        if (amount.compareTo(new BigDecimal("2000000")) <= 0) return new BigDecimal("5.75");
        if (amount.compareTo(new BigDecimal("5000000")) <= 0) return new BigDecimal("5.50");
        if (amount.compareTo(new BigDecimal("10000000")) <= 0) return new BigDecimal("5.25");
        if (amount.compareTo(new BigDecimal("20000000")) <= 0) return new BigDecimal("5.00");
        return new BigDecimal("4.75");
    }

    private static BigDecimal getMarginByLoanType(LoanType type) {
        switch (type) {
            case CASH:
                return new BigDecimal("1.75");
            case MORTGAGE:
                return new BigDecimal("1.50");
            case AUTO:
                return new BigDecimal("1.25");
            case REFINANCING:
                return new BigDecimal("1.00");
            case STUDENT:
                return new BigDecimal("0.75");
            default:
                throw new UnkownLoanTypeException();
        }
    }
}
