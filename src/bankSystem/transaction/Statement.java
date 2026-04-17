package bankSystem.transaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Statement {
    private final String accountNumber;
    private final LocalDate from;
    private final LocalDate to;
    private final BigDecimal startingBalance;
    private final BigDecimal endingBalance;
    private final List<Transaction> transactions;
}
