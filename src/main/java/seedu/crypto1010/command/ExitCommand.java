package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;

import java.util.Scanner;

public class ExitCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: exit
            Exits the program
            """;

    public ExitCommand() {
        super(HELP_DESCRIPTION);
    }

    @Override
    public void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception {
        Ui.println("Exiting Crypto1010...");
    }
}
