package seedu.duke.command;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.ValidationResult;

public class ValidateCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            format: validate
            Validates entire blockchain integrity
            """;

    public ValidateCommand() {
        super(HELP_DESCRIPTION);
    }

    @Override
    public void execute(String description, Blockchain blockchain) throws Exceptions {
        ValidationResult result = blockchain.validate();
        if (result.isValid()) {
            System.out.println("Blockchain is valid. All blocks verified successfully.");
        } else {
            System.out.println("Blockchain is invalid. Reason: " + result.getReason());
        }
    }
}
