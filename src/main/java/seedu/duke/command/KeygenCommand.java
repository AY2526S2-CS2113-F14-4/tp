package seedu.duke.command;

import java.util.List;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.Key;
import seedu.duke.model.Wallet;
import seedu.duke.model.WalletManager;

public class KeygenCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            format: keygen w/NAME
            Generates and displays key pair for new wallet, or regenerates for existing wallet
            Displays the process of creating a key pair
            """;
    private static final String INVALID_WALLET_NUMBER_ERROR = "Error: Invalid number of args";
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid send format. Use: keygen w/WALLET";
    private static final String WALLET_NOT_FOUND_ERROR = "Error: Wallet not found";
    private static final String KEY_PAIR_GENERATION_SUCCESSFUL = "Key pair successfully generated";

    private final String arguments;
    private final WalletManager walletManager;

    public KeygenCommand(String arguments, WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
        this.walletManager = walletManager;
    }

    @Override
    public void execute(String description, Blockchain blockchain) throws Exceptions {
        // Parse wallet name
        String walletName = parseArguments(arguments);

        // Validate wallet exists
        if (!walletManager.hasWallet(walletName)) {
            throw new Exceptions(WALLET_NOT_FOUND_ERROR);
        }

        List<Wallet> wallets = walletManager.getWallets();
        for (Wallet wallet : wallets) {
            if (wallet.getName().equals(walletName)) {
                wallet.setKeys(Key.generateKeyPair());
                System.out.println(KEY_PAIR_GENERATION_SUCCESSFUL);
            }
        }
    }

    private String parseArguments(String args) throws Exceptions{
        String[] parts = args.split("\\s+");
        if (parts.length != 1) {
            throw new Exceptions(INVALID_WALLET_NUMBER_ERROR);
        } else if (parts[0].startsWith("w/")) {
            return parts[0].substring(2);
        } else {
            throw new Exceptions(INVALID_FORMAT_ERROR);
        }
    }
}
