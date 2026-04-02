package seedu.crypto1010.model;

import java.math.BigInteger;
import java.security.SecureRandom;
import seedu.crypto1010.Ui;
import seedu.crypto1010.exceptions.Crypto1010Exception;

public class Key {
    private static final String GENERATE_START = "Starting key generation...";
    private static final String PRIME_P_DISPLAY = "Prime P = ";
    private static final String PRIME_Q_DISPLAY = "Prime Q = ";
    private static final String MODULUS_DISPLAY = "Modulus = ";
    private static final String TOTIENT_DISPLAY = "Totient = ";
    private static final String PRIVATE_EXP_DISPLAY = "Private exponent = ";
    private static final String KEY_GENERATION_FAIL_ERROR = "Error: Key pair verification failed";

    private static final int KEY_SIZE = 1024;
    private static final int MAX_UNTRUNCATED_LENGTH = 20;
    private static final int TRUNCATED_PART_LENGTH = 10;
    private static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BigInteger modulus;
    private final BigInteger exponent;
    private final boolean isPublic;
    private final String walletAddress;

    public Key(BigInteger modulus, BigInteger exponent, boolean isPublic) throws Crypto1010Exception {
        this.modulus = modulus;
        this.exponent = exponent;
        this.isPublic = isPublic;
        this.walletAddress = isPublic ? deriveAddress(modulus, exponent) : null;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public static Key[] generateKeyPair() throws Crypto1010Exception {
        Ui.println(GENERATE_START);

        // Creates primeP
        BigInteger primeP = BigInteger.probablePrime(KEY_SIZE, RANDOM);
        Ui.println(PRIME_P_DISPLAY + truncate(primeP));

        // Recreates primeQ until it is different from primeP
        BigInteger primeQ;
        do {
            primeQ = BigInteger.probablePrime(KEY_SIZE, RANDOM);
        } while (primeQ.equals(primeP));
        Ui.println(PRIME_Q_DISPLAY + truncate(primeQ));


        // Creates public modulus, Euler's totient, and private exponent for private key
        BigInteger modulus = primeP.multiply(primeQ);
        Ui.println(MODULUS_DISPLAY + truncate(modulus));

        BigInteger totient = primeP.subtract(BigInteger.ONE).multiply(primeQ.subtract(BigInteger.ONE));
        Ui.println(TOTIENT_DISPLAY + truncate(totient));

        BigInteger privateExponent = PUBLIC_EXPONENT.modInverse(totient);
        Ui.println(PRIVATE_EXP_DISPLAY + truncate(privateExponent));

        // Ensures math checks out
        BigInteger check = PUBLIC_EXPONENT.multiply(privateExponent).mod(totient);
        if (!check.equals(BigInteger.ONE)) {
            throw new Crypto1010Exception(KEY_GENERATION_FAIL_ERROR);
        }

        // Creates Keys for key pair
        Key publicKey = new Key(modulus, PUBLIC_EXPONENT, true);
        Key privateKey = new Key(modulus, privateExponent, false);
        return new Key[]{publicKey, privateKey};
    }

    public static String truncate(BigInteger bigInt) {
        String intRepresentation = bigInt.toString();
        if (intRepresentation.length() <= MAX_UNTRUNCATED_LENGTH) {
            return intRepresentation;
        }
        return intRepresentation.substring(0, TRUNCATED_PART_LENGTH) + "..."
                + intRepresentation.substring(intRepresentation.length() - TRUNCATED_PART_LENGTH);
    }

    private static String deriveAddress(BigInteger modulus, BigInteger exponent) {
        BigInteger combined = modulus.add(exponent);
        // multiply by a large prime to spread bits, then take mod to get exactly 160 bits
        BigInteger spread = combined.multiply(BigInteger.valueOf(6364136223846793005L))
                .abs()
                .mod(BigInteger.TWO.pow(160));
        // pad to exactly 40 hex characters
        return "0x" + String.format("%040x", spread);
    }
}
