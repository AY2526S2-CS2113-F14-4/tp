package seedu.crypto1010.command;

import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.WalletManager;
import seedu.crypto1010.service.TransactionRecordingService;
import seedu.crypto1010.service.TransferRequest;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SendCommand extends Command {
    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    private static final Pattern BTC_LEGACY_ADDRESS_PATTERN =
            Pattern.compile("^[13][A-HJ-NP-Za-km-z1-9]{25,34}$");
    private static final Pattern BTC_BECH32_ADDRESS_PATTERN =
            Pattern.compile("^(bc1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]{11,71}"
                    + "|BC1[QPZRY9X8GF2TVDW0S3JN54KHCE6MUA7L]{11,71})$");
    private static final Pattern SOL_ADDRESS_PATTERN = Pattern.compile("^[1-9A-HJ-NP-Za-km-z]{32,44}$");

    private static final String DEFAULT_SPEED = "standard";
    private static final String MANUAL_SPEED_LABEL = "manual";
    private static final String WALLET_PREFIX = "w/";
    private static final String RECIPIENT_PREFIX = "to/";
    private static final String AMOUNT_PREFIX = "amt/";
    private static final String SPEED_PREFIX = "speed/";
    private static final String FEE_PREFIX = "fee/";
    private static final String NOTE_PREFIX = "note/";
    private static final BigDecimal SLOW_FEE = new BigDecimal("0.0005");
    private static final BigDecimal STANDARD_FEE = new BigDecimal("0.0010");
    private static final BigDecimal FAST_FEE = new BigDecimal("0.0020");

    private static final String HELP_DESCRIPTION = """
            Format: send w/WALLET_NAME to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]
            Examples: send w/alice to/0x8b3...e21 amt/0.05
                      send w/main to/bc1qxyz...9k amt/0.001 speed/slow fee/0.00001 note/rent

            SPEED FEE and MEMO are optional arguments
            Supported SPEED values: speed/slow, speed/standard, speed/fast
            If fee/FEE is provided, it overrides any speed/SPEED
            If neither speed/ nor fee/ is provided, speed/standard is used by default
            Sends cryptocurrency from one wallet to another address
            """;
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid send format. Use: send w/WALLET_NAME"
        + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]";
    private static final String SEND_FORMAT = "Use: send w/WALLET_NAME"
            + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]";
    private static final String AMOUNT_INVALID_ERROR = "Error: Amount must be a positive number.";
    private static final String FEE_INVALID_ERROR = "Error: Fee must be a non-negative number.";
    private static final String SPEED_INVALID_ERROR = "Error: Unsupported speed. Use speed/slow, speed/standard,"
            + " or speed/fast.";
    private static final String INVALID_ADDRESS_ERROR = "Error: Invalid recipient address.";
    private static final String WALLET_NOT_FOUND_ERROR = "Error: Wallet not found.";

    private final String arguments;
    private final WalletManager walletManager;
    private final TransactionRecordingService transactionRecordingService;

    public SendCommand(String arguments, WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
        this.walletManager = Objects.requireNonNull(walletManager);
        this.transactionRecordingService = new TransactionRecordingService(this.walletManager);
    }

    @Override
    public void execute(Blockchain blockchain, Scanner in) throws Crypto1010Exception {
        ParsedArgs parsed = parseRequiredArguments(arguments);
        validateSenderWallet(parsed.walletName);
        validateRecipientAddress(parsed.recipientAddress);

        TransferRequest transferRequest = createTransferRequest(parsed);
        transactionRecordingService.recordTransfer(transferRequest, blockchain);

        printTransferSummary(transferRequest);
    }

    private ParsedArgs parseRequiredArguments(String args) throws Crypto1010Exception {
        ParsedArgs parsed = parseArguments(args);
        if (parsed == null) {
            throw new Crypto1010Exception(INVALID_FORMAT_ERROR);
        }
        return parsed;
    }

    private void validateSenderWallet(String walletName) throws Crypto1010Exception {
        if (!walletManager.hasWallet(walletName)) {
            throw new Crypto1010Exception(WALLET_NOT_FOUND_ERROR);
        }
    }

    private void validateRecipientAddress(String recipientAddress) throws Crypto1010Exception {
        if (!isValidAddress(recipientAddress)) {
            throw new Crypto1010Exception(INVALID_ADDRESS_ERROR + " " + SEND_FORMAT);
        }
    }

    private TransferRequest createTransferRequest(ParsedArgs parsed) throws Crypto1010Exception {
        BigDecimal amount = parsePositiveAmount(parsed.amount);
        String speed = resolveSpeed(parsed.speed);
        BigDecimal fee = resolveValidatedFee(parsed.fee, speed);
        String speedLabel = parsed.fee == null ? speed : MANUAL_SPEED_LABEL;

        return new TransferRequest(
                parsed.walletName,
                parsed.recipientAddress,
                amount,
                speedLabel,
                fee,
                parsed.note);
    }

    private BigDecimal parsePositiveAmount(String amountArgument) throws Crypto1010Exception {
        BigDecimal amount = parseDecimal(amountArgument);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Crypto1010Exception(AMOUNT_INVALID_ERROR + " " + SEND_FORMAT);
        }
        return amount;
    }

    private String resolveSpeed(String speedArgument) throws Crypto1010Exception {
        String speed = speedArgument == null ? DEFAULT_SPEED : speedArgument.toLowerCase();
        if (!isSupportedSpeed(speed)) {
            throw new Crypto1010Exception(SPEED_INVALID_ERROR + " " + SEND_FORMAT);
        }
        return speed;
    }

    private BigDecimal resolveValidatedFee(String feeArgument, String speed) throws Crypto1010Exception {
        BigDecimal fee = resolveFee(feeArgument, speed);
        if (fee == null) {
            throw new Crypto1010Exception(FEE_INVALID_ERROR + " " + SEND_FORMAT);
        }
        return fee;
    }

    private void printTransferSummary(TransferRequest transferRequest) {
        Ui.println("Transaction sent successfully.");
        Ui.println("Wallet: " + transferRequest.getSenderWalletName());
        Ui.println("To: " + transferRequest.getRecipientAddress());
        Ui.println("Amount: " + transferRequest.getAmount().toPlainString());
        Ui.println("Speed: " + transferRequest.getSpeedLabel());
        Ui.println("Fee: " + transferRequest.getFee().toPlainString());
        if (transferRequest.getNote() != null) {
            Ui.println("Note: " + transferRequest.getNote());
        }
    }

    private ParsedArgs parseArguments(String args) {
        if (args == null || args.isBlank()) {
            return null;
        }

        String[] tokens = args.trim().split("\\s+");
        ParsedArgs parsed = new ParsedArgs();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isBlank()) {
                continue;
            }

            if (token.startsWith(NOTE_PREFIX)) {
                if (parsed.note != null) {
                    return null;
                }
                parsed.note = extractNoteValue(tokens, i);
                if (parsed.note == null) {
                    return null;
                }
                break;
            }

            TokenParseResult result = parseSingleValueToken(
                    token,
                    parsed,
                    WALLET_PREFIX,
                    parsed.walletName,
                    value -> parsed.walletName = value);
            if (result == TokenParseResult.PARSED) {
                continue;
            }
            if (result == TokenParseResult.INVALID) {
                return null;
            }

            result = parseSingleValueToken(
                    token,
                    parsed,
                    RECIPIENT_PREFIX,
                    parsed.recipientAddress,
                    value -> parsed.recipientAddress = value);
            if (result == TokenParseResult.PARSED) {
                continue;
            }
            if (result == TokenParseResult.INVALID) {
                return null;
            }

            result = parseSingleValueToken(
                    token,
                    parsed,
                    AMOUNT_PREFIX,
                    parsed.amount,
                    value -> parsed.amount = value);
            if (result == TokenParseResult.PARSED) {
                continue;
            }
            if (result == TokenParseResult.INVALID) {
                return null;
            }

            result = parseSingleValueToken(
                    token,
                    parsed,
                    SPEED_PREFIX,
                    parsed.speed,
                    value -> parsed.speed = value);
            if (result == TokenParseResult.PARSED) {
                continue;
            }
            if (result == TokenParseResult.INVALID) {
                return null;
            }

            result = parseSingleValueToken(
                    token,
                    parsed,
                    FEE_PREFIX,
                    parsed.fee,
                    value -> parsed.fee = value);
            if (result == TokenParseResult.PARSED) {
                continue;
            }
            if (result == TokenParseResult.INVALID) {
                return null;
            }
            return null;
        }

        if (parsed.walletName == null || parsed.recipientAddress == null || parsed.amount == null) {
            return null;
        }

        return parsed;
    }

    private String extractNoteValue(String[] tokens, int noteTokenIndex) {
        StringBuilder noteBuilder = new StringBuilder(tokens[noteTokenIndex].substring(NOTE_PREFIX.length()));
        for (int i = noteTokenIndex + 1; i < tokens.length; i++) {
            noteBuilder.append(" ").append(tokens[i]);
        }
        String noteValue = noteBuilder.toString().trim();
        return noteValue.isEmpty() ? null : noteValue;
    }

    private TokenParseResult parseSingleValueToken(String token, ParsedArgs parsed, String prefix,
                                                   String existingValue, Consumer<String> setter) {
        if (!token.startsWith(prefix)) {
            return TokenParseResult.NOT_MATCHED;
        }

        String value = token.substring(prefix.length()).trim();
        if (existingValue != null || value.isEmpty() || containsWhitespace(value)) {
            return TokenParseResult.INVALID;
        }

        setter.accept(value);
        return TokenParseResult.PARSED;
    }

    private BigDecimal parseDecimal(String amountStr) {
        try {
            return new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal resolveFee(String feeArgument, String speed) {
        if (feeArgument != null) {
            BigDecimal manualFee = parseDecimal(feeArgument);
            if (manualFee == null || manualFee.compareTo(BigDecimal.ZERO) < 0) {
                return null;
            }
            return manualFee;
        }

        return switch (speed) {
        case "slow" -> SLOW_FEE;
        case "standard" -> STANDARD_FEE;
        case "fast" -> FAST_FEE;
        default -> null;
        };
    }

    private boolean isSupportedSpeed(String speed) {
        return "slow".equals(speed) || "standard".equals(speed) || "fast".equals(speed);
    }

    private boolean containsWhitespace(String value) {
        return value.chars().anyMatch(Character::isWhitespace);
    }

    private boolean isValidAddress(String address) {
        if (address == null || address.isBlank() || containsWhitespace(address)) {
            return false;
        }
        return ETH_ADDRESS_PATTERN.matcher(address).matches()
                || BTC_LEGACY_ADDRESS_PATTERN.matcher(address).matches()
                || BTC_BECH32_ADDRESS_PATTERN.matcher(address).matches()
                || SOL_ADDRESS_PATTERN.matcher(address).matches();
    }

    private static class ParsedArgs {
        String walletName;
        String recipientAddress;
        String amount;
        String speed;
        String fee;
        String note;
    }

    private enum TokenParseResult {
        NOT_MATCHED,
        PARSED,
        INVALID
    }
}
