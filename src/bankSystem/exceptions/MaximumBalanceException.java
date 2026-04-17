package bankSystem.exceptions;

public class MaximumBalanceException extends RuntimeException {
    public MaximumBalanceException(String message) {
        super(message);
    }
}
