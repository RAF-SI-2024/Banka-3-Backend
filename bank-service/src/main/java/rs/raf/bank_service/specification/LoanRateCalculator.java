package rs.raf.bank_service.specification;

import java.math.BigDecimal;

public class LoanRateCalculator {
    public static BigDecimal calculateMonthlyRate(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("100"), 6, BigDecimal.ROUND_HALF_UP);
        monthlyRate = monthlyRate.divide(new BigDecimal("12"), 6, BigDecimal.ROUND_HALF_UP);

        BigDecimal onePlusRPowerN = monthlyRate.add(BigDecimal.ONE).pow(months);
        BigDecimal installment = principal.multiply(monthlyRate.multiply(onePlusRPowerN))
                .divide(onePlusRPowerN.subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);

        return installment;
    }
}
