package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;

import java.util.Scanner;

public abstract class Command {
    protected String helpDescription;

    Command(String helpDescription) {
        this.helpDescription = helpDescription;
    }

    public abstract void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception;

    public void execute(Blockchain blockchain) throws Crypto1010Exception {
        execute(blockchain, null);
    }

    public void displayHelpDescription() {
        Ui.println(helpDescription);
    }
}
