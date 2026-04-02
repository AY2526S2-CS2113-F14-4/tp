package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.CurrencyCode;
import seedu.crypto1010.model.Wallet;
import seedu.crypto1010.model.WalletManager;

import java.util.Objects;
import java.util.Scanner;

public class CreateCommand extends Command {
    private static final String HELP_DESCRIPTION = """
            Format: create w/WALLET_NAME [curr/CURRENCY]
            Example: create w/BobWallet curr/btc
            
            Creates a wallet with the associated NAME
            NAME must be one word without spaces
            CURRENCY is optional and is used by crossSend to identify the wallet for that currency
            """;
  
    private static final String NAME_ERROR = "Error: wallet name cannot be empty.";
    private static final String NAME_WHITESPACE_ERROR = "Error: wallet name must be one word without spaces.";
    private static final String NAME_RESERVED_CHARACTER_ERROR =
            "Error: wallet name cannot contain '|'.";
    private static final String DUPLICATE_ERROR = "Error: wallet name already exists.";
    private static final String DUPLICATE_CURRENCY_ERROR =
            "Error: a wallet for that currency already exists in this account.";
    private static final String INVALID_FORMAT_ERROR =
            "Error: Invalid create format. Use: create w/WALLET_NAME [curr/CURRENCY]";
    private static final String CREATE_FORMAT = "Use: create w/WALLET_NAME [curr/CURRENCY]";
    private static final String CURRENCY_INVALID_ERROR = "Error: CURRENCY must be 2-10 letters or digits.";
    private static final String WALLET_PREFIX = "w/";
    private static final String CURRENCY_PREFIX = "curr/";

    private final String arguments;
    private final WalletManager walletManager;

    public CreateCommand(String arguments, WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
        this.walletManager = Objects.requireNonNull(walletManager);
    }

    @Override
    public void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception {
        ParsedCreateArguments parsedArguments = parseArguments(resolveArguments(arguments));
        String walletName = parsedArguments.walletName();
        String currencyCode = parsedArguments.currencyCode();

        if (walletManager.hasWallet(walletName)) {
            throw new Crypto1010Exception(DUPLICATE_ERROR);
        }
        if (!CurrencyCode.isGeneric(currencyCode) && walletManager.hasWalletForCurrency(currencyCode)) {
            throw new Crypto1010Exception(DUPLICATE_CURRENCY_ERROR + " " + CREATE_FORMAT);
        }

        Wallet wallet = walletManager.createWallet(walletName, currencyCode);
        if (CurrencyCode.isGeneric(currencyCode)) {
            Ui.println("Wallet created: " + wallet.getName());
        } else {
            Ui.println("Wallet created: " + wallet.getName() + " | Currency: " + currencyCode);
        }
    }

    private String resolveArguments(String description) {
        if (arguments == null || arguments.isBlank()) {
            return description;
        }
        return arguments;
    }

    private ParsedCreateArguments parseArguments(String args) throws Crypto1010Exception {
        if (args == null || args.isBlank()) {
            throw new Crypto1010Exception(NAME_ERROR + " " + CREATE_FORMAT);
        }

        String[] tokens = args.trim().split("\\s+");
        String walletName = null;
        String currencyCode = CurrencyCode.GENERIC;

        for (String token : tokens) {
            if (token.startsWith(WALLET_PREFIX)) {
                if (walletName != null) {
                    throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
                }
                walletName = token.substring(WALLET_PREFIX.length()).trim();
                continue;
            }
            if (token.startsWith(CURRENCY_PREFIX)) {
                if (!CurrencyCode.isGeneric(currencyCode)) {
                    throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
                }
                String parsedCurrency = token.substring(CURRENCY_PREFIX.length()).trim();
                if (!CurrencyCode.isValidSpecificCurrency(parsedCurrency)) {
                    throw new Crypto1010Exception(CURRENCY_INVALID_ERROR + " " + CREATE_FORMAT);
                }
                currencyCode = CurrencyCode.normalize(parsedCurrency);
                continue;
            }
            if (walletName != null && CurrencyCode.isGeneric(currencyCode) && !token.contains("/")) {
                throw new Crypto1010Exception(NAME_WHITESPACE_ERROR + " " + CREATE_FORMAT);
            }
            throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
        }

        if (walletName == null) {
            throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
        }
        walletName = CommandParserUtil.validateWalletName(
                walletName,
                NAME_ERROR,
                NAME_WHITESPACE_ERROR,
                CREATE_FORMAT);
        if (walletName.contains("|")) {
            throw new Crypto1010Exception(NAME_RESERVED_CHARACTER_ERROR + " " + CREATE_FORMAT);
        }

        return new ParsedCreateArguments(walletName, currencyCode);
    }

    private record ParsedCreateArguments(String walletName, String currencyCode) {
    }
}
