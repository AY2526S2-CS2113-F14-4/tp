package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.CurrencyCode;
import seedu.crypto1010.model.WalletManager;
import seedu.crypto1010.service.CrossAccountTransferService;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

public class CrossSendCommand extends Command {
    private static final String ACCOUNT_PREFIX = "acc/";
    private static final String AMOUNT_PREFIX = "amt/";
    private static final String CURRENCY_PREFIX = "curr/";
    private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");

    private static final String HELP_DESCRIPTION = """
            Format: crossSend acc/ACCOUNT_NAME amt/AMOUNT curr/CURRENCY
            Example: crossSend acc/alice amt/2.5 curr/btc
            
            Transfers AMOUNT from the current account's wallet for CURRENCY to another account
            Only same-currency transfers are allowed
            If the recipient account does not have a wallet for CURRENCY, one is created automatically
            """;
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid crossSend format. "
            + "Use: crossSend acc/ACCOUNT_NAME amt/AMOUNT curr/CURRENCY";
    private static final String AMOUNT_INVALID_ERROR = "Error: Amount must be a positive number.";
    private static final String ACCOUNT_INVALID_ERROR = "Error: ACCOUNT_NAME is invalid.";
    private static final String CURRENCY_INVALID_ERROR = "Error: CURRENCY must be 2-10 letters or digits.";
    private static final String COMMAND_FORMAT = "Use: crossSend acc/ACCOUNT_NAME amt/AMOUNT curr/CURRENCY";

    private final String arguments;
    private final CrossAccountTransferService crossAccountTransferService;

    public CrossSendCommand(String arguments, WalletManager walletManager, String currentAccountName,
                            Class<?> storageAnchor) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
        this.crossAccountTransferService = new CrossAccountTransferService(
                Objects.requireNonNull(walletManager),
                currentAccountName,
                storageAnchor);
    }

    @Override
    public void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception {
        ParsedArgs parsedArgs = parseArguments(arguments);
        if (parsedArgs == null) {
            throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
        }

        if (!ACCOUNT_NAME_PATTERN.matcher(parsedArgs.accountName()).matches()) {
            throw new Crypto1010Exception(ACCOUNT_INVALID_ERROR + " " + COMMAND_FORMAT);
        }

        BigDecimal amount = parsePositiveAmount(parsedArgs.amount());
        String normalizedCurrency = normalizeCurrency(parsedArgs.currencyCode());

        CrossAccountTransferService.CrossAccountTransferResult result = crossAccountTransferService.transfer(
                parsedArgs.accountName(),
                amount,
                normalizedCurrency,
                blockchain);

        Ui.println("Cross-account transfer completed successfully.");
        Ui.println("From wallet: " + result.senderWalletName());
        Ui.println("To account: " + parsedArgs.accountName().toLowerCase());
        Ui.println("Recipient wallet: " + result.recipientWalletName());
        Ui.println("Amount: " + amount.stripTrailingZeros().toPlainString());
        Ui.println("Currency: " + normalizedCurrency);
        if (result.recipientWalletCreated()) {
            Ui.println("Recipient wallet was created automatically.");
        }
    }

    private ParsedArgs parseArguments(String args) {
        if (args == null || args.isBlank()) {
            return null;
        }

        String[] tokens = args.trim().split("\\s+");
        String accountName = null;
        String amount = null;
        String currencyCode = null;

        for (String token : tokens) {
            if (token.startsWith(ACCOUNT_PREFIX)) {
                if (accountName != null) {
                    return null;
                }
                accountName = token.substring(ACCOUNT_PREFIX.length()).trim();
                if (accountName.isEmpty()) {
                    return null;
                }
                continue;
            }
            if (token.startsWith(AMOUNT_PREFIX)) {
                if (amount != null) {
                    return null;
                }
                amount = token.substring(AMOUNT_PREFIX.length()).trim();
                if (amount.isEmpty()) {
                    return null;
                }
                continue;
            }
            if (token.startsWith(CURRENCY_PREFIX)) {
                if (currencyCode != null) {
                    return null;
                }
                currencyCode = token.substring(CURRENCY_PREFIX.length()).trim();
                if (currencyCode.isEmpty()) {
                    return null;
                }
                continue;
            }
            return null;
        }

        if (accountName == null || amount == null || currencyCode == null) {
            return null;
        }

        return new ParsedArgs(accountName, amount, currencyCode);
    }

    private BigDecimal parsePositiveAmount(String amountText) throws Crypto1010Exception {
        try {
            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new Crypto1010Exception(AMOUNT_INVALID_ERROR + " " + COMMAND_FORMAT);
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new Crypto1010Exception(AMOUNT_INVALID_ERROR + " " + COMMAND_FORMAT);
        }
    }

    private String normalizeCurrency(String currencyCode) throws Crypto1010Exception {
        if (!CurrencyCode.isValidSpecificCurrency(currencyCode)) {
            throw new Crypto1010Exception(CURRENCY_INVALID_ERROR + " " + COMMAND_FORMAT);
        }
        return CurrencyCode.normalize(currencyCode);
    }

    private record ParsedArgs(String accountName, String amount, String currencyCode) {
    }
}
