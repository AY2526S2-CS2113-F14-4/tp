package seedu.duke.storage;

import seedu.duke.model.Block;
import seedu.duke.model.Blockchain;
import seedu.duke.model.ValidationResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlockchainStorage {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DATA_DIR = "data";
    private static final String FILE_NAME = "blockchain.json";

    private final Path dataFilePath;

    public BlockchainStorage(Class<?> appClass) {
        this.dataFilePath = resolveDataFilePath(appClass);
    }

    public Blockchain load() throws IOException {
        if (!Files.exists(dataFilePath)) {
            return Blockchain.createDefault();
        }

        String json = Files.readString(dataFilePath, StandardCharsets.UTF_8);
        Blockchain loaded = fromJson(json);
        ValidationResult result = loaded.validate();
        if (!result.isValid()) {
            throw new IOException("Loaded blockchain is invalid: " + result.getReason());
        }
        return loaded;
    }

    public void save(Blockchain blockchain) throws IOException {
        Files.createDirectories(dataFilePath.getParent());
        Files.writeString(dataFilePath, toJson(blockchain), StandardCharsets.UTF_8);
    }

    private Path resolveDataFilePath(Class<?> appClass) {
        Path defaultPath = Path.of(System.getProperty("user.dir"), DATA_DIR, FILE_NAME);
        try {
            Path codeSourcePath = Path.of(appClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(codeSourcePath)) {
                return codeSourcePath.getParent().resolve(DATA_DIR).resolve(FILE_NAME);
            }
            return defaultPath;
        } catch (URISyntaxException | NullPointerException e) {
            return defaultPath;
        }
    }

    private String toJson(Blockchain blockchain) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"blocks\": [");
        List<Block> blocks = blockchain.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\n    {");
            sb.append("\n      \"index\": ").append(block.getIndex()).append(",");
            sb.append("\n      \"timestamp\": \"").append(escapeJson(block.getTimestamp())).append("\",");
            sb.append("\n      \"previousHash\": \"").append(escapeJson(block.getPreviousHash())).append("\",");
            sb.append("\n      \"currentHash\": \"").append(escapeJson(block.getCurrentHash())).append("\",");
            sb.append("\n      \"transactions\": [");
            List<String> transactions = block.getTransactions();
            for (int j = 0; j < transactions.size(); j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(escapeJson(transactions.get(j))).append("\"");
            }
            sb.append("]\n    }");
        }
        sb.append("\n  ]\n}\n");
        return sb.toString();
    }

    private Blockchain fromJson(String json) throws IOException {
        Object parsed = new JsonParser(json).parse();
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IOException("Invalid blockchain JSON: root must be an object.");
        }

        Object blocksObj = root.get("blocks");
        if (!(blocksObj instanceof List<?> blockList)) {
            throw new IOException("Invalid blockchain JSON: missing blocks array.");
        }

        List<Block> blocks = new ArrayList<>();
        for (Object blockObj : blockList) {
            if (!(blockObj instanceof Map<?, ?> blockMap)) {
                throw new IOException("Invalid blockchain JSON: block item must be an object.");
            }
            blocks.add(parseBlock(blockMap));
        }

        if (blocks.isEmpty()) {
            throw new IOException("Invalid blockchain JSON: blocks array cannot be empty.");
        }
        return new Blockchain(blocks);
    }

    private Block parseBlock(Map<?, ?> blockMap) throws IOException {
        int index = asInt(blockMap.get("index"), "index");
        LocalDateTime timestamp = asTimestamp(blockMap.get("timestamp"));
        String previousHash = asString(blockMap.get("previousHash"), "previousHash");
        String currentHash = asString(blockMap.get("currentHash"), "currentHash");
        List<String> transactions = asStringList(blockMap.get("transactions"), "transactions");

        return new Block(index, timestamp, previousHash, transactions, currentHash);
    }

    private int asInt(Object value, String fieldName) throws IOException {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("Invalid blockchain JSON: " + fieldName + " must be a number.");
    }

    private LocalDateTime asTimestamp(Object value) throws IOException {
        String text = asString(value, "timestamp");
        try {
            return LocalDateTime.parse(text, TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IOException("Invalid blockchain JSON: timestamp format must be yyyy-MM-dd HH:mm:ss.");
        }
    }

    private String asString(Object value, String fieldName) throws IOException {
        if (value instanceof String text) {
            return text;
        }
        throw new IOException("Invalid blockchain JSON: " + fieldName + " must be a string.");
    }

    private List<String> asStringList(Object value, String fieldName) throws IOException {
        if (!(value instanceof List<?> items)) {
            throw new IOException("Invalid blockchain JSON: " + fieldName + " must be an array.");
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof String text)) {
                throw new IOException("Invalid blockchain JSON: " + fieldName + " entries must be strings.");
            }
            result.add(text);
        }
        return result;
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class JsonParser {
        private final String input;
        private int index;

        private JsonParser(String input) {
            this.input = input;
        }

        private Object parse() throws IOException {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (!isAtEnd()) {
                throw error("Unexpected trailing characters.");
            }
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (isAtEnd()) {
                throw error("Unexpected end of JSON.");
            }

            char c = current();
            return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> parseTrue();
            case 'f' -> parseFalse();
            case 'n' -> parseNull();
            default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            skipWhitespace();
            Map<String, Object> map = new LinkedHashMap<>();
            if (consumeIf('}')) {
                return map;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (consumeIf('}')) {
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() throws IOException {
            expect('[');
            skipWhitespace();
            List<Object> list = new ArrayList<>();
            if (consumeIf(']')) {
                return list;
            }

            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (consumeIf(']')) {
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!isAtEnd()) {
                char c = current();
                index++;
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (isAtEnd()) {
                        throw error("Invalid escape sequence.");
                    }
                    char escaped = current();
                    index++;
                    switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append(parseUnicodeEscape());
                        break;
                    default:
                        throw error("Unsupported escape: \\" + escaped);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw error("Unterminated string.");
        }

        private char parseUnicodeEscape() throws IOException {
            if (index + 4 > input.length()) {
                throw error("Invalid unicode escape.");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw error("Invalid unicode escape.");
            }
        }

        private Object parseNumber() throws IOException {
            int start = index;
            if (consumeIf('-')) {
                // optional sign
            }
            if (consumeIf('0')) {
                // single zero is valid
            } else {
                if (!isDigit(currentOrNull())) {
                    throw error("Invalid number.");
                }
                while (!isAtEnd() && isDigit(current())) {
                    index++;
                }
            }
            if (!isAtEnd() && current() == '.') {
                index++;
                if (isAtEnd() || !isDigit(current())) {
                    throw error("Invalid number.");
                }
                while (!isAtEnd() && isDigit(current())) {
                    index++;
                }
            }
            if (!isAtEnd() && (current() == 'e' || current() == 'E')) {
                index++;
                if (!isAtEnd() && (current() == '+' || current() == '-')) {
                    index++;
                }
                if (isAtEnd() || !isDigit(current())) {
                    throw error("Invalid number.");
                }
                while (!isAtEnd() && isDigit(current())) {
                    index++;
                }
            }

            String token = input.substring(start, index);
            try {
                if (token.contains(".") || token.contains("e") || token.contains("E")) {
                    return Double.parseDouble(token);
                }
                return Long.parseLong(token);
            } catch (NumberFormatException e) {
                throw error("Invalid number format.");
            }
        }

        private Boolean parseTrue() throws IOException {
            expectWord("true");
            return Boolean.TRUE;
        }

        private Boolean parseFalse() throws IOException {
            expectWord("false");
            return Boolean.FALSE;
        }

        private Object parseNull() throws IOException {
            expectWord("null");
            return null;
        }

        private void expectWord(String word) throws IOException {
            if (index + word.length() > input.length() || !input.startsWith(word, index)) {
                throw error("Expected '" + word + "'.");
            }
            index += word.length();
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (isAtEnd() || current() != expected) {
                throw error("Expected '" + expected + "'.");
            }
            index++;
        }

        private boolean consumeIf(char expected) {
            if (!isAtEnd() && current() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (!isAtEnd()) {
                char c = current();
                if (!Character.isWhitespace(c)) {
                    return;
                }
                index++;
            }
        }

        private boolean isAtEnd() {
            return index >= input.length();
        }

        private char current() {
            return input.charAt(index);
        }

        private char currentOrNull() {
            return isAtEnd() ? '\0' : current();
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private IOException error(String message) {
            return new IOException(message + " At position " + index + ".");
        }
    }
}
