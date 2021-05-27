package io.github.alexoooo.sample.compile;


import kotlin.script.experimental.jvm.util.KotlinJars;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot;
import org.jetbrains.kotlin.cli.common.environment.UtilKt;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.config.JvmTarget;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static kotlin.script.experimental.jvm.util.JvmClasspathUtilKt.classpathFromClassloader;


// see: https://discuss.kotlinlang.org/t/embeddable-kotlin-compiler-long-term-memory-leak/16653
// see: https://github.com/nekkiy/dynamic-kotlin/blob/master/src/main/kotlin/com/example/KotlinDynamicCompiler.kt
public class KotlinRuntimeCompiler
{
    //-----------------------------------------------------------------------------------------------------------------
    private static final Path srcDirName = Paths.get("src");
    private static final Path binDirName = Paths.get("bin");


    static {
        UtilKt.setIdeaIoUseFallback();
    }


    //-----------------------------------------------------------------------------------------------------------------
    private final Path workDir;


    //-----------------------------------------------------------------------------------------------------------------
    public KotlinRuntimeCompiler(Path workDir) {
        this.workDir = workDir;
    }


    //-----------------------------------------------------------------------------------------------------------------
    public void compileOnDisk(
            String className, String sourceCode, ClassLoader classLoader)
    {
        String hash = Utils.hash(sourceCode);
        String uniqueName = className + "_" + hash;
        Path codeDir = workDir.resolve(uniqueName);

        compileInDir(className, sourceCode, codeDir, classLoader);
    }


    private void compileInDir(
            String className,
            String sourceCode,
            Path codeDir,
            ClassLoader classLoader)
    {
        Utils.deleteDir(codeDir);

        Path codeSrcDir = codeDir.resolve(srcDirName);
        Path codeBinDir = codeDir.resolve(binDirName);
        writeCode(className, sourceCode, codeSrcDir, codeBinDir);

        Optional<String> error = compileModule(
                className,
                List.of(codeSrcDir.toString()),
                codeBinDir,
                classLoader);

        if (error.isPresent()) {
            throw new IllegalStateException(error.get());
        }
    }


    private void writeCode(
            String className,
            String sourceCode,
            Path codeSrcDir,
            Path codeBinDir
    ) {
        Path sourceFile = codeSrcDir.resolve(className + ".kt");

        try {
            Files.createDirectories(codeSrcDir);
            Files.createDirectories(codeBinDir);
            Files.writeString(sourceFile, sourceCode);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private Optional<String> compileModule(
            String moduleName,
            List<String> sourcePaths,
            Path saveClassesDir,
            ClassLoader classLoader
    ) {
        ByteArrayOutputStream errorStreamBytes = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(errorStreamBytes);

        CompilerConfiguration configuration = configureCompiler(
                moduleName, sourcePaths, saveClassesDir, classLoader, errorStream);

        StubDisposable noopDisposable = new StubDisposable();
        KotlinCoreEnvironment env = KotlinCoreEnvironment.createForProduction(
                noopDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        GenerationState result = KotlinToJVMBytecodeCompiler.INSTANCE.analyzeAndGenerate(env);
        errorStream.flush();

        Disposer.dispose(noopDisposable);

        if (result != null) {
            return Optional.empty();
        }

        String error = errorStreamBytes.toString(StandardCharsets.UTF_8);

        return Optional.of(error);
    }


    private CompilerConfiguration configureCompiler(
            String moduleName,
            List<String> sourcePaths,
            Path saveClassesDir,
            ClassLoader classLoader,
            PrintStream errStream)
    {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName);

        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                new PrintingMessageCollector(errStream, MessageRenderer.PLAIN_FULL_PATHS, true));

        configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, saveClassesDir.toFile());
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_16);

        addClasspathForStdlibAndClassloader(configuration, classLoader);

        for (var sourcePath : sourcePaths) {
            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, new KotlinSourceRoot(sourcePath, false));
        }

        return configuration;
    }


    private void addClasspathForStdlibAndClassloader(
            CompilerConfiguration configuration, ClassLoader classLoader)
    {
        Collection<File> classloaderClasspath = classpathFromClassloader(classLoader, false);
        assert classloaderClasspath != null;

        Set<File> classPath = new LinkedHashSet<>(classloaderClasspath);

        classPath.add(KotlinJars.INSTANCE.getStdlib());

        for (var file : classPath) {
            configuration.add(CLIConfigurationKeys.CONTENT_ROOTS, new JvmClasspathRoot(file));
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    private static class StubDisposable implements Disposable {
        @Override
        public void dispose() {
        }
    }
}
