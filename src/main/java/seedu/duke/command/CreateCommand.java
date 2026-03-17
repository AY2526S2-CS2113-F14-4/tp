package seedu.duke.command;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.Wallet;
import seedu.duke.model.WalletManager;

public class CreateCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            format: create NAME
            Creates a wallet called NAME
            """;
  
    private static final String NAME_ERROR = "Error: wallet name cannot be empty.";
    private static final String DUPLICATE_ERROR = "Error: wallet name already exists.";

    private final String walletName;
    private final WalletManager walletManager;

    public CreateCommand(String walletName, WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.walletName = walletName;
        this.walletManager = walletManager;
    }

    @Override
    public void execute(String description, Blockchain blockchain) throws Exceptions {
        if (walletName == null || walletName.isBlank()) {
            throw new Exceptions(NAME_ERROR);
        }

        String trimmedWalletName = walletName.trim();
        if (walletManager.hasWallet(trimmedWalletName)) {
            throw new Exceptions(DUPLICATE_ERROR);
        }

        Wallet wallet = walletManager.createWallet(trimmedWalletName);
        System.out.println("Wallet created: " + wallet.getName());
    }
}
