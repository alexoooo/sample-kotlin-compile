package io.github.alexoooo.sample.compile.script;

import io.github.alexoooo.sample.compile.KotlinExpressionUtils;
import kotlin.script.experimental.api.ScriptDiagnostic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;


public enum KotlinScriptExpressionFacade {;
    //-----------------------------------------------------------------------------------------------------------------
    private static final String className = "Codegen";
    private static final String nestedClassName = KotlinScriptFacade.classNamePrefix + className;


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
        String sourceCode = KotlinExpressionUtils.generateSupplier(logic, className);
        String digest = KotlinExpressionUtils.hash(sourceCode);

        Path jarFile = Path.of(".cache", className + "_" + digest + ".jar");

        List<ScriptDiagnostic> diagnostics = KotlinScriptFacade.compileScriptToJarFile(
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
}
