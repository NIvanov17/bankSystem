package bankSystem.exceptions;

public class ExceededLimitException extends RuntimeException{
    public ExceededLimitException(String message) {
        super(message);
    }
}
