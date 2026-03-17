package seedu.duke.command;

import seedu.duke.exceptions.Exceptions;
import seedu.duke.model.Blockchain;
import seedu.duke.model.Wallet;
import seedu.duke.model.WalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendCommand extends Command {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(w/|to/|amt/|speed/|fee/|note/)");
    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    private static final Pattern BTC_ADDRESS_PATTERN =
        Pattern.compile("^(bc1[ac-hj-np-z02-9]{11,71}|[13][a-km-zA-HJ-NP-Z1-9]{25,34})$");
    private static final Pattern SOL_ADDRESS_PATTERN = Pattern.compile("^[1-9A-HJ-NP-Za-km-z]{32,44}$");

    private static final String DEFAULT_SPEED = "standard";
    private static final String NETWORK_FEE_ACCOUNT = "network-fee";
    private static final BigDecimal SLOW_FEE = new BigDecimal("0.0005");
    private static final BigDecimal STANDARD_FEE = new BigDecimal("0.0010");
    private static final BigDecimal FAST_FEE = new BigDecimal("0.0020");

    private static final String HELP_DESCRIPTION = """
            Format: send w/WALLET_NAME to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]
            SPEED FEE and MEMO are optional arguments
            Sends cryptocurrency from one wallet to another address
            """;
    private static final String INVALID_FORMAT_ERROR = "Error: Invalid send format. Use: send w/WALLET_NAME"
        + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]";
    private static final String WALLET_NOT_FOUND_ERROR = "Error: Wallet not found.";
    private static final String AMOUNT_INVALID_ERROR = "Error: Amount must be a positive number.";
    private static final String FEE_INVALID_ERROR = "Error: Fee must be a non-negative number.";
    private static final String SPEED_INVALID_ERROR = "Error: Unsupported speed. Use slow, standard, or fast.";
    private static final String INSUFFICIENT_BALANCE_ERROR = "Error: Insufficient balance.";
    private static final String INVALID_ADDRESS_ERROR = "Error: Invalid recipient address.";

    private final String arguments;
    private final WalletManager walletManager;

    public SendCommand(String arguments, WalletManager walletManager) {
        super(HELP_DESCRIPTION);
        this.arguments = arguments;
        this.walletManager = walletManager;
    }

    @Override
    public void execute(String description, Blockchain blockchain) throws Exceptions {
        ParsedArgs parsed = parseArguments(arguments);
        if (parsed == null) {
            throw new Exceptions(INVALID_FORMAT_ERROR);
        }

        Wallet wallet = walletManager.findWallet(parsed.walletName)
                .orElseThrow(() -> new Exceptions(WALLET_NOT_FOUND_ERROR));

        BigDecimal amount = parseDecimal(parsed.amount);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exceptions(AMOUNT_INVALID_ERROR);
        }

        if (!isValidAddress(parsed.recipientAddress)) {
            throw new Exceptions(INVALID_ADDRESS_ERROR);
        }

        String speed = parsed.speed == null ? DEFAULT_SPEED : parsed.speed.toLowerCase();
        if (!isSupportedSpeed(speed)) {
            throw new Exceptions(SPEED_INVALID_ERROR);
        }

        BigDecimal fee = resolveFee(parsed.fee, speed);
        if (fee == null) {
            throw new Exceptions(FEE_INVALID_ERROR);
        }

        BigDecimal balance = blockchain.getPreciseBalance(parsed.walletName);
        BigDecimal totalCost = amount.add(fee);
        if (balance.compareTo(totalCost) < 0) {
            throw new Exceptions(INSUFFICIENT_BALANCE_ERROR);
        }

        List<String> transactions = new ArrayList<>();
        transactions.add(formatTransaction(parsed.walletName, parsed.recipientAddress, amount));
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            transactions.add(formatTransaction(parsed.walletName, NETWORK_FEE_ACCOUNT, fee));
        }
        blockchain.addTransactions(transactions);

        wallet.addTransaction(buildHistoryEntry(parsed, speed, fee));

        System.out.println("Transaction sent successfully.");
        System.out.println("Wallet: " + parsed.walletName);
        System.out.println("To: " + parsed.recipientAddress);
        System.out.println("Amount: " + amount.toPlainString());
        System.out.println("Speed: " + (parsed.fee == null ? speed : "manual"));
        System.out.println("Fee: " + fee.toPlainString());
        if (parsed.note != null) {
            System.out.println("Note: " + parsed.note);
        }
    }

    private ParsedArgs parseArguments(String args) {
        if (args == null || args.isBlank()) {
            return null;
        }

        String trimmedArgs = args.trim();
        Matcher matcher = PREFIX_PATTERN.matcher(trimmedArgs);
        List<PrefixMatch> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(new PrefixMatch(matcher.group(), matcher.start()));
        }

        if (matches.isEmpty() || matches.get(0).startIndex != 0) {
            return null;
        }

        ParsedArgs parsed = new ParsedArgs();
        boolean hasWallet = false;
        boolean hasRecipient = false;
        boolean hasAmount = false;
        boolean hasSpeed = false;
        boolean hasFee = false;
        boolean hasNote = false;

        for (int i = 0; i < matches.size(); i++) {
            PrefixMatch current = matches.get(i);
            int valueStart = current.startIndex + current.prefix.length();
            int valueEnd = i + 1 < matches.size() ? matches.get(i + 1).startIndex : trimmedArgs.length();
            String value = trimmedArgs.substring(valueStart, valueEnd).trim();
            if (value.isEmpty()) {
                return null;
            }

            switch (current.prefix) {
            case "w/":
                if (hasWallet || containsWhitespace(value)) {
                    return null;
                }
                parsed.walletName = value;
                hasWallet = true;
                break;
            case "to/":
                if (hasRecipient || containsWhitespace(value)) {
                    return null;
                }
                parsed.recipientAddress = value;
                hasRecipient = true;
                break;
            case "amt/":
                if (hasAmount || containsWhitespace(value)) {
                    return null;
                }
                parsed.amount = value;
                hasAmount = true;
                break;
            case "speed/":
                if (hasSpeed || containsWhitespace(value)) {
                    return null;
                }
                parsed.speed = value;
                hasSpeed = true;
                break;
            case "fee/":
                if (hasFee || containsWhitespace(value)) {
                    return null;
                }
                parsed.fee = value;
                hasFee = true;
                break;
            case "note/":
                if (hasNote) {
                    return null;
                }
                parsed.note = value;
                hasNote = true;
                break;
            default:
                return null;
            }
        }

        if (parsed.walletName == null || parsed.recipientAddress == null || parsed.amount == null) {
            return null;
        }

        return parsed;
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

    private String formatTransaction(String sender, String receiver, BigDecimal amount) {
        return sender + " -> " + receiver + " : " + amount.stripTrailingZeros().toPlainString();
    }

    private String buildHistoryEntry(ParsedArgs parsed, String speed, BigDecimal fee) {
        StringBuilder history = new StringBuilder();
        history.append("to/").append(parsed.recipientAddress)
                .append(" amt/").append(parsed.amount)
                .append(" speed/").append(parsed.fee == null ? speed : "manual")
                .append(" fee/").append(fee.stripTrailingZeros().toPlainString());
        if (parsed.note != null) {
            history.append(" note/").append(parsed.note);
        }
        return history.toString();
    }

    private boolean containsWhitespace(String value) {
        return value.chars().anyMatch(Character::isWhitespace);
    }

    private boolean isValidAddress(String address) {
        if (address == null || address.isBlank() || containsWhitespace(address)) {
            return false;
        }
        return ETH_ADDRESS_PATTERN.matcher(address).matches()
                || BTC_ADDRESS_PATTERN.matcher(address).matches()
                || SOL_ADDRESS_PATTERN.matcher(address).matches();
    }

    private static class PrefixMatch {
        private final String prefix;
        private final int startIndex;

        private PrefixMatch(String prefix, int startIndex) {
            this.prefix = prefix;
            this.startIndex = startIndex;
        }
    }

    private static class ParsedArgs {
        String walletName;
        String recipientAddress;
        String amount;
        String speed;
        String fee;
        String note;
    }
}
