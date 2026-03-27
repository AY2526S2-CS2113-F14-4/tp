package seedu.duke.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.Wallet;
import seedu.duke.model.WalletManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

class ListCommandTest {
    @Test
    void execute_noWallets_printsEmptyMessage() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        ListCommand command = new ListCommand(walletManager);

        String output = runCommand(command, blockchain);

        assertEquals("No wallets found." + System.lineSeparator(), output);
    }

    @Test
    void execute_existingWallets_printsWalletNames() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet alice = walletManager.createWallet("alice");
        Wallet bob = walletManager.createWallet("bob");
        ListCommand command = new ListCommand(walletManager);

        String output = runCommand(command, blockchain);

        String expected = String.join(System.lineSeparator(),
                "Wallets:",
            "1. alice | Address: " + alice.getAddress(),
            "2. bob | Address: " + bob.getAddress()) + System.lineSeparator();
        assertEquals(expected, output);
    }

    @Test
    void execute_withUnexpectedArguments_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        ListCommand command = new ListCommand(walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute("extra", blockchain));

        assertEquals("Error: Invalid list format. Use: list", exception.getMessage());
    }

    @Test
    void constructor_nullWalletManager_throwsException() {
        assertThrows(NullPointerException.class, () -> new ListCommand(null));
    }

    private String runCommand(Command command, Blockchain blockchain) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        try {
            command.execute(blockchain);
        } catch (Exceptions e) {
            throw new RuntimeException(e);
        } finally {
            System.setOut(originalOut);
        }
        return outputStream.toString();
    }
}
