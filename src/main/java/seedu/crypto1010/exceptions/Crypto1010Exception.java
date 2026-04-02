package seedu.crypto1010.exceptions;

import seedu.crypto1010.Ui;

/**
 * Custom exception type used throughout the Crypto1010 application.
 * Allows user-friendly and personality-filled error messages.
 */
public class Crypto1010Exception extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@code Crypto1010Exception} with the given message.
     *
     * @param message detail message describing the error
     */
    public Crypto1010Exception(String message) {
        super(message);
    }

    /**
     * Prints an error message to the console.
     *
     * @param message the error message to print
     */
    public static void printError(String message) {
        Ui.println(message);
    }
}
