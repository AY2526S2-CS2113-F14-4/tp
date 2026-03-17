package seedu.duke.command;

public enum CommandWord {
    HELP("help", "lists all commands"),
    EXIT("exit", "exits the program"),

    // Wallet related commands
    CREATE("create", "creates a new wallet"),
    LIST("list", "lists all your wallets"),
    KEYGEN("keygen", "generates and displays key pair"),
    BALANCE("balance", "displays balance of wallet"),
    SEND("send", "sends cryptocurrency from one wallet to another"),

    // Blockchain related commands
    VALIDATE("validate", "validates block chain integrity"),
    VIEWBLOCK("viewblock", "displays details of the block");

    private final String command;
    private final String description;

    CommandWord(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getCommand() {
        return command;
    }
}
