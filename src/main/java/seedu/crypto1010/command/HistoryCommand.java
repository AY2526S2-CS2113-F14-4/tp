package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.Wallet;
import seedu.crypto1010.model.WalletManager;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class HistoryCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: history w/WALLET_NAME
            Example: history w/BobWallet

            Displays the recorded send history of the wallet
            Shows outgoing transactions in chronological order
            """;
    private static final String NAME_ERROR = "Error: wallet name cannot be empty.";
    private static final String NAME_WHITESPACE_ERROR = "Error: wallet name must be one word without spaces.";
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid history format. Use: history w/WALLET_NAME";
    private static final String WALLET_NOT_FOUND_ERROR = "Error: Wallet not found.";
    private static final String HISTORY_FORMAT = "Use: history w/WALLET_NAME";

    private final String arguments;
    private final WalletManager walletManager;

    public HistoryCommand(String arguments, WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
        this.walletManager = Objects.requireNonNull(walletManager);
    }

    @Override
    public void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception {
        String walletName = parseArguments(arguments);
        Wallet wallet = walletManager.findWallet(walletName)
                .orElseThrow(() -> new Crypto1010Exception(WALLET_NOT_FOUND_ERROR));

        List<String> transactionHistory = wallet.getTransactionHistory();
        if (transactionHistory.isEmpty()) {
            Ui.println("No transaction history found for " + wallet.getName() + ".");
            return;
        }

        Ui.println("Transaction history for " + wallet.getName() + ":");
        for (int i = 0; i < transactionHistory.size(); i++) {
            Ui.println((i + 1) + ". " + transactionHistory.get(i));
        }
    }

    private String parseArguments(String args) throws Crypto1010Exception {
        if (args == null || args.isBlank()) {
            throw new Crypto1010Exception(NAME_ERROR + " " + HISTORY_FORMAT);
        }

        String trimmedArgs = args.trim();
        if (!trimmedArgs.startsWith("w/")) {
            throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
        }

        String walletName = trimmedArgs.substring(2).trim();
        if (walletName.isEmpty()) {
            throw new Crypto1010Exception(NAME_ERROR + " " + HISTORY_FORMAT);
        }
        if (walletName.chars().anyMatch(Character::isWhitespace)) {
            throw new Crypto1010Exception(NAME_WHITESPACE_ERROR + " " + HISTORY_FORMAT);
        }

        return walletName;
    }
}
