package seedu.crypto1010;

/** Handles all user interface output for the Crypto1010 application. */
public class Ui {
    private Ui() {
    }

    /**
     * Prints a message followed by a newline to the standard output.
     *
     * @param message the message to print
     */
    public static void println(String message) {
        System.out.println(message);
    }

    /**
     * Prints an empty line to the standard output.
     */
    public static void println() {
        System.out.println();
    }

    /**
     * Prints a message to the standard output without a trailing newline.
     *
     * @param message the message to print
     */
    public static void print(String message) {
        System.out.print(message);
    }
}
