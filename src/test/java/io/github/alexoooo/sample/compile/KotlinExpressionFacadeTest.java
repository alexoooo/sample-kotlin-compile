package io.github.alexoooo.sample.compile;


import io.github.alexoooo.sample.compile.compiler.KotlinCompilerExpressionFacade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class KotlinExpressionFacadeTest {
    @Test
    public void onePlusOneEqualsTwo() {
//        int onePlusOne = Integer.parseInt(KotlinExpressionFacade.execute(
        int onePlusOne = Integer.parseInt(KotlinCompilerExpressionFacade.execute(
                "1 + 1"
        ).toString());

        assertEquals(2, onePlusOne);
    }


    @Test
    public void malformedExpression() {
        assertThrows(
                RuntimeException.class,
                () -> KotlinCompilerExpressionFacade.execute("1 + \"1\""));
    }
}
