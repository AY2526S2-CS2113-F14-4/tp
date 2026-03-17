package seedu.duke.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.Wallet;
import seedu.duke.model.WalletManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SendCommandTest {
    private static final String ETH_ADDRESS = "0x1111111111111111111111111111111111111111";

    @Test
    void execute_validSendWithDefaultSpeed_recordsTransactionAndHistory() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("bob"); // bob has balance 10 from default blockchain
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/4", walletManager);

        String output = runCommand(command, blockchain);

        String expected = "Transaction sent successfully." + System.lineSeparator()
                + "Wallet: bob" + System.lineSeparator()
                + "To: " + ETH_ADDRESS + System.lineSeparator()
                + "Amount: 4" + System.lineSeparator()
                + "Speed: standard" + System.lineSeparator()
                + "Fee: 0.0010" + System.lineSeparator();
        assertEquals(expected, output);

        assertEquals(3, blockchain.size());
        assertEquals(new BigDecimal("0.999"), blockchain.getPreciseBalance("bob"));
        Wallet wallet = walletManager.findWallet("bob").orElse(null);
        assertNotNull(wallet);
        assertEquals(1, wallet.getTransactionHistory().size());
        assertTrue(wallet.getTransactionHistory().get(0).contains("speed/standard"));
    }

    @Test
    void execute_insufficientBalance_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("alice"); // alice has balance -10
        SendCommand command = new SendCommand("w/alice to/" + ETH_ADDRESS + " amt/1", walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute(blockchain));
        assertEquals("Error: Insufficient balance.", exception.getMessage());
    }

    @Test
    void execute_walletNotFound_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        SendCommand command = new SendCommand("w/nonexistent to/" + ETH_ADDRESS + " amt/1", walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute(blockchain));
        assertEquals("Error: Wallet not found.", exception.getMessage());
    }

    @Test
    void execute_invalidAmount_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("bob");
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/-5", walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute(blockchain));
        assertEquals("Error: Amount must be a positive number.", exception.getMessage());
    }

    @Test
    void execute_invalidFormat_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        SendCommand command = new SendCommand("invalid", walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute(blockchain));
        assertTrue(exception.getMessage().startsWith("Error: Invalid send format."));
    }

    @Test
    void execute_manualFeeOverride_usesProvidedFee() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("bob");
        SendCommand command = new SendCommand(
                "w/bob to/" + ETH_ADDRESS + " amt/4 speed/fast fee/0.5 note/priority transfer",
                walletManager);

        String output = runCommand(command, blockchain);

        assertTrue(output.contains("Speed: manual"));
        assertTrue(output.contains("Fee: 0.5"));
        assertEquals(new BigDecimal("0.5"), blockchain.getPreciseBalance("network-fee"));
        Wallet wallet = walletManager.findWallet("bob").orElse(null);
        assertNotNull(wallet);
        assertTrue(wallet.getTransactionHistory().get(0).contains("note/priority transfer"));
    }

    @Test
    void execute_invalidAddress_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("bob");
        SendCommand command = new SendCommand("w/bob to/not-an-address amt/1", walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute(blockchain));
        assertEquals("Error: Invalid recipient address.", exception.getMessage());
    }

    @Test
    void execute_unsupportedSpeed_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("bob");
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/1 speed/urgent", walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute(blockchain));
        assertEquals("Error: Unsupported speed. Use slow, standard, or fast.", exception.getMessage());
    }

    @Test
    void execute_negativeManualFee_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("bob");
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/1 fee/-0.1", walletManager);

        Exceptions exception = assertThrows(Exceptions.class, () -> command.execute(blockchain));
        assertEquals("Error: Fee must be a non-negative number.", exception.getMessage());
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


