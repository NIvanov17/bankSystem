package bankSystem.transaction;

import bankSystem.enums.TransactionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@ToString
public class Transaction {
    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String fromAccount;
    private final String toAccount;
    private final BigDecimal resBalance;
}
