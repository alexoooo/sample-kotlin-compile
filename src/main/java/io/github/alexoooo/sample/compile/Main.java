package io.github.alexoooo.sample.compile;


public class Main {
    //-----------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        Utils.RuntimeStatus runtimeStatus = new Utils.RuntimeStatus();
        for (int i = 0; i < 1_000; i++) {
            runtimeStatus.printStatus();

            int value = Integer.parseInt(KotlinExpressionFacade.execute(
                    "0 + " + i
            ).toString());

            if (i != value) {
                throw new IllegalStateException(value + " (" + i + " expected)");
            }
        }
    }
}
