package seedu.duke;

import seedu.duke.command.Command;
import seedu.duke.command.ExitCommand;
import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.WalletManager;
import seedu.duke.storage.BlockchainStorage;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Duke {
    /**
     * Main entry-point for the java.duke.Duke application.
     */
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        BlockchainStorage storage = new BlockchainStorage(Duke.class);
        Blockchain blockchain = loadBlockchain(storage);
        WalletManager walletManager = new WalletManager();
        Parser parser = new Parser(walletManager);

        while (true) {
            String message = in.nextLine().strip();
            try {
                Command c = parser.parse(message);
                String[] components = message.split("\\s+", 2);
                String description = components.length > 1 ? components[1] : "";
                if (c instanceof ExitCommand) {
                    saveBlockchain(storage, blockchain);
                    break;
                }
                c.execute(description, blockchain);
                saveBlockchain(storage, blockchain);
            } catch (Exceptions e) {
                System.out.println(e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid Command");
            } catch (NoSuchElementException e) {
                System.out.println("No Input");
            }
        }
    }

    private static Blockchain loadBlockchain(BlockchainStorage storage) {
        try {
            return storage.load();
        } catch (IOException e) {
            System.out.println("Failed to load blockchain data. Starting with default blockchain.");
            return Blockchain.createDefault();
        }
    }

    private static void saveBlockchain(BlockchainStorage storage, Blockchain blockchain) {
        try {
            storage.save(blockchain);
        } catch (IOException e) {
            System.out.println("Failed to save blockchain data.");
        }
    }
}
