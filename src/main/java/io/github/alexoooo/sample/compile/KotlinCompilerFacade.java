package io.github.alexoooo.sample.compile;

import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public enum KotlinCompilerFacade {;
    //-----------------------------------------------------------------------------------------------------------------
    public record Result(
            ExitCode exitCode,
            List<Error> errors
    ) {}


    public record Error(
            String message,
            CompilerMessageSourceLocation location
    ) {}


    //-----------------------------------------------------------------------------------------------------------------
    public static Result compile(Path sourceFile, Path outputJar, List<Path> classPath) {
        K2JVMCompilerArguments compilerArguments = buildCompilerArguments(sourceFile, outputJar, classPath);

        MessageCollectorImpl messageCollector = new MessageCollectorImpl();
        K2JVMCompiler k2JvmCompiler = new K2JVMCompiler();

        ExitCode exitCode = k2JvmCompiler.exec(messageCollector, Services.EMPTY, compilerArguments);

        List<Error> errors = messageCollector
                .getErrors()
                .stream()
                .map(i -> new Error(i.getMessage(), i.getLocation()))
                .toList();

        return new Result(exitCode, errors);
    }


    private static K2JVMCompilerArguments buildCompilerArguments(
            Path sourceFile,
            Path outputJar,
            List<Path> classPath
    ) {
        String sourceFileArg = sourceFile.toAbsolutePath().normalize().toString();

        K2JVMCompilerArguments cliArgs = new K2JVMCompilerArguments();

        String outputJarArg = outputJar.toAbsolutePath().normalize().toString();
        cliArgs.setDestination(outputJarArg);

        String classPathArg = classPath.stream()
                .map(i -> i.toAbsolutePath().normalize().toString())
                .collect(Collectors.joining(File.pathSeparator));
        cliArgs.setClasspath(classPathArg);

        cliArgs.setNoReflect(true);

        List<String> freeArgs = List.of(sourceFileArg);
        cliArgs.setFreeArgs(freeArgs);

        return cliArgs;
    }
}
