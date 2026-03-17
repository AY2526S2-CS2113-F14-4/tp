package seedu.duke.command;

import seedu.duke.Parser;
import seedu.duke.model.Blockchain;
import seedu.duke.model.WalletManager;

public class HelpCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            format: help [COMMAND]
            COMMAND is optional
            If no valid COMMAND is given: lists all the available commands
            If a valid COMMAND is given: displays details regarding that command
            """;

    public HelpCommand() {
        super(HELP_DESCRIPTION);
    }

    @Override
    public void execute(String description, Blockchain blockchain) {
        WalletManager walletManager = new WalletManager();
        Parser parser = new Parser(walletManager);
        try {
            Command c = parser.parse(description);
            c.displayHelpDescription();
        } catch (IllegalArgumentException e) {
            for (CommandWord c : CommandWord.values()) {
                System.out.print("  ");
                System.out.print(c.getCommand());
                for (int i = 0; i < 12 - c.getCommand().length(); i++) {
                    System.out.print(" ");
                }
                System.out.println(c.getDescription());
            }
            System.out.println("For more details about each command type 'help COMMAND', eg. 'help list'");
        }
    }
}
