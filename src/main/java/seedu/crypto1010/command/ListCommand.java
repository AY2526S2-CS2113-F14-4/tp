package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.CurrencyCode;
import seedu.crypto1010.model.Wallet;
import seedu.crypto1010.model.WalletManager;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class ListCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: list
            
            Lists all the available wallets
            """;
  
    private static final String NO_WALLETS_MESSAGE = "No wallets found.";
    private static final String INVALID_WALLET_DATA_ERROR = "Error: Wallet data is corrupted.";

    private final WalletManager walletManager;
  
    public ListCommand(WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.walletManager = Objects.requireNonNull(walletManager);
    }

    @Override
    public void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception {

        List<Wallet> wallets = walletManager.getWallets();
        if (wallets.isEmpty()) {
            Ui.println(NO_WALLETS_MESSAGE);
            return;
        }

        Ui.println("Wallets:");
        for (int i = 0; i < wallets.size(); i++) {
            Wallet wallet = wallets.get(i);
            String walletName = validateWalletName(wallet);
            Ui.print((i + 1) + ". " + walletName);
            if (!CurrencyCode.isGeneric(wallet.getCurrencyCode())) {
                Ui.print(" | Currency: " + wallet.getCurrencyCode());
            }
            Ui.print(" | Address: ");
            try {
                Ui.println(wallet.getAddress());
            } catch (Crypto1010Exception e) {
                Ui.println(e.getMessage());
            }
        }
    }

    private String validateWalletName(Wallet wallet) throws Crypto1010Exception {
        if (wallet == null || wallet.getName() == null || wallet.getName().isBlank()) {
            throw new Crypto1010Exception(INVALID_WALLET_DATA_ERROR);
        }
        return wallet.getName();
    }
}
