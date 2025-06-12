package io.github.alexoooo.sample.compile.script;

import kotlin.Unit;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.script.experimental.annotations.KotlinScript;
import kotlin.script.experimental.api.*;
import kotlin.script.experimental.host.ConfigurationFromTemplateKt;
import kotlin.script.experimental.host.ScriptingHostConfiguration;
import kotlin.script.experimental.host.StringScriptSource;
import kotlin.script.experimental.jvm.*;
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache;
import kotlin.script.experimental.util.PropertiesCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


@KotlinScript
public enum KotlinScriptFacade
{;
    //-----------------------------------------------------------------------------------------------------------------
    public static final String classNamePrefix = "Script$";


    private static final KotlinType baseClassType = new KotlinType(
            JvmClassMappingKt.getKotlinClass(KotlinScriptFacade.class));

    private static final KClass<?> contextClass =
            JvmClassMappingKt.getKotlinClass(ScriptCompilationConfiguration.class);


    //-----------------------------------------------------------------------------------------------------------------
    public static List<ScriptDiagnostic> compileScriptToJarFile(
            String sourceCode,
            ClassLoader classLoader,
            Path jarFile
    ) {
        ScriptCompilationConfiguration scriptCompilationConfiguration =
                ConfigurationFromTemplateKt.createCompilationConfigurationFromTemplate(
                        baseClassType,
                        JvmScriptingHostConfigurationKt.getDefaultJvmScriptingHostConfiguration(),
                        contextClass,
                        builder -> {
                            buildScriptCompilationConfiguration(builder, classLoader, jarFile);
                            return Unit.INSTANCE;
                        }
                );

        SourceCode sourceCodeModel = new StringScriptSource(sourceCode, null);

        ScriptJvmCompilerIsolated scriptJvmCompilerIsolated = new ScriptJvmCompilerIsolated(
                JvmScriptingHostConfigurationKt.getDefaultJvmScriptingHostConfiguration());

        ResultWithDiagnostics<CompiledScript> result = scriptJvmCompilerIsolated.compile(
                sourceCodeModel, scriptCompilationConfiguration);

        return result.getReports();
    }


    //-----------------------------------------------------------------------------------------------------------------
    private static void buildScriptCompilationConfiguration(
            ScriptCompilationConfiguration.Builder builder,
            ClassLoader classLoader,
            Path jarFile
    ) {
        JvmScriptCompilationConfigurationBuilder jvmScriptCompilationConfigurationBuilder =
                JvmScriptCompilationKt.getJvm(builder);

        JvmScriptCompilationKt.dependenciesFromClassloader(
                jvmScriptCompilationConfigurationBuilder,
                new String[] {},
                classLoader,
                true,
                false
        );

        builder.getData().putAll(jvmScriptCompilationConfigurationBuilder.getData());

        PropertiesCollection.Key<ScriptingHostConfiguration> scriptingHostConfigurationKey =
                ScriptCompilationKt.getHostConfiguration(builder);

        try {
            Files.createDirectories(jarFile.getParent());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        ScriptingHostConfiguration scriptingHostConfiguration = scriptingHostConfiguration(jarFile);

        builder.set(scriptingHostConfigurationKey, scriptingHostConfiguration);
    }


    private static @NotNull ScriptingHostConfiguration scriptingHostConfiguration(Path jarFile) {
        File cacheFile = jarFile.toFile();

        return new ScriptingHostConfiguration(
                new ScriptingHostConfiguration[] {
                        JvmScriptingHostConfigurationKt.getDefaultJvmScriptingHostConfiguration()
                },
                scriptHostConfigurationBuilder -> {
                    JvmScriptingHostConfigurationBuilder jvmScriptingHostConfigurationBuilder =
                            JvmScriptingHostConfigurationKt.getJvm(scriptHostConfigurationBuilder);

                    PropertiesCollection.Key<CompiledJvmScriptsCache> compiledJvmScriptsCacheKey =
                            JvmScriptCachingKt.getCompilationCache(jvmScriptingHostConfigurationBuilder);

                    jvmScriptingHostConfigurationBuilder.set(
                            compiledJvmScriptsCacheKey,
                            new CompiledScriptJarsCache((sourceCode, scriptCompilationConfiguration) ->
                                    cacheFile));

                    scriptHostConfigurationBuilder.getData().putAll(
                            jvmScriptingHostConfigurationBuilder.getData());

                    return Unit.INSTANCE;
                }
        );
    }
}
