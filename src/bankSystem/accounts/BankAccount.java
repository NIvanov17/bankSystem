package bankSystem.accounts;

import bankSystem.enums.TransactionType;
import bankSystem.exceptions.ExceededLimitException;
import bankSystem.exceptions.InsufficientFundsException;
import bankSystem.transaction.Transaction;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BankAccount {
    private final String accountNumber;
    private final Customer owner;
    private BigDecimal balance;
    private final BigDecimal dailyWithdrawalLimit;
    private BigDecimal withdrawnToday;
    private LocalDate withdrawTrackingDate;
    private final List<Transaction> transactionHistory = new ArrayList<>();

    public BankAccount(String accountNumber, Customer owner, BigDecimal dailyWithdrawalLimit) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.dailyWithdrawalLimit = dailyWithdrawalLimit;
        this.withdrawnToday = BigDecimal.ZERO;
        this.balance = BigDecimal.ZERO;
        this.withdrawTrackingDate = LocalDate.now();
    }


    public synchronized void deposit(BigDecimal amount) {
        validateAmount(amount);
        balance = balance.add(amount);
        addTransaction(TransactionType.DEPOSIT, amount, null, accountNumber);
    }

    public synchronized void withdraw(BigDecimal amount) {
        validateAmount(amount);
        refreshDailyTrackingIfNeeded();
        enforceDailyLimit(amount);
        enforceWithdrawalAllowed(amount);
        balance = balance.subtract(amount);
        withdrawnToday = withdrawnToday.add(amount);
        addTransaction(TransactionType.WITHDRAW, amount, accountNumber, null);
    }

    public void transferTo(BankAccount recipient, BigDecimal amount) {
        if (recipient == null) {
            throw new IllegalArgumentException("Target account cannot be null");
        }

        if (this == recipient) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        validateAmount(amount);
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        BankAccount first = this.accountNumber.compareTo(recipient.getAccountNumber()) < 0 ? this : recipient;
        BankAccount second = this.accountNumber.compareTo(recipient.getAccountNumber()) < 0 ? recipient : this;
        synchronized (first) {
            synchronized (second) {
                refreshDailyTrackingIfNeeded();
                enforceDailyLimit(amount);
                enforceWithdrawalAllowed(amount);
                this.balance = this.balance.subtract(amount);
                this.withdrawnToday = this.withdrawnToday.add(amount);
                recipient.balance = recipient.balance.add(amount);
                this.addTransaction(TransactionType.TRANSFER_SENT,amount, this.accountNumber, recipient.getAccountNumber());
                recipient.addTransaction(TransactionType.TRANSFER_RECEIVED, amount, recipient.getAccountNumber(), this.accountNumber);
            }
        }
    }

    public synchronized List<Transaction> getTransactionHistory() {
        return List.copyOf(transactionHistory);
    }

    public synchronized void applyInterestInternal(BigDecimal interestRate) {
        balance = balance.add(interestRate);
        addTransaction(TransactionType.INTEREST, interestRate, null, accountNumber);
    }

    public synchronized void chargeFee(BigDecimal fee) {
        enforceWithdrawalAllowed(fee);
        balance = balance.subtract(fee);
        addTransaction(TransactionType.FEE, fee, null, accountNumber);
    }

    public void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
    }

    private void addTransaction(TransactionType type, BigDecimal amount, String fromAccount, String toAccount) {
        transactionHistory.add(new Transaction(
                LocalDateTime.now(),
                type,
                amount,
                fromAccount,
                toAccount,
                balance));
    }

    private void refreshDailyTrackingIfNeeded() {
        LocalDate currentDate = LocalDate.now();
        if (!currentDate.equals(withdrawTrackingDate)) {
            withdrawnToday = BigDecimal.ZERO;
            withdrawTrackingDate = currentDate;
        }
    }

    private void enforceDailyLimit(BigDecimal amount) {
        if (withdrawnToday.add(amount).compareTo(dailyWithdrawalLimit) > 0) {
            throw new ExceededLimitException("Daily withdrawal limit exceeded");
        }
    }

    public synchronized BigDecimal getBalanceSafely() {
        return balance;
    }

    public void enforceWithdrawalAllowed(BigDecimal amount) {
        if (balance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
    }

}
