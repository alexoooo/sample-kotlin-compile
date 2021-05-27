package io.github.alexoooo.sample.compile;

import java.nio.file.Paths;


public class Main {
    private static final String className = "Codegen";
    private static long previousUsedMemory = 0;


    public static void main(String[] args) {
        KotlinRuntimeCompiler compiler = new KotlinRuntimeCompiler(
                Paths.get("target/codegen"));

        for (int i = 0; i < 10_000; i++) {
            printStatus(i);

            String code = generateSampleCode();
            ClassLoader classLoader = new ClassLoader(Main.class.getClassLoader()) {};
            compiler.compileOnDisk(className, code, classLoader);
        }
    }


    private static void printStatus(int index) {
        Runtime runtime = Runtime.getRuntime();

        runtime.gc();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        long delta = usedMemory - previousUsedMemory;
        previousUsedMemory = usedMemory;

        System.out.println("i = " + index + " / used memory = " + usedMemory + " / delta = " + delta);
    }


    private static String generateSampleCode() {
        String logic = "1 + 1";

        return """
                import java.util.function.Supplier
                
                class Codegen: Supplier<Int> {
                    override fun get(): Int = run {
                        """ + logic + """
                    
                    }
                }
                """;
    }
}
