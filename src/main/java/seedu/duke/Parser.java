package seedu.duke;

import seedu.duke.command.BalanceCommand;
import seedu.duke.command.Command;
import seedu.duke.command.CommandWord;
import seedu.duke.command.CreateCommand;
import seedu.duke.command.ExitCommand;
import seedu.duke.command.HelpCommand;
import seedu.duke.command.KeygenCommand;
import seedu.duke.command.ListCommand;
import seedu.duke.command.SendCommand;
import seedu.duke.command.ValidateCommand;
import seedu.duke.command.ViewBlockCommand;
import seedu.duke.model.WalletManager;

public class Parser {
    private final WalletManager walletManager;

    public Parser(WalletManager walletManager) {
        this.walletManager = walletManager;
    }

    public CommandWord parseCommand(String commandWord) {
        return CommandWord.valueOf(commandWord.toUpperCase());
    }

    /**
     * Parses the given text input to find
     * which command is being called
     * then returns the class corresponding
     * with the command with the correct fields
     *
     * @param inputText the string from the user input
     * @return the class associated with the command
     *     that was parsed from the input text
     */
    public Command parse(String inputText) {
        String trimmedInput = inputText.trim();
        String[] components = trimmedInput.split("\\s+", 2);
        CommandWord commandWord = parseCommand(components[0]);
        String arguments = components.length > 1 ? components[1].trim() : "";
        return switch (commandWord) {
        case LIST -> new ListCommand(walletManager);
        case HELP -> new HelpCommand();
        case CREATE -> new CreateCommand(arguments, walletManager);
        case BALANCE -> new BalanceCommand(arguments);
        case VALIDATE -> new ValidateCommand();
        case VIEWBLOCK -> new ViewBlockCommand(arguments);
        case EXIT -> new ExitCommand();
        case SEND -> new SendCommand(arguments, walletManager);
        case KEYGEN -> new KeygenCommand(arguments, walletManager);
        };
    }
}
