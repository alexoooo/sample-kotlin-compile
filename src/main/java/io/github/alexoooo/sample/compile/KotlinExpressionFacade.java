package io.github.alexoooo.sample.compile;

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


public enum KotlinExpressionFacade {;
    //-----------------------------------------------------------------------------------------------------------------
    private static final String classNameBase = "KotlinExpressionFacade";


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
        String logicDigest = Utils.hash(logic);
        String className = classNameBase + "_" + logicDigest;
        String sourceCode = generateSupplier(logic, className);

        Path sourceFile = Path.of(".cache", className + ".kt");
        Files.createDirectories(sourceFile.getParent());
        Files.write(
                sourceFile,
                sourceCode.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        Path jarFile = Path.of(".cache", className + ".jar");

        List<Path> currentClassPath = Utils.currentClassPath();

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


    //-----------------------------------------------------------------------------------------------------------------
    private static String generateSupplier(String logic, String className) {
        return "import java.util.function.Supplier\n" +
                "class " + className + ": Supplier<Any> {\n" +
                "    override fun get(): Any = run {\n" +
                logic + "\n" +
                "    }\n" +
                "}";
    }
}
