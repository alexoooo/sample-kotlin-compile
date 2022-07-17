package io.github.alexoooo.sample.compile;

import kotlin.script.experimental.api.ScriptDiagnostic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;


public class Main {
    private static final String className = "Codegen";
    private static final String nestedClassName = KotlinCompilerFacade.classNamePrefix + className;

    private static long previousUsedMemory = 0;
    private static long previousTime = System.currentTimeMillis();


    public static void main(String[] args) {
        for (int i = 0; i < 10_000; i++) {
            printStatus(i);

            int value = Integer.parseInt(execute(
                    "0 + " + i
            ).toString());

            if (i != value) {
                throw new IllegalStateException(value + " (" + i + " expected)");
            }
        }
    }


    private static void printStatus(int index) {
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        long delta = usedMemory - previousUsedMemory;
        previousUsedMemory = usedMemory;

        long time = System.currentTimeMillis();
        long duration = time - previousTime;
        previousTime = time;

        System.out.println("i = " + index +
                " / used memory = " + usedMemory +
                " / delta = " + delta +
                " / time = " + duration);
    }


    private static String generateSampleCode() {
        String logic = "1 + 1";

        return "import java.util.function.Supplier\n" +
                "class " + className + ": Supplier<Int> {\n" +
                "    override fun get(): Int = run {\n" +
                        logic + "\n" +
                "    }\n" +
                "}";
    }


    private static <T> T execute(String logic) {
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

    private static <T> T executeChecked(String logic) throws Exception
    {
        String sourceCode = generateSupplier(logic);
        String digest = Utils.hash(sourceCode);

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

            Object value = logicInstance.get();

            @SuppressWarnings("unchecked")
            T uncheckedCast = (T) value;

            return uncheckedCast;
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
}
