package seedu.crypto1010.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Block {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int index;
    private final LocalDateTime timestamp;
    private final String previousHash;
    private final List<String> transactions;
    private final String currentHash;

    public Block(int index, LocalDateTime timestamp, String previousHash, List<String> transactions) {
        this(index, timestamp, previousHash, transactions,
                computeHash(index, timestamp, previousHash, transactions));
    }

    public Block(int index, LocalDateTime timestamp, String previousHash,
                 List<String> transactions, String currentHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.previousHash = previousHash;
        this.transactions = new ArrayList<>(transactions);
        this.currentHash = currentHash;
        assert index >= 0 : "block index must be non-negative";
        assert this.timestamp != null : "timestamp must not be null";
        assert this.previousHash != null : "previous hash must not be null";
        assert this.transactions != null : "transactions must not be null";
        assert this.currentHash != null : "current hash must not be null";
    }

    public int getIndex() {
        return index;
    }

    public String getTimestamp() {
        assert timestamp != null : "timestamp must not be null";
        return timestamp.format(FORMATTER);
    }

    public LocalDateTime getTimestampValue() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public List<String> getTransactions() {
        assert transactions != null : "transactions must not be null";
        return Collections.unmodifiableList(transactions);
    }

    public String computeCurrentHash() {
        assert transactions != null : "transactions must not be null";
        return computeHash(index, timestamp, previousHash, transactions);
    }

    public boolean hasValidTransactionData() {
        if (transactions.isEmpty()) {
            return false;
        }
        for (String transaction : transactions) {
            if (transaction == null || transaction.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String computeHash(int index, LocalDateTime timestamp,
                                      String previousHash, List<String> transactions) {
        assert timestamp != null : "timestamp must not be null";
        assert previousHash != null : "previous hash must not be null";
        assert transactions != null : "transactions must not be null";
        String payload = index + "|" + timestamp + "|" + previousHash + "|" + String.join(";", transactions);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private static String bytesToHex(byte[] hashBytes) {
        StringBuilder hex = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String value = Integer.toHexString(0xff & hashByte);
            if (value.length() == 1) {
                hex.append('0');
            }
            hex.append(value);
        }
        return hex.toString();
    }
}
