package io.github.alexoooo.sample.compile.compiler;

import io.github.alexoooo.sample.compile.KotlinExpressionUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Supplier;


public enum KotlinCompilerExpressionFacade {;
    //-----------------------------------------------------------------------------------------------------------------
    private static final String className = "Codegen";


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


    private static Object executeChecked(String logic)
            throws
                IOException,
                ClassNotFoundException,
                NoSuchMethodException,
                InvocationTargetException,
                InstantiationException,
                IllegalAccessException
    {
        String sourceCode = KotlinExpressionUtils.generateSupplier(logic, className);
        String digest = KotlinExpressionUtils.hash(sourceCode);

        Path sourceFile = Path.of(".cache", className + "_" + digest + ".kt");
        Files.createDirectories(sourceFile.getParent());
        Files.write(
                sourceFile,
                sourceCode.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        Path jarFile = Path.of(".cache", className + "_" + digest + ".jar");

        List<Path> currentClassPath = KotlinExpressionUtils.currentClassPath();

        KotlinCompilerFacade.Result result = KotlinCompilerFacade.compile(sourceFile, jarFile, currentClassPath);

        if (! Files.exists(jarFile)) {
            throw new IllegalStateException("Compile failed: " + result);
        }

        try (URLClassLoader jarClassLoader = new URLClassLoader(
                new URL[] {jarFile.toUri().toURL()}))
        {
            @SuppressWarnings("unchecked")
            var clazz = (Class<Supplier<?>>) jarClassLoader.loadClass(className);

            Supplier<?> logicInstance = clazz.getDeclaredConstructor().newInstance();

            return logicInstance.get();
        }
        finally {
            Files.deleteIfExists(jarFile);
            Files.deleteIfExists(sourceFile);
        }
    }
}
