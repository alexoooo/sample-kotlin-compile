package io.github.alexoooo.sample.compile;


public class Main {
    private static long previousUsedMemory = 0;
    private static long previousTime = System.currentTimeMillis();


    public static void main(String[] args) {
        for (int i = 0; i < 1_000; i++) {
            printStatus(i);

            int value = Integer.parseInt(KotlinExpressionFacade.execute(
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
}
