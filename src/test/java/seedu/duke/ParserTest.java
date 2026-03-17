package seedu.duke;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import seedu.duke.command.BalanceCommand;
import seedu.duke.command.Command;
import seedu.duke.model.WalletManager;

import org.junit.jupiter.api.Test;

class ParserTest {
    @Test
    void parse_balanceCommandWithName_returnsBalanceCommand() {
        Parser parser = new Parser(new WalletManager());

        Command command = parser.parse("balance alice");

        assertInstanceOf(BalanceCommand.class, command);
    }
}
