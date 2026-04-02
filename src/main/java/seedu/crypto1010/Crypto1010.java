package seedu.crypto1010;

import seedu.crypto1010.auth.AuthenticationException;
import seedu.crypto1010.auth.AuthenticationService;
import seedu.crypto1010.command.Command;
import seedu.crypto1010.command.ExitCommand;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.WalletManager;
import seedu.crypto1010.storage.AccountStorage;
import seedu.crypto1010.storage.BlockchainStorage;
import seedu.crypto1010.storage.WalletStorage;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Crypto1010 {
    private static final Logger LOGGER = Logger.getLogger(Crypto1010.class.getName());
    private static final String DIVIDER =
            "============================================================";
    private static final String ACCOUNT_ACCESS_HEADER = "Crypto1010 Account Access";
    private static final String ACCOUNT_SELECTION_ERROR =
            "Error: Invalid selection. Choose login, register, or exit.";

    /**
     * Main entry-point for the java.crypto1010.Crypto1010 application.
     */
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        AuthenticationService authenticationService = loadAuthenticationService();
        String accountUsername = authenticateUser(in, authenticationService);
        if (accountUsername == null) {
            return;
        }

        printWelcome(accountUsername);
        BlockchainStorage blockchainStorage = new BlockchainStorage(Crypto1010.class, accountUsername);
        WalletStorage walletStorage = new WalletStorage(Crypto1010.class, accountUsername);
        LoadResult<Blockchain> blockchainLoadResult = loadBlockchain(blockchainStorage);
        LoadResult<WalletManager> walletLoadResult = loadWalletManager(walletStorage);
        Blockchain blockchain = blockchainLoadResult.data();
        WalletManager walletManager = walletLoadResult.data();
        boolean allowBlockchainSave = blockchainLoadResult.loadedSuccessfully();
        boolean allowWalletSave = walletLoadResult.loadedSuccessfully();
        if (!allowBlockchainSave) {
            Ui.println("Blockchain save is disabled to avoid overwriting existing data after load failure.");
        }
        if (!allowWalletSave) {
            Ui.println("Wallet save is disabled to avoid overwriting existing data after load failure.");
        }
        Parser parser = new Parser(walletManager, accountUsername, Crypto1010.class);

        while (true) {
            String message;
            try {
                message = in.nextLine().strip();
            } catch (NoSuchElementException e) {
                saveData(
                        blockchainStorage,
                        walletStorage,
                        blockchain,
                        walletManager,
                        allowBlockchainSave,
                        allowWalletSave);
                break;
            }
            try {
                Command c;
                try {
                    c = parser.parse(message);
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.FINE, "Command parse failed for input: " + message, e);
                    Ui.println("Error: Invalid command. Use: help");
                    continue;
                }
                long startNs = System.nanoTime();
                if (c instanceof ExitCommand) {
                    c.execute(blockchain, in);
                    long durationMs = (System.nanoTime() - startNs) / 1_000_000;
                    LOGGER.fine(() -> "Command executed successfully: exit (" + durationMs + " ms)");
                    saveData(
                            blockchainStorage,
                            walletStorage,
                            blockchain,
                            walletManager,
                            allowBlockchainSave,
                            allowWalletSave);
                    break;
                }
                c.execute(blockchain, in);
                long durationMs = (System.nanoTime() - startNs) / 1_000_000;
                String commandName = c.getClass().getSimpleName();
                LOGGER.fine(() -> "Command executed successfully: " + commandName + " (" + durationMs + " ms)");
                saveData(
                        blockchainStorage,
                        walletStorage,
                        blockchain,
                        walletManager,
                        allowBlockchainSave,
                        allowWalletSave);
            } catch (Crypto1010Exception e) {
                LOGGER.log(Level.WARNING, "Command execution failed.", e);
                Ui.println(e.getMessage());
            }
        }
    }

    private static AuthenticationService loadAuthenticationService() {
        AuthenticationService authenticationService = new AuthenticationService(new AccountStorage(Crypto1010.class));
        try {
            authenticationService.load();
        } catch (IOException e) {
            Ui.println("Failed to load account data. Starting with no registered accounts.");
        }
        return authenticationService;
    }

    private static String authenticateUser(Scanner in, AuthenticationService authenticationService) {
        while (true) {
            printAuthenticationMenu(authenticationService);
            String choice = promptForTrimmedInput(in, "Choice:");
            if (choice == null) {
                return null;
            }

            switch (choice.toLowerCase()) {
            case "1":
            case "login":
                String loggedInUsername = handleLogin(in, authenticationService);
                if (loggedInUsername != null) {
                    return loggedInUsername;
                }
                break;
            case "2":
            case "register":
                String registeredUsername = handleRegistration(in, authenticationService);
                if (registeredUsername != null) {
                    return registeredUsername;
                }
                break;
            case "3":
            case "exit":
                Ui.println("Exiting Crypto1010.");
                return null;
            default:
                Ui.println(ACCOUNT_SELECTION_ERROR);
            }
        }
    }

    private static void printAuthenticationMenu(AuthenticationService authenticationService) {
        Ui.println(DIVIDER);
        Ui.println(ACCOUNT_ACCESS_HEADER);
        if (!authenticationService.hasRegisteredAccounts()) {
            Ui.println("No registered accounts found. Register to get started.");
        }
        Ui.println("1. login");
        Ui.println("2. register");
        Ui.println("3. exit");
        Ui.println(DIVIDER);
    }

    private static String handleLogin(Scanner in, AuthenticationService authenticationService) {
        if (!authenticationService.hasRegisteredAccounts()) {
            Ui.println("Error: No accounts registered yet. Choose register first.");
            return null;
        }

        String username = promptForTrimmedInput(in, "Username:");
        String password = promptForTrimmedInput(in, "Password:");
        if (username == null || password == null) {
            return null;
        }

        try {
            String authenticatedUsername = authenticationService.authenticate(username, password);
            Ui.println("Login successful. Logged in as " + authenticatedUsername + ".");
            return authenticatedUsername;
        } catch (AuthenticationException e) {
            Ui.println(e.getMessage());
            return null;
        }
    }

    private static String handleRegistration(Scanner in, AuthenticationService authenticationService) {
        String username = promptForTrimmedInput(in, "Choose username:");
        String password = promptForTrimmedInput(in, "Choose password:");
        String passwordConfirmation = promptForTrimmedInput(in, "Confirm password:");
        if (username == null || password == null || passwordConfirmation == null) {
            return null;
        }

        try {
            String registeredUsername = authenticationService.register(username, password, passwordConfirmation);
            Ui.println("Registration successful. Logged in as " + registeredUsername + ".");
            return registeredUsername;
        } catch (AuthenticationException | IOException e) {
            Ui.println(e.getMessage());
            return null;
        }
    }

    private static String promptForTrimmedInput(Scanner in, String prompt) {
        Ui.println(prompt);
        try {
            return in.nextLine().strip();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private static void printWelcome(String accountUsername) {
        Ui.println(DIVIDER);
        Ui.println("Welcome to Crypto1010");
        Ui.println("Logged in as: " + accountUsername);
        Ui.println("Manage wallets, send transactions, and inspect your blockchain quickly.");
        Ui.println("Try: create w/MainWallet | list | help");
        Ui.println(DIVIDER);
    }

    private static LoadResult<Blockchain> loadBlockchain(BlockchainStorage storage) {
        try {
            return new LoadResult<>(storage.load(), true);
        } catch (IOException e) {
            Ui.println("Failed to load blockchain data. Starting with default blockchain.");
            return new LoadResult<>(Blockchain.createDefault(), false);
        }
    }

    private static LoadResult<WalletManager> loadWalletManager(WalletStorage storage) {
        try {
            return new LoadResult<>(storage.load(), true);
        } catch (IOException e) {
            Ui.println("Failed to load wallet data. Starting with empty wallet list.");
            return new LoadResult<>(new WalletManager(), false);
        }
    }

    private static void saveData(
            BlockchainStorage blockchainStorage,
            WalletStorage walletStorage,
            Blockchain blockchain,
            WalletManager walletManager,
            boolean allowBlockchainSave,
            boolean allowWalletSave) {
        if (allowBlockchainSave) {
            try {
                blockchainStorage.save(blockchain);
            } catch (IOException e) {
                Ui.println("Failed to save blockchain data.");
            }
        }
        if (allowWalletSave) {
            try {
                walletStorage.save(walletManager);
            } catch (IOException e) {
                Ui.println("Failed to save wallet data.");
            }
        }
    }

    private record LoadResult<T>(T data, boolean loadedSuccessfully) {
    }
}
