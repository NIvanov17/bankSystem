import bankSystem.accounts.CheckingAccount;
import bankSystem.accounts.Customer;
import bankSystem.accounts.SavingsAccount;
import bankSystem.bank.Bank;
import bankSystem.enums.InterestCalculationMode;
import bankSystem.exceptions.OverdraftLimitExceededException;
import bankSystem.exceptions.UnknownAccountException;
import bankSystem.transaction.Statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Bank bank = createBank();
        DemoData data = createDemoData(bank);

        printOpenedAccounts(bank);
        runBasicOperations(bank, data);
        printStatement(bank, data.aliceChecking.getAccountNumber());
        runConcurrentTest(bank, data);
        printCustomerLinks(data);
        testCloseAccount(bank, data.bobChecking.getAccountNumber());
        printTransactionHistories(bank);
    }

    private static Bank createBank() {
        Bank bank = new Bank("DemoBank");
        bank.setWithdrawalFee(new BigDecimal("0.50"));
        bank.setOverdraftFee(new BigDecimal("25.00"));
        return bank;
    }

    private static DemoData createDemoData(Bank bank) {
        Customer alice = bank.registerCustomer("Alice");
        Customer bob = bank.registerCustomer("Bob");
        Customer charlie = bank.registerCustomer("Charlie");

        CheckingAccount aliceChecking = bank.openCheckingAccount(
                alice,
                new BigDecimal("1000"),
                new BigDecimal("200")
        );

        CheckingAccount bobChecking = bank.openCheckingAccount(
                bob,
                new BigDecimal("1000"),
                new BigDecimal("100")
        );

        SavingsAccount charlieSavings = bank.openSavingsAccount(
                charlie,
                new BigDecimal("500"),
                new BigDecimal("0.12"),
                new BigDecimal("50000")
        );

        return new DemoData(alice, bob, charlie, aliceChecking, bobChecking, charlieSavings);
    }

    private static void printOpenedAccounts(Bank bank) {
        System.out.println("=== OPENED ACCOUNTS ===");
        bank.getAllAccounts().forEach(a ->
                System.out.println(a.getAccountNumber() + " | " +
                        a.getOwner() + " | " +
                        a.getClass().getSimpleName())
        );
    }

    private static void runBasicOperations(Bank bank, DemoData data) {
        System.out.println("\n=== BASIC OPERATIONS ===");

        bank.deposit(data.aliceChecking.getAccountNumber(), new BigDecimal("500"));
        System.out.println("Alice after deposit 500: " + data.aliceChecking.getBalanceSafely());

        bank.withdraw(data.aliceChecking.getAccountNumber(), new BigDecimal("600"));
        System.out.println("Alice after withdraw 600: " + data.aliceChecking.getBalanceSafely());

        try {
            bank.withdraw(data.aliceChecking.getAccountNumber(), new BigDecimal("200"));
        } catch (OverdraftLimitExceededException e) {
            System.out.println("Expected overdraft exception: " + e.getMessage());
        }

        bank.deposit(data.bobChecking.getAccountNumber(), new BigDecimal("100"));
        bank.deposit(data.aliceChecking.getAccountNumber(), new BigDecimal("300"));
        bank.transfer(data.aliceChecking.getAccountNumber(), data.bobChecking.getAccountNumber(), new BigDecimal("50"));

        System.out.println("Alice after transfer: " + data.aliceChecking.getBalanceSafely());
        System.out.println("Bob after transfer:   " + data.bobChecking.getBalanceSafely());

        bank.deposit(data.charlieSavings.getAccountNumber(), new BigDecimal("1000"));
        data.charlieSavings.applyInterestRate(InterestCalculationMode.MONTHLY);
        System.out.println("Charlie after monthly interest: " + data.charlieSavings.getBalanceSafely());

        data.charlieSavings.applyInterestRate(InterestCalculationMode.YEARLY);
        System.out.println("Charlie after yearly interest:  " + data.charlieSavings.getBalanceSafely());
    }

    private static void printStatement(Bank bank, String accountNumber) {
        System.out.println("\n=== STATEMENT ===");

        Statement statement = bank.generateStatement(
                accountNumber,
                LocalDate.now().minusDays(1),
                LocalDate.now()
        );

        System.out.println("Statement for account " + statement.getAccountNumber());
        System.out.println("From: " + statement.getFrom());
        System.out.println("To: " + statement.getTo());
        System.out.println("Starting balance: " + statement.getStartingBalance());
        System.out.println("Ending balance:   " + statement.getEndingBalance());
        System.out.println("Transactions count: " + statement.getTransactions().size());
    }

    private static void runConcurrentTest(Bank bank, DemoData data) throws InterruptedException {
        System.out.println("\n=== CONCURRENT TEST ===");

        bank.deposit(data.aliceChecking.getAccountNumber(), new BigDecimal("5000"));
        bank.deposit(data.bobChecking.getAccountNumber(), new BigDecimal("5000"));

        List<Thread> threads = new ArrayList<>();

        Runnable depositsToAlice = () -> {
            for (int i = 0; i < 100; i++) {
                bank.deposit(data.aliceChecking.getAccountNumber(), BigDecimal.ONE);
            }
        };

        Runnable transfersAliceToBob = () -> {
            for (int i = 0; i < 50; i++) {
                try {
                    bank.transfer(data.aliceChecking.getAccountNumber(), data.bobChecking.getAccountNumber(), BigDecimal.ONE);
                } catch (Exception e) {
                    System.out.println("Transfer A->B failed: " + e.getMessage());
                }
            }
        };

        Runnable transfersBobToAlice = () -> {
            for (int i = 0; i < 50; i++) {
                try {
                    bank.transfer(data.bobChecking.getAccountNumber(), data.aliceChecking.getAccountNumber(), BigDecimal.ONE);
                } catch (Exception e) {
                    System.out.println("Transfer B->A failed: " + e.getMessage());
                }
            }
        };

        Runnable withdrawalsFromBob = () -> {
            for (int i = 0; i < 40; i++) {
                try {
                    bank.withdraw(data.bobChecking.getAccountNumber(), BigDecimal.ONE);
                } catch (Exception e) {
                    System.out.println("Withdraw from Bob failed: " + e.getMessage());
                }
            }
        };

        for (int i = 0; i < 5; i++) {
            threads.add(new Thread(depositsToAlice));
            threads.add(new Thread(transfersAliceToBob));
            threads.add(new Thread(transfersBobToAlice));
            threads.add(new Thread(withdrawalsFromBob));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("Alice final balance:   " + data.aliceChecking.getBalanceSafely());
        System.out.println("Bob final balance:     " + data.bobChecking.getBalanceSafely());
        System.out.println("Charlie final balance: " + data.charlieSavings.getBalanceSafely());
    }

    private static void printCustomerLinks(DemoData data) {
        System.out.println("\n=== CUSTOMER LINKS ===");
        System.out.println(data.alice + " -> " + data.alice.getAccountNumbersCopy());
        System.out.println(data.bob + " -> " + data.bob.getAccountNumbersCopy());
        System.out.println(data.charlie + " -> " + data.charlie.getAccountNumbersCopy());
    }

    private static void testCloseAccount(Bank bank, String accountNumber) {
        System.out.println("\n=== CLOSE ACCOUNT ===");
        bank.closeAccount(accountNumber);
        System.out.println("Accounts remaining: " + bank.getAccountCount());

        try {
            bank.getAccount(accountNumber);
        } catch (UnknownAccountException e) {
            System.out.println("Expected unknown account exception: " + e.getMessage());
        }
    }

    private static void printTransactionHistories(Bank bank) {
        System.out.println("\n=== TRANSACTION HISTORIES ===");
        bank.getAllAccounts().forEach(a -> {
            System.out.println(a.getOwner() + " (" + a.getAccountNumber() + ")");
            a.getTransactionHistory().forEach(t -> System.out.println("  " + t));
        });
    }

    private static class DemoData {
        private final Customer alice;
        private final Customer bob;
        private final Customer charlie;
        private final CheckingAccount aliceChecking;
        private final CheckingAccount bobChecking;
        private final SavingsAccount charlieSavings;

        private DemoData(Customer alice,
                         Customer bob,
                         Customer charlie,
                         CheckingAccount aliceChecking,
                         CheckingAccount bobChecking,
                         SavingsAccount charlieSavings) {
            this.alice = alice;
            this.bob = bob;
            this.charlie = charlie;
            this.aliceChecking = aliceChecking;
            this.bobChecking = bobChecking;
            this.charlieSavings = charlieSavings;
        }
    }
}