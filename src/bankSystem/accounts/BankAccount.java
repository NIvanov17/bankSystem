package bankSystem.accounts;

import bankSystem.enums.TransactionType;
import bankSystem.exceptions.ExceededLimitException;
import bankSystem.exceptions.InsufficientFundsException;
import bankSystem.transaction.Transaction;
import bankSystem.util.BalanceUtils;
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
    private final List<Transaction> transactionHistory = new ArrayList<>();

    public BankAccount(String accountNumber, Customer owner, BigDecimal dailyWithdrawalLimit) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.dailyWithdrawalLimit = dailyWithdrawalLimit;
        this.balance = BigDecimal.ZERO;
    }


    public synchronized void deposit(BigDecimal amount) {
        beforeCredit(amount);
        balance = BalanceUtils.add(balance, amount);
        addTransaction(TransactionType.DEPOSIT, amount, null, accountNumber);
    }

    public synchronized void withdraw(BigDecimal amount) {
        beforeDebit(amount);
        applyDebit(amount);
        addTransaction(TransactionType.WITHDRAW, amount, accountNumber, null);
        afterDebit(amount);
    }

    public void transferTo(BankAccount recipient, BigDecimal amount) {
        validateAmount(amount);

        BankAccount first = this.accountNumber.compareTo(recipient.getAccountNumber()) < 0 ? this : recipient;
        BankAccount second = this.accountNumber.compareTo(recipient.getAccountNumber()) < 0 ? recipient : this;
        synchronized (first) {
            synchronized (second) {
                this.beforeDebit(amount);
                recipient.beforeCredit(amount);

                this.applyDebit(amount);
                recipient.applyCredit(amount);

                this.addTransaction(TransactionType.TRANSFER_SENT, amount, this.accountNumber, recipient.getAccountNumber());
                recipient.addTransaction(TransactionType.TRANSFER_RECEIVED, amount, this.accountNumber, recipient.getAccountNumber());

                this.afterDebit(amount);
            }
        }
    }

    public synchronized List<Transaction> getTransactionHistory() {
        return List.copyOf(transactionHistory);
    }

    public synchronized void applyInterestInternal(BigDecimal interestRate) {
        enforceDepositAllowed(interestRate);
        balance = balance.add(interestRate);
        addTransaction(TransactionType.INTEREST, interestRate, null, accountNumber);
    }

    public synchronized void chargeFee(BigDecimal fee) {
        enforceWithdrawalAllowed(fee);
        balance = BalanceUtils.subtract(balance, fee);
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

    private void enforceDailyLimit(BigDecimal amount) {
        BigDecimal withdrawnToday = getWithdrawnAmountForDate(LocalDate.now());
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

    private BigDecimal getWithdrawnAmountForDate(LocalDate date) {
        return transactionHistory.stream()
                .filter(transaction -> transaction.getTimestamp().toLocalDate().equals(date))
                .filter(transaction ->
                        transaction.getType() == TransactionType.WITHDRAW ||
                                transaction.getType() == TransactionType.TRANSFER_SENT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected void enforceDepositAllowed(BigDecimal amount){

    }

    protected final void beforeCredit(BigDecimal amount) {
        validateAmount(amount);
        enforceDepositAllowed(amount);
    }

    protected final void beforeDebit(BigDecimal amount) {
        validateAmount(amount);
        enforceDailyLimit(amount);
        enforceWithdrawalAllowed(amount);
    }

    protected final void applyCredit(BigDecimal amount) {
        balance = BalanceUtils.add(balance, amount);
    }

    protected final void applyDebit(BigDecimal amount) {
        balance = BalanceUtils.subtract(balance, amount);
    }

    protected void afterDebit(BigDecimal amount) {
    }

}

