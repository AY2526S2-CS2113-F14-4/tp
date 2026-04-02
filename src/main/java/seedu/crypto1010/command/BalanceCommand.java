package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.WalletManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

public class BalanceCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: balance w/WALLET_NAME
            Example: balance w/BobWallet
            
            Displays the balance of wallet up to 8 decimal points
            """;
    private static final String NAME_ERROR = "Error: wallet name cannot be empty.";
    private static final String NAME_WHITESPACE_ERROR = "Error: wallet name must be one word without spaces.";
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid balance format. Use: balance w/WALLET_NAME";
    private static final String WALLET_NOT_FOUND_ERROR = "Error: Wallet not found.";
    private static final String BALANCE_FORMAT = "Use: balance w/WALLET_NAME";

    private final String arguments;
    private final WalletManager walletManager;

    public BalanceCommand(String arguments, WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
        this.walletManager = walletManager;
    }

    @Override
    public void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception {
        String walletName = parseArguments(arguments);
        String trimmedWalletName = walletName.trim();
        if (!walletManager.hasWallet(trimmedWalletName)) {
            throw new Crypto1010Exception(WALLET_NOT_FOUND_ERROR);
        }
        BigDecimal balance = blockchain.getPreciseBalance(trimmedWalletName);

        Ui.println("Balance of " + trimmedWalletName + ": " + formatBalance(balance));
    }

    private String parseArguments(String args) throws Crypto1010Exception {
        return CommandParserUtil.parseRequiredWalletNameArgument(
                args,
                INVALID_FORMAT_ERROR,
                NAME_ERROR,
                NAME_WHITESPACE_ERROR,
                BALANCE_FORMAT);
    }

    private String formatBalance(BigDecimal balance) {
        return balance.setScale(8, RoundingMode.HALF_UP).toPlainString();
    }

}
