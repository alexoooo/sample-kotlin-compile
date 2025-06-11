package io.github.alexoooo.sample.compile;

import kotlin.script.experimental.api.ScriptDiagnostic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;


public enum KotlinExpressionFacade {;
    //-----------------------------------------------------------------------------------------------------------------
    private static final String hashName = "SHA3-256";

    private static final String className = "Codegen";
    private static final String nestedClassName = KotlinCompilerFacade.classNamePrefix + className;


    //-----------------------------------------------------------------------------------------------------------------
    public static Object execute(String logic) {
        try {
            return executeChecked(logic);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static Object executeChecked(String logic) throws Exception
    {
        String sourceCode = generateSupplier(logic);
        String digest = hash(sourceCode);

        Path jarFile = Path.of(".cache", className + "_" + digest + ".jar");

        List<ScriptDiagnostic> diagnostics = KotlinCompilerFacade.compileScriptToJarFile(
                sourceCode,
                Thread.currentThread().getContextClassLoader(),
                jarFile);

        if (! Files.exists(jarFile)) {
            throw new IllegalStateException("Compile failed: " + diagnostics);
        }

        try (URLClassLoader jarClassLoader = new URLClassLoader(
                new URL[] {jarFile.toUri().toURL()}))
        {
            @SuppressWarnings("unchecked")
            var clazz = (Class<Supplier<?>>) jarClassLoader.loadClass(nestedClassName);

            Supplier<?> logicInstance = clazz.getDeclaredConstructor().newInstance();

            return logicInstance.get();
        }
        finally {
            Files.delete(jarFile);
        }
    }


    private static String generateSupplier(String logic) {
        return "import java.util.function.Supplier\n" +
                "class " + className + ": Supplier<Any> {\n" +
                "    override fun get(): Any = run {\n" +
                logic + "\n" +
                "    }\n" +
                "}";
    }


    //-----------------------------------------------------------------------------------------------------------------
    private static String hash(String text)
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
