package bankSystem.bank;

import bankSystem.accounts.BankAccount;
import bankSystem.accounts.CheckingAccount;
import bankSystem.accounts.Customer;
import bankSystem.accounts.SavingsAccount;
import bankSystem.exceptions.UnknownAccountException;
import bankSystem.transaction.Statement;
import bankSystem.transaction.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class Bank {
    private final Map<String, BankAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong nextAccountNumber  = new AtomicLong(100000);
    private final AtomicLong nextCustomerId = new AtomicLong(1);

    private final String bankName;

    private BigDecimal withdrawalFee = BigDecimal.ZERO;
    private BigDecimal overdraftFee = BigDecimal.ZERO;

    public Bank(String bankName) {
        this.bankName = bankName;
    }

    public Customer registerCustomer(String name) {
        String customerId = String.valueOf(nextCustomerId.getAndIncrement());
        Customer customer = new Customer(customerId, name);
        customers.put(customerId, customer);
        return customer;
    }

    public SavingsAccount openSavingsAccount(Customer owner, BigDecimal dailyWithdrawLimit, BigDecimal interestRate, BigDecimal maximumBalance) {
        String accountNumber = String.valueOf(nextAccountNumber.getAndIncrement());
        SavingsAccount savingsAccount = new SavingsAccount(accountNumber, owner, dailyWithdrawLimit, interestRate, maximumBalance);
        accounts.put(accountNumber, savingsAccount);
        owner.addAccount(accountNumber);
        return savingsAccount;
    }

    public CheckingAccount openCheckingAccount(Customer owner, BigDecimal dailyWithdrawLimit, BigDecimal overdraftLimit) {
        String accountNumber = String.valueOf(nextAccountNumber.getAndIncrement());
        CheckingAccount checkingAccount = new CheckingAccount(accountNumber, owner, dailyWithdrawLimit, overdraftLimit);
        accounts.put(accountNumber, checkingAccount);
        owner.addAccount(accountNumber);
        return checkingAccount;
    }

    public void closeAccount(String accountNumber) {
        BankAccount account = accounts.get(accountNumber);
        if (account == null) {
            throw new UnknownAccountException("Account not found: " + accountNumber);
        }
        accounts.remove(accountNumber);
        account.getOwner().removeAccount(accountNumber);
    }

    public BankAccount getAccount(String accountNumber){
        BankAccount account = accounts.get(accountNumber);
        if (account == null) {
            throw new UnknownAccountException("Account not found: " + accountNumber);
        }
        return account;
    }

    public List<BankAccount> getAllAccounts(){
        return List.copyOf(accounts.values());
    }

    public void deposit(String accountNumber, BigDecimal amount){
        getAccount(accountNumber).deposit(amount);
    }

    public void withdraw(String accountNumber, BigDecimal amount){
        BankAccount account = getAccount(accountNumber);
        account.withdraw(amount);

        if(withdrawalFee.compareTo(BigDecimal.ZERO) > 0){
            account.chargeFee(withdrawalFee);
        }

        if (account instanceof CheckingAccount checkingAccount
                && overdraftFee.compareTo(BigDecimal.ZERO) > 0
                && checkingAccount.chargeOverdraftFee()) {
            checkingAccount.chargeFee(overdraftFee);
        }
    }

    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount){
        BankAccount sender = getAccount(fromAccountNumber);
        sender.transferTo(getAccount(toAccountNumber), amount);
        if (sender instanceof CheckingAccount checkingAccount
                && overdraftFee.compareTo(BigDecimal.ZERO) > 0
                && checkingAccount.chargeOverdraftFee()) {
            checkingAccount.chargeFee(overdraftFee);
        }
    }

    public int getAccountCount() {
        return accounts.size();
    }

    public Statement generateStatement(String accountNumber, LocalDate from, LocalDate to){
        BankAccount account = getAccount(accountNumber);
        List<Transaction> transactionHistory = account.getTransactionHistory();

        List<Transaction> filtered = transactionHistory.stream()
                .filter(transaction -> {
                    LocalDate transactionDate = transaction.getTimestamp().toLocalDate();
                    return !transactionDate.isBefore(from) && !transactionDate.isAfter(to);
                })
                .toList();

        BigDecimal startBalance = transactionHistory.stream()
                .filter(transaction -> transaction.getTimestamp().toLocalDate().isBefore(from))
                .reduce((first, second) -> second)
                .map(Transaction::getResBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal endBalance = filtered.isEmpty() ? startBalance : filtered.get(filtered.size() - 1).getResBalance();

        return new Statement(
                accountNumber,
                from,
                to,
                startBalance,
                endBalance,
                filtered
        );
    }

}
