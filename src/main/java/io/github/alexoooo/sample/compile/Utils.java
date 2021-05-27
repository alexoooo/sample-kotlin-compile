package io.github.alexoooo.sample.compile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;


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


    public static void deleteDir(Path dir)
    {
        if (! Files.exists(dir)) {
            return;
        }

        try {
            List<Path> contentsInOrder = Files
                .walk(dir)
                .sorted(Comparator.reverseOrder())
                .toList();

            for (Path next : contentsInOrder) {
                Files.delete(next);
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (Files.exists(dir)) {
            throw new IllegalStateException("Unable to delete: " + dir);
        }
    }
}
