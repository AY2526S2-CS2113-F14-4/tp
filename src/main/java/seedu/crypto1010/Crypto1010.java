package seedu.crypto1010;

import seedu.crypto1010.auth.AuthenticationException;
import seedu.crypto1010.auth.AuthenticationService;
import seedu.crypto1010.command.Command;
import seedu.crypto1010.command.CommandWord;
import seedu.crypto1010.command.ExitCommand;
import seedu.crypto1010.command.TutorialCommand;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.Wallet;
import seedu.crypto1010.model.WalletManager;
import seedu.crypto1010.storage.AccountStorage;
import seedu.crypto1010.storage.BlockchainStorage;
import seedu.crypto1010.storage.WalletStorage;
import seedu.crypto1010.ui.CliVisuals;
import seedu.crypto1010.ui.InteractiveShell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Crypto1010 {
    private static final Logger LOGGER = Logger.getLogger(Crypto1010.class.getName());
    private static final String ACCOUNT_SELECTION_ERROR =
            "Error: Invalid selection. Choose login, register, or exit.";
    private static final String INVALID_COMMAND_HINT_PREFIX = "Did you mean: ";
    private static final int MAX_TIMELINE_ENTRIES = 14;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<String> RISKY_COMMANDS = List.of("send", "crosssend", "create", "keygen");

    /**
     * Main entry-point for the java.crypto1010.Crypto1010 application.
     */
    public static void main(String[] args) {
        CliVisuals.install();
        try (InteractiveShell shell = InteractiveShell.create()) {
            CliVisuals.printAppBanner();
            CliVisuals.printCommandExplorer();
            CliVisuals.printUiControlGuide();
            Scanner in = new Scanner(System.in);
            AuthenticationService authenticationService = loadAuthenticationService();
            String accountUsername = authenticateUser(shell, authenticationService);
            if (accountUsername == null) {
                return;
            }

            CliVisuals.printWelcomePanel(accountUsername);
            shell.printCommandPalette();
            BlockchainStorage blockchainStorage = new BlockchainStorage(Crypto1010.class, accountUsername);
            WalletStorage walletStorage = new WalletStorage(Crypto1010.class, accountUsername);
            LoadResult<Blockchain> blockchainLoadResult = loadBlockchain(blockchainStorage);
            LoadResult<WalletManager> walletLoadResult = loadWalletManager(walletStorage);
            Blockchain blockchain = blockchainLoadResult.data();
            WalletManager walletManager = walletLoadResult.data();
            boolean allowBlockchainSave = blockchainLoadResult.loadedSuccessfully();
            boolean allowWalletSave = walletLoadResult.loadedSuccessfully();
            if (!allowBlockchainSave) {
                CliVisuals.printWarning(
                        "Blockchain save is disabled to avoid overwriting existing data after load failure.");
            }
            if (!allowWalletSave) {
                CliVisuals.printWarning(
                        "Wallet save is disabled to avoid overwriting existing data after load failure.");
            }
            Parser parser = new Parser(walletManager, accountUsername, Crypto1010.class);
            CliVisuals.printStatusDashboard(accountUsername, walletManager, blockchain);
            shell.printCommandPalette();
            CliRuntimeState runtimeState = new CliRuntimeState();

            while (true) {
                shell.updateDynamicCandidates(
                        walletNames(walletManager),
                        authenticationService.getRegisteredUsernames());
                String rightStatus = CliVisuals.buildRightStatus(
                        runtimeState.inputMode().label(),
                        walletManager,
                        blockchain,
                        runtimeState.lastSaveAt(),
                        runtimeState.confirmationsEnabled());
                String message = shell.readLine(accountUsername, runtimeState.inputMode(), rightStatus);
                if (message == null) {
                    saveData(
                            blockchainStorage,
                            walletStorage,
                            blockchain,
                            walletManager,
                            allowBlockchainSave,
                            allowWalletSave);
                    runtimeState.setLastSaveAt(nowTimestamp());
                    addTimelineEntry(runtimeState.timeline(), "SYS", "session closed", 0);
                    break;
                }
                if (message.isBlank()) {
                    continue;
                }

                if (message.startsWith(":")) {
                    UiDirective directive = handleMetaCommand(
                            message,
                            shell,
                            runtimeState,
                            accountUsername,
                            walletManager,
                            blockchain);
                    if (directive == UiDirective.EXIT) {
                        saveData(
                                blockchainStorage,
                                walletStorage,
                                blockchain,
                                walletManager,
                                allowBlockchainSave,
                                allowWalletSave);
                        runtimeState.setLastSaveAt(nowTimestamp());
                        addTimelineEntry(runtimeState.timeline(), "SYS", "session closed by :exit", 0);
                        break;
                    }
                    continue;
                }

                String normalizedInput = normalizeInputAliases(message);
                String commandWord = extractCommandWord(normalizedInput);
                if (commandWord == null) {
                    CliVisuals.printError("Error: Invalid command. Use: help");
                    continue;
                }

                if (runtimeState.confirmationsEnabled() && requiresConfirmation(commandWord)) {
                    if (!requestCommandConfirmation(shell, normalizedInput)) {
                        CliVisuals.printWarning("Cancelled by user.");
                        addTimelineEntry(runtimeState.timeline(), "SKIP", normalizedInput, 0);
                        continue;
                    }
                }

                Command command;
                try {
                    try {
                        command = parser.parse(normalizedInput);
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.FINE, "Command parse failed for input: " + normalizedInput, e);
                        CliVisuals.printError("Error: Invalid command. Use: help");
                        String suggestion = shell.suggestCommand(normalizedInput);
                        if (suggestion != null) {
                            CliVisuals.printHint(INVALID_COMMAND_HINT_PREFIX + suggestion);
                        }
                        addTimelineEntry(runtimeState.timeline(), "ERR", normalizedInput, 0);
                        continue;
                    }

                    CommandRunResult runResult = executeCommand(command, blockchain, in);
                    if (runResult.error() != null) {
                        LOGGER.log(
                                Level.WARNING,
                                "Command execution failed: " + normalizedInput,
                                runResult.error());
                        List<String> lines = new ArrayList<>(runResult.outputLines());
                        lines.add(runResult.error().getMessage());
                        CliVisuals.printCommandResultCard(normalizedInput, false, runResult.durationMs(), lines);
                        addTimelineEntry(runtimeState.timeline(), "ERR", normalizedInput, runResult.durationMs());
                        continue;
                    }

                    CliVisuals.printCommandResultCard(
                            normalizedInput,
                            true,
                            runResult.durationMs(),
                            runResult.outputLines());
                    LOGGER.info(() -> "Command executed successfully: " + normalizedInput
                            + " (" + runResult.durationMs() + " ms)");
                    saveData(
                            blockchainStorage,
                            walletStorage,
                            blockchain,
                            walletManager,
                            allowBlockchainSave,
                            allowWalletSave);
                    runtimeState.setLastSaveAt(nowTimestamp());
                    addTimelineEntry(runtimeState.timeline(), "OK", normalizedInput, runResult.durationMs());

                    if (command instanceof ExitCommand) {
                        break;
                    }
                    CliVisuals.printSectionBreak();
                } catch (Crypto1010Exception e) {
                    LOGGER.log(Level.WARNING, "Unhandled command execution error: " + normalizedInput, e);
                    CliVisuals.printError(e.getMessage());
                    addTimelineEntry(runtimeState.timeline(), "ERR", normalizedInput, 0);
                }
            }
        } finally {
            CliVisuals.uninstall();
        }
    }

    private static AuthenticationService loadAuthenticationService() {
        AuthenticationService authenticationService = new AuthenticationService(new AccountStorage(Crypto1010.class));
        try {
            authenticationService.load();
        } catch (IOException e) {
            CliVisuals.printWarning("Failed to load account data. Starting with no registered accounts.");
        }
        return authenticationService;
    }

    private static String authenticateUser(InteractiveShell shell, AuthenticationService authenticationService) {
        while (true) {
            printAuthenticationMenu(authenticationService);
            String choice = shell.readPlain("Choice:");
            if (choice == null) {
                return null;
            }

            switch (choice.toLowerCase()) {
            case "1":
            case "login":
                String loggedInUsername = handleLogin(shell, authenticationService);
                if (loggedInUsername != null) {
                    return loggedInUsername;
                }
                break;
            case "2":
            case "register":
                String registeredUsername = handleRegistration(shell, authenticationService);
                if (registeredUsername != null) {
                    return registeredUsername;
                }
                break;
            case "3":
            case "exit":
                CliVisuals.printInfo("Exiting Crypto1010.");
                return null;
            default:
                CliVisuals.printError(ACCOUNT_SELECTION_ERROR);
            }
        }
    }

    private static void printAuthenticationMenu(AuthenticationService authenticationService) {
        CliVisuals.printAuthMenu(authenticationService.hasRegisteredAccounts());
    }

    private static String handleLogin(InteractiveShell shell, AuthenticationService authenticationService) {
        if (!authenticationService.hasRegisteredAccounts()) {
            CliVisuals.printError("Error: No accounts registered yet. Choose register first.");
            return null;
        }

        String username = shell.readPlain("Username:");
        String password = shell.readSecret("Password:");
        if (username == null || password == null) {
            return null;
        }

        try {
            String authenticatedUsername = authenticationService.authenticate(username, password);
            CliVisuals.printSuccess("Login successful. Logged in as " + authenticatedUsername + ".");
            return authenticatedUsername;
        } catch (AuthenticationException e) {
            CliVisuals.printError(e.getMessage());
            return null;
        }
    }

    private static String handleRegistration(InteractiveShell shell, AuthenticationService authenticationService) {
        String username = shell.readPlain("Choose username:");
        String password = shell.readSecret("Choose password:");
        String passwordConfirmation = shell.readSecret("Confirm password:");
        if (username == null || password == null || passwordConfirmation == null) {
            return null;
        }

        try {
            String registeredUsername = authenticationService.register(username, password, passwordConfirmation);
            CliVisuals.printSuccess("Registration successful. Logged in as " + registeredUsername + ".");
            return registeredUsername;
        } catch (AuthenticationException | IOException e) {
            CliVisuals.printError(e.getMessage());
            return null;
        }
    }

    private static LoadResult<Blockchain> loadBlockchain(BlockchainStorage storage) {
        try {
            return new LoadResult<>(storage.load(), true);
        } catch (IOException e) {
            CliVisuals.printWarning("Failed to load blockchain data. Starting with default blockchain.");
            return new LoadResult<>(Blockchain.createDefault(), false);
        }
    }

    private static LoadResult<WalletManager> loadWalletManager(WalletStorage storage) {
        try {
            return new LoadResult<>(storage.load(), true);
        } catch (IOException e) {
            CliVisuals.printWarning("Failed to load wallet data. Starting with empty wallet list.");
            return new LoadResult<>(new WalletManager(), false);
        }
    }

    private static void saveData(
            BlockchainStorage blockchainStorage,
            WalletStorage walletStorage,
            Blockchain blockchain,
            WalletManager walletManager,
            boolean allowBlockchainSave,
            boolean allowWalletSave) {
        if (allowBlockchainSave) {
            try {
                blockchainStorage.save(blockchain);
            } catch (IOException e) {
                CliVisuals.printWarning("Failed to save blockchain data.");
            }
        }
        if (allowWalletSave) {
            try {
                walletStorage.save(walletManager);
            } catch (IOException e) {
                CliVisuals.printWarning("Failed to save wallet data.");
            }
        }
    }

    private static UiDirective handleMetaCommand(
            String message,
            InteractiveShell shell,
            CliRuntimeState runtimeState,
            String accountUsername,
            WalletManager walletManager,
            Blockchain blockchain) {
        String payload = message.substring(1).trim();
        if (payload.isEmpty()) {
            CliVisuals.printError("Error: Empty UI command. Use :help");
            return UiDirective.CONTINUE;
        }

        String[] parts = payload.split("\\s+", 2);
        String uiCommand = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        switch (uiCommand) {
        case "help":
            CliVisuals.printUiControlGuide();
            CliVisuals.printHint("Themes available: " + CliVisuals.themeNames());
            break;
        case "theme":
            if (argument.isBlank()) {
                CliVisuals.printError("Error: Use :theme <" + CliVisuals.themeNames() + ">");
                break;
            }
            if (CliVisuals.setTheme(argument)) {
                CliVisuals.printSuccess("Theme changed to " + CliVisuals.getThemeName() + ".");
            } else {
                CliVisuals.printError("Unknown theme. Use one of: " + CliVisuals.themeNames());
            }
            break;
        case "themes":
            CliVisuals.printHint("Available themes: " + CliVisuals.themeNames());
            break;
        case "plain":
            applyPlainMode(argument);
            break;
        case "mode":
            InteractiveShell.InputMode mode = shell.parseMode(argument);
            if (mode == null) {
                CliVisuals.printError("Unknown mode. Use: normal, compose, search, history");
            } else {
                runtimeState.setInputMode(mode);
                CliVisuals.printSuccess("Input mode set to " + mode.label() + ".");
            }
            break;
        case "timeline":
            CliVisuals.printTimeline(runtimeState.timeline());
            break;
        case "search":
            if (argument.isBlank()) {
                CliVisuals.printError("Error: Use :search <text>");
                break;
            }
            List<String> matches = shell.searchHistory(argument, 12);
            CliVisuals.printSearchResults(argument, matches);
            break;
        case "status":
            CliVisuals.printStatusDashboard(accountUsername, walletManager, blockchain);
            break;
        case "home":
            CliVisuals.printAppBanner();
            CliVisuals.printStatusDashboard(accountUsername, walletManager, blockchain);
            CliVisuals.printUiControlGuide();
            shell.printCommandPalette();
            break;
        case "clear":
            CliVisuals.clearScreen();
            CliVisuals.printAppBanner();
            break;
        case "confirm":
            applyConfirmationMode(runtimeState, argument);
            break;
        case "exit":
        case "quit":
            return UiDirective.EXIT;
        default:
            CliVisuals.printError("Unknown UI command: :" + uiCommand + ". Use :help");
        }
        return UiDirective.CONTINUE;
    }

    private static void applyPlainMode(String argument) {
        if (argument.equalsIgnoreCase("toggle")) {
            CliVisuals.setPlainMode(!CliVisuals.isPlainMode());
            CliVisuals.printSuccess("Plain mode " + (CliVisuals.isPlainMode() ? "enabled." : "disabled."));
            return;
        }
        Boolean enabled = parseOnOff(argument);
        if (enabled == null) {
            CliVisuals.printError("Error: Use :plain <on|off|toggle>");
            return;
        }
        CliVisuals.setPlainMode(enabled);
        CliVisuals.printSuccess("Plain mode " + (enabled ? "enabled." : "disabled."));
    }

    private static void applyConfirmationMode(CliRuntimeState runtimeState, String argument) {
        if (argument.equalsIgnoreCase("toggle")) {
            runtimeState.setConfirmationsEnabled(!runtimeState.confirmationsEnabled());
            CliVisuals.printSuccess("Confirmations "
                    + (runtimeState.confirmationsEnabled() ? "enabled." : "disabled."));
            return;
        }
        Boolean enabled = parseOnOff(argument);
        if (enabled == null) {
            CliVisuals.printError("Error: Use :confirm <on|off|toggle>");
            return;
        }
        runtimeState.setConfirmationsEnabled(enabled);
        CliVisuals.printSuccess("Confirmations " + (enabled ? "enabled." : "disabled."));
    }

    private static Boolean parseOnOff(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("on".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("off".equalsIgnoreCase(value.trim())) {
            return false;
        }
        return null;
    }

    private static CommandRunResult executeCommand(Command command, Blockchain blockchain, Scanner in)
            throws Crypto1010Exception {
        String commandName = command.getClass().getSimpleName();
        if (command instanceof TutorialCommand) {
            long startedAt = System.nanoTime();
            LOGGER.info(() -> "Starting interactive command: " + commandName);
            command.execute(blockchain, in);
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.info(() -> "Finished interactive command: " + commandName + " (" + durationMs + " ms)");
            return new CommandRunResult(
                    List.of("Interactive tutorial executed in passthrough mode."),
                    durationMs,
                    null);
        }

        long startedAt = System.nanoTime();
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        Crypto1010Exception commandError = null;
        LOGGER.fine(() -> "Starting command: " + commandName);
        try (PrintStream captureOut = new PrintStream(outputBuffer, true)) {
            System.setOut(captureOut);
            try {
                command.execute(blockchain, in);
            } catch (Crypto1010Exception e) {
                commandError = e;
            }
        } finally {
            System.setOut(originalOut);
        }
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        List<String> outputLines = Arrays.stream(outputBuffer.toString().split("\\R"))
                .map(String::stripTrailing)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
        LOGGER.fine(() -> "Completed command: " + commandName + " (" + durationMs + " ms)");
        return new CommandRunResult(outputLines, durationMs, commandError);
    }

    private static String normalizeInputAliases(String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("/")) {
            return trimmed;
        }

        String[] parts = trimmed.split("\\s+", 2);
        String alias = parts[0].toLowerCase(Locale.ROOT);
        String rest = parts.length > 1 ? parts[1].trim() : "";

        return switch (alias) {
        case "/h", "/help" -> appendCommandRest(CommandWord.HELP.getCommand(), rest);
        case "/ls", "/list" -> appendCommandRest(CommandWord.LIST.getCommand(), rest);
        case "/vc", "/viewchain" -> appendCommandRest(CommandWord.VIEWCHAIN.getCommand(), rest);
        case "/vb", "/viewblock" -> appendCommandRest(CommandWord.VIEWBLOCK.getCommand(), rest);
        case "/q", "/quit", "/exit" -> CommandWord.EXIT.getCommand();
        case "/mk", "/create" -> rest.isBlank() ? CommandWord.CREATE.getCommand() : "create w/" + rest;
        case "/kb", "/keygen" -> rest.isBlank() ? CommandWord.KEYGEN.getCommand() : "keygen w/" + rest;
        case "/bal", "/balance" -> rest.isBlank() ? CommandWord.BALANCE.getCommand() : "balance w/" + rest;
        case "/snd", "/send" -> appendCommandRest(CommandWord.SEND.getCommand(), rest);
        case "/xs", "/crosssend" -> appendCommandRest(CommandWord.CROSSSEND.getCommand(), rest);
        default -> trimmed;
        };
    }

    private static String appendCommandRest(String command, String rest) {
        if (rest == null || rest.isBlank()) {
            return command;
        }
        return command + " " + rest;
    }

    private static String extractCommandWord(String normalizedInput) {
        if (normalizedInput == null || normalizedInput.isBlank()) {
            return null;
        }
        return normalizedInput.split("\\s+", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static boolean requiresConfirmation(String commandWord) {
        if (commandWord == null) {
            return false;
        }
        return RISKY_COMMANDS.contains(commandWord.toLowerCase(Locale.ROOT));
    }

    private static boolean requestCommandConfirmation(InteractiveShell shell, String commandText) {
        String response = shell.readPlain("Confirm `" + commandText + "` ? [y/N]");
        if (response == null) {
            return false;
        }
        String normalized = response.trim().toLowerCase(Locale.ROOT);
        return "y".equals(normalized) || "yes".equals(normalized);
    }

    private static List<String> walletNames(WalletManager walletManager) {
        return walletManager.getWallets().stream()
                .map(Wallet::getName)
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    private static void addTimelineEntry(Deque<String> timeline, String status, String command, long durationMs) {
        String entry = "[" + nowTimestamp() + "] "
                + status
                + "  "
                + command
                + "  ("
                + durationMs
                + " ms)";
        if (timeline.size() >= MAX_TIMELINE_ENTRIES) {
            timeline.removeFirst();
        }
        timeline.addLast(entry);
    }

    private static String nowTimestamp() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    private record LoadResult<T>(T data, boolean loadedSuccessfully) {
    }

    private record CommandRunResult(List<String> outputLines, long durationMs, Crypto1010Exception error) {
    }

    private enum UiDirective {
        CONTINUE,
        EXIT
    }

    private static final class CliRuntimeState {
        private final Deque<String> timeline;
        private InteractiveShell.InputMode inputMode;
        private boolean confirmationsEnabled;
        private String lastSaveAt;

        private CliRuntimeState() {
            this.timeline = new ArrayDeque<>();
            this.inputMode = InteractiveShell.InputMode.NORMAL;
            this.confirmationsEnabled = true;
            this.lastSaveAt = "--";
        }

        private Deque<String> timeline() {
            return timeline;
        }

        private InteractiveShell.InputMode inputMode() {
            return inputMode;
        }

        private void setInputMode(InteractiveShell.InputMode inputMode) {
            this.inputMode = inputMode;
        }

        private boolean confirmationsEnabled() {
            return confirmationsEnabled;
        }

        private void setConfirmationsEnabled(boolean confirmationsEnabled) {
            this.confirmationsEnabled = confirmationsEnabled;
        }

        private String lastSaveAt() {
            return lastSaveAt;
        }

        private void setLastSaveAt(String lastSaveAt) {
            this.lastSaveAt = lastSaveAt;
        }
    }
}
