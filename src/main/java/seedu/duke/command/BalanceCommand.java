package seedu.duke.command;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BalanceCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: balance NAME
            Displays the balance of wallet up to 8 decimal points
            """;
    private static final String NAME_ERROR = "Error: wallet name cannot be empty.";

    private final String walletName;

    public BalanceCommand(String walletName) {
        super(HELP_DESCRIPTION);
        this.walletName = walletName;
    }

    @Override
    public void execute(String description, Blockchain blockchain) throws Exceptions {
        if (walletName == null || walletName.isBlank()) {
            System.out.println(NAME_ERROR);
            return;
        }

        String trimmedWalletName = walletName.trim();
        BigDecimal balance = blockchain.getPreciseBalance(trimmedWalletName);

        System.out.println("Balance of " + trimmedWalletName + ": " + formatBalance(balance));
    }

    private String formatBalance(BigDecimal balance) {
        return balance.setScale(8, RoundingMode.HALF_UP).toPlainString();
    }
}
