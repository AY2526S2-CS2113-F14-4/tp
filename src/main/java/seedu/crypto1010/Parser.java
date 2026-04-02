package seedu.crypto1010;

import seedu.crypto1010.command.BalanceCommand;
import seedu.crypto1010.command.Command;
import seedu.crypto1010.command.CommandWord;
import seedu.crypto1010.command.CrossSendCommand;
import seedu.crypto1010.command.CreateCommand;
import seedu.crypto1010.command.ExitCommand;
import seedu.crypto1010.command.HelpCommand;
import seedu.crypto1010.command.HistoryCommand;
import seedu.crypto1010.command.KeygenCommand;
import seedu.crypto1010.command.ListCommand;
import seedu.crypto1010.command.SendCommand;
import seedu.crypto1010.command.TutorialCommand;
import seedu.crypto1010.command.ValidateCommand;
import seedu.crypto1010.command.ViewChainCommand;
import seedu.crypto1010.command.ViewBlockCommand;
import seedu.crypto1010.model.WalletManager;

public class Parser {
    private final WalletManager walletManager;
    private final String currentAccountName;
    private final Class<?> storageAnchor;

    public Parser(WalletManager walletManager) {
        this(walletManager, "", Crypto1010.class);
    }

    public Parser(WalletManager walletManager, String currentAccountName, Class<?> storageAnchor) {
        this.walletManager = walletManager;
        this.currentAccountName = currentAccountName;
        this.storageAnchor = storageAnchor;
    }

    public CommandWord parseCommand(String commandWord) {
        if (commandWord == null || commandWord.isBlank()) {
            throw new IllegalArgumentException("Command word must not be blank.");
        }
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
        if (inputText == null || inputText.isBlank()) {
            throw new IllegalArgumentException("Input command must not be blank.");
        }
        String trimmedInput = inputText.trim();
        String[] components = trimmedInput.split("\\s+", 2);
        try {
            CommandWord commandWord = parseCommand(components[0]);
            String arguments = components.length > 1 ? components[1].trim() : "";
            return switch (commandWord) {
            case LIST -> new ListCommand(walletManager);
            case HELP -> new HelpCommand(arguments);
            case CREATE -> new CreateCommand(arguments, walletManager);
            case BALANCE -> new BalanceCommand(arguments, walletManager);
            case HISTORY -> new HistoryCommand(arguments, walletManager);
            case CROSSSEND -> new CrossSendCommand(arguments, walletManager, currentAccountName, storageAnchor);
            case VALIDATE -> new ValidateCommand();
            case VIEWCHAIN -> new ViewChainCommand();
            case VIEWBLOCK -> new ViewBlockCommand(arguments);
            case EXIT -> new ExitCommand();
            case SEND -> new SendCommand(arguments, walletManager);
            case KEYGEN -> new KeygenCommand(arguments, walletManager);
            case TUTORIAL -> new TutorialCommand(arguments);
            };
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to parse command: " + trimmedInput, e);
        }
    }
}
