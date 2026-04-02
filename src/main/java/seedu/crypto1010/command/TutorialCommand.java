package seedu.crypto1010.command;

import seedu.crypto1010.Parser;
import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.WalletManager;

import java.util.Scanner;

public class TutorialCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: tutorial start
            Example: tutorial start
            
            Starts the tutorial which guides the user through using the simple commands
            """;

    private static final String ERROR_MESSAGE = "Please input the given command to continue\n" +
            "If you want to exit tutorial mode, type: tutorial exit";
    private static final String EXIT_MESSAGE = "Exiting tutorial...";
    private static final String WELCOME_MESSAGE = "Welcome to the tutorial!";
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid tutorial format. Use tutorial start";

    private static final String[] instructions = {
        "create w/alice",
        "create w/bob",
        "list",
        "keygen w/alice",
        "keygen w/bob",
        "list",
        "balance w/alice",
        "balance w/bob",
        "help c/send",
        "send w/bob amt/3 to/{alice's wallet address}",
        "balance w/alice",
        "balance w/bob",
        "validate",
        "history w/bob",
        "viewblock 2",
        "tutorial exit"
    };

    private static final String[] tutorialMessages = {
        "First, let's start by creating a new wallet called \"alice\"",
        "Next, let's create another wallet called \"bob\"",
        "Let's look at the wallets that we have created",
        "Notice that both wallets do not have addresses yet!\n" +
                "Let's first generate a key pair for alice",
        "Now we do the same for bob",
        "Now let's list our wallets again",
        "Notice that both wallets have addresses now, we will use these addresses later\n" +
            "Now let us see how much alice has in her wallet",
        "We do the same for bob",
        "Remember the amount of money that each wallet has before the transaction\n" +
                "Before we send money, let's use the help command to learn how to send money",
        "Now we are ready to send money!\n" +
            "Let's get bob to send 3 dollars to alice\n" +
            "For the destination, remember to use the address of alice's wallet we obtained from earlier",
        "Now that the transaction is successful, let's check the balance of the wallets again starting with alice",
        "And now bob",
        "Notice how there was a fee deducted from bob's wallet in addition to the amount that he sent to alice\n" +
                "Now we validate the blockchain to make sure that it is not tampered with",
        "We can also view the transaction that we made",
        "If you want to view the block that shows the transaction instead",
        "Congrats! You made it to the end of the tutorial!\n" +
                "You are now ready to start your own simulated crypto blockchain!"
    };

    private final String arguments;

    public TutorialCommand(String arguments) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
    }

    public void execute (Blockchain blockchain, Scanner in) throws Crypto1010Exception {
        if (!arguments.equals("start")) {
            throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
        }
        Blockchain tutorialBlockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Parser parser = new Parser(walletManager);

        int index = 0;

        Ui.println(WELCOME_MESSAGE);
        while (true) {
            Ui.println();
            Ui.println(tutorialMessages[index]);
            Ui.println("Enter the following command:");
            Ui.println(instructions[index]);
            String input = in.nextLine().strip();
            if (input.equals("tutorial exit") || input.equals("exit")) {
                Ui.println(EXIT_MESSAGE);
                return;
            } else if (input.equals(instructions[index]) ||
                    (index == 9 && input.startsWith(instructions[index].substring(0,19)))) {
                Command c = parser.parse(input);
                try {
                    c.execute(tutorialBlockchain);
                    index++;
                } catch (Crypto1010Exception e) {
                    Ui.println(ERROR_MESSAGE);
                }
            } else {
                Ui.println(ERROR_MESSAGE);
            }
        }
    }
}
