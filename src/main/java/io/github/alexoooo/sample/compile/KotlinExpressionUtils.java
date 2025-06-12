package io.github.alexoooo.sample.compile;

import io.github.alexoooo.sample.compile.compiler.KotlinCompilerFacade;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


public enum KotlinExpressionUtils {;
    //-----------------------------------------------------------------------------------------------------------------
    private static final String hashName = "SHA3-256";


    //-----------------------------------------------------------------------------------------------------------------
    public static String generateSupplier(String logic, String supplierClassName) {
        return "import java.util.function.Supplier\n" +
                "class " + supplierClassName + ": Supplier<Any> {\n" +
                "    override fun get(): Any = run {\n" +
                logic + "\n" +
                "    }\n" +
                "}";
    }


    //-----------------------------------------------------------------------------------------------------------------
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


    //-----------------------------------------------------------------------------------------------------------------
    public static List<Path> currentClassPath() {
        Set<String> classPathComponents = new LinkedHashSet<>();
        classPathComponents.addAll(systemClassPath());
        classPathComponents.addAll(jarManifestClassPath());

        List<Path> builder = new ArrayList<>();
        for (String classpath : classPathComponents) {
            Path classpathFile = Path.of(classpath).toAbsolutePath().normalize();
            builder.add(classpathFile);
        }
        return builder;
    }


    private static List<String> systemClassPath()  {
        String classpath = System.getProperty("java.class.path");
        String[] classpathParts = classpath.split(File.pathSeparator);
        return Arrays.asList(classpathParts);
    }


    private static List<String> jarManifestClassPath() {
        try {
            return jarManifestClassPathChecked();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> jarManifestClassPathChecked() throws IOException, URISyntaxException {
        URL jarUrl = KotlinExpressionUtils.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
        if (! jarUrl.toString().endsWith(".jar")) {
            return List.of();
        }

        File jarFile = new File(jarUrl.toURI());
        try (JarFile jf = new JarFile(jarFile)) {
            Manifest mf = jf.getManifest();
            String cp = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (cp == null) {
                return List.of();
            }
            return Arrays.asList(cp.split("\\s+"));
        }
    }
}
