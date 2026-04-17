package bankSystem.accounts;

import bankSystem.exceptions.OverdraftLimitExceededException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CheckingAccount extends BankAccount {
    private final BigDecimal overdraftLimit;
    private LocalDate lastOverdraftFeeDate;

    public CheckingAccount(String accountNumber,
                           Customer owner,
                           BigDecimal dailyWithdrawalLimit,
                           BigDecimal overdraftLimit) {
        super(accountNumber, owner, dailyWithdrawalLimit);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public void enforceWithdrawalAllowed(BigDecimal amount) {
        BigDecimal minAllowBalance = overdraftLimit.negate();
        if(getBalanceSafely().subtract(amount).compareTo(minAllowBalance) < 0){
            throw new OverdraftLimitExceededException("Overdraft limit exceeded");
        }
    }

    public synchronized boolean chargeOverdraftFee(){
        LocalDate now = LocalDate.now();

        boolean accountNegative = getBalanceSafely().compareTo(BigDecimal.ZERO) < 0;
        boolean chargedToday = now.equals(lastOverdraftFeeDate);

        if(accountNegative && !chargedToday){
            lastOverdraftFeeDate = now;
            return true;
        }
        return false;
    }
}
