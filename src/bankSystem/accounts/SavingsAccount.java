package bankSystem.accounts;

import bankSystem.enums.InterestCalculationMode;
import bankSystem.exceptions.MaximumBalanceException;
import bankSystem.util.BalanceUtils;
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
            case MONTHLY -> BalanceUtils.divide(interestRate, BigDecimal.valueOf(12));
            case YEARLY -> interestRate;
        };

        BigDecimal interest = BalanceUtils.multiply(getBalanceSafely(), effectiveRate);

        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            applyInterestInternal(interest);
        }
    }

    @Override
    protected void enforceDepositAllowed(BigDecimal amount) {
        BigDecimal newBalance = getBalanceSafely().add(amount);
        if (newBalance.compareTo(maximumBalance) > 0) {
            throw new MaximumBalanceException("Maximum balance exceeded");
        }
    }
}
