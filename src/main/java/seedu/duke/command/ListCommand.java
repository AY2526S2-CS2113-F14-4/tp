package seedu.duke.command;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.Wallet;
import seedu.duke.model.WalletManager;

import java.util.List;
import java.util.Objects;

public class ListCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: list
            
            Lists all the available wallets
            """;
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid list format. Use: list";
    private static final String INVALID_WALLET_DATA_ERROR = "Error: Wallet data is corrupted.";
    private static final String NO_WALLETS_MESSAGE = "No wallets found.";

    private final WalletManager walletManager;
  
    public ListCommand(WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.walletManager = Objects.requireNonNull(walletManager);
    }

    @Override
    public void execute(String description, Blockchain blockchain) throws Exceptions {
        validateArguments(description);
        List<Wallet> wallets = walletManager.getWallets();
        if (wallets.isEmpty()) {
            System.out.println(NO_WALLETS_MESSAGE);
            return;
        }

        System.out.println("Wallets:");
        for (int i = 0; i < wallets.size(); i++) {
            Wallet wallet = wallets.get(i);
            if (wallet == null || wallet.getName() == null || wallet.getName().isBlank()) {
                throw new Exceptions(INVALID_WALLET_DATA_ERROR);
            }
            System.out.println((i + 1) + ". " + wallet.getName());
        }
    }

    private void validateArguments(String description) throws Exceptions {
        if (description != null && !description.isBlank()) {
            throw new Exceptions(INVALID_FORMAT_ERROR);
        }
    }
}
