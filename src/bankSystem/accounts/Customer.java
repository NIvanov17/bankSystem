package bankSystem.accounts;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Customer {
    private final String id;
    private final String name;
    private final List<String> accountNumbers = new ArrayList<>();

    public Customer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public synchronized void addAccount(String accountNumber) {
        accountNumbers.add(accountNumber);
    }

    public synchronized void removeAccount(String accountNumber) {
        accountNumbers.remove(accountNumber);
    }

    public synchronized List<String> getAccountNumbersCopy() {
        return List.copyOf(accountNumbers);
    }

    @Override
    public String toString() {
        return "Customer id='%s', name='%s'".formatted(id, name);
    }
}
