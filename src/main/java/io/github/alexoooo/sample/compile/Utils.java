package io.github.alexoooo.sample.compile;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public enum Utils {;
    private static final String hashName = "SHA3-256";


    public static String hash(String text)
    {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(hashName);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        byte[] encodedHash = digest.digest(
                text.getBytes(StandardCharsets.UTF_8));

        return new BigInteger(1, encodedHash).toString(16);
    }
}
