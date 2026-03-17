package seedu.duke.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Blockchain {
    private static final String GENESIS_PREVIOUS_HASH = "0000000000000000";
    private static final Pattern TRANSACTION_PATTERN =
            Pattern.compile("^(.+?)\\s*->\\s*(.+?)\\s*:\\s*([+-]?\\d+(?:\\.\\d+)?)$");

    private final List<Block> blocks;

    public Blockchain(List<Block> blocks) {
        this.blocks = new ArrayList<>(blocks);
    }

    public static Blockchain createDefault() {
        List<Block> defaultBlocks = new ArrayList<>();
        Block genesis = new Block(
                0,
                LocalDateTime.of(2026, 2, 12, 14, 30, 21),
                GENESIS_PREVIOUS_HASH,
                List.of("Genesis Block"));
        defaultBlocks.add(genesis);
        defaultBlocks.add(new Block(
                1,
                LocalDateTime.of(2026, 2, 12, 14, 35, 2),
                genesis.getCurrentHash(),
                List.of("alice -> bob : 10", "bob -> carol : 5")));
        return new Blockchain(defaultBlocks);
    }

    public int size() {
        return blocks.size();
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public Block getBlock(int index) {
        return blocks.get(index);
    }

    public ValidationResult validate() {
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);

            String computedHash = block.computeCurrentHash();
            if (!computedHash.equals(block.getCurrentHash())) {
                return ValidationResult.invalid("Hash mismatch at Block " + i + ".");
            }

            if (!block.hasValidTransactionData()) {
                return ValidationResult.invalid("Invalid transaction data at Block " + i + ".");
            }

            if (i == 0) {
                if (!GENESIS_PREVIOUS_HASH.equals(block.getPreviousHash())) {
                    return ValidationResult.invalid("Invalid previous hash linkage at Block " + i + ".");
                }
                continue;
            }

            Block previousBlock = blocks.get(i - 1);
            if (!previousBlock.getCurrentHash().equals(block.getPreviousHash())) {
                return ValidationResult.invalid("Invalid previous hash linkage at Block " + i + ".");
            }
        }

        return ValidationResult.valid();
    }

    public void addTransactions(List<String> transactions) {
        Objects.requireNonNull(transactions);
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Transactions cannot be empty.");
        }

        Block previousBlock = blocks.get(blocks.size() - 1);
        Block newBlock = new Block(
                previousBlock.getIndex() + 1,
                LocalDateTime.now(),
                previousBlock.getCurrentHash(),
                transactions);
        blocks.add(newBlock);
    }

    public double getBalance(String walletName) {
        return getPreciseBalance(walletName).doubleValue();
    }

    public BigDecimal getPreciseBalance(String walletName) {
        String normalizedWalletName = walletName == null ? "" : walletName.trim();
        BigDecimal balance = BigDecimal.ZERO;
        for (Block block : blocks) {
            for (String transaction : block.getTransactions()) {
                balance = balance.add(parseTransactionAmount(normalizedWalletName, transaction));
            }
        }
        return balance;
    }

    private BigDecimal parseTransactionAmount(String walletName, String transaction) {
        Matcher matcher = TRANSACTION_PATTERN.matcher(transaction);
        if (!matcher.matches()) {
            return BigDecimal.ZERO;
        }

        String sender = matcher.group(1).trim();
        String receiver = matcher.group(2).trim();
        BigDecimal amount = new BigDecimal(matcher.group(3));
        BigDecimal delta = BigDecimal.ZERO;
        if (sender.equalsIgnoreCase(walletName)) {
            delta = delta.subtract(amount);
        }
        if (receiver.equalsIgnoreCase(walletName)) {
            delta = delta.add(amount);
        }
        return delta;
    }
}
