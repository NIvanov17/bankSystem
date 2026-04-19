package bankSystem.accounts;

import bankSystem.exceptions.OverdraftLimitExceededException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CheckingAccount extends BankAccount {
    private final BigDecimal overdraftLimit;
    private final BigDecimal overdraftFee;
    private LocalDate lastOverdraftFeeDate;

    public CheckingAccount(String accountNumber,
                           Customer owner,
                           BigDecimal dailyWithdrawalLimit,
                           BigDecimal overdraftLimit, BigDecimal overdraftFee) {
        super(accountNumber, owner, dailyWithdrawalLimit);
        this.overdraftLimit = overdraftLimit;
        this.overdraftFee = overdraftFee;
    }

    @Override
    public void enforceWithdrawalAllowed(BigDecimal amount) {
        BigDecimal minAllowBalance = overdraftLimit.negate();
        if(getBalanceSafely().subtract(amount).compareTo(minAllowBalance) < 0){
            throw new OverdraftLimitExceededException("Overdraft limit exceeded");
        }
    }

    @Override
    protected void afterDebit(BigDecimal amount) {
        if (getBalanceSafely().compareTo(BigDecimal.ZERO) < 0) {
            LocalDate now = LocalDate.now();
            boolean chargedToday = now.equals(lastOverdraftFeeDate);

            if (!chargedToday) {
                chargeFee(overdraftFee);
                lastOverdraftFeeDate = now;
            }
        }
    }
}
