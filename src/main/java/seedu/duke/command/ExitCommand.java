package seedu.duke.command;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;

public class ExitCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            format: exit
            Exits the program
            """;

    public ExitCommand() {
        super(HELP_DESCRIPTION);
    }

    @Override
    public void execute(String description, Blockchain blockchain) throws Exceptions {
        System.out.println("exit command executed");
    }
}
