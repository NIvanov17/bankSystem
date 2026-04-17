package bankSystem.accounts;

import bankSystem.enums.InterestCalculationMode;
import bankSystem.exceptions.MaximumBalanceException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class SavingsAccount extends BankAccount {
    private final BigDecimal interestRate;
    private final BigDecimal maximumBalance;

    public SavingsAccount(String accountNumber,
                          Customer owner,
                          BigDecimal dailyWithdrawLimit,
                          BigDecimal interestRate,
                          BigDecimal maximumBalance) {
        super(accountNumber, owner, dailyWithdrawLimit);
        this.interestRate = interestRate;
        this.maximumBalance = maximumBalance;
    }


    public synchronized void applyInterestRate(InterestCalculationMode mode){
        BigDecimal effectiveRate = switch (mode){
            case MONTHLY -> interestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            case YEARLY -> interestRate;
        };

        BigDecimal interest = getBalanceSafely().multiply(effectiveRate)
                .setScale(2, RoundingMode.HALF_UP);

        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            applyInterestInternal(interest);
        }
    }
}
