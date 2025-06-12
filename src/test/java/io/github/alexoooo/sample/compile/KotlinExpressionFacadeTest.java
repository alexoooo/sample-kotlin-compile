package io.github.alexoooo.sample.compile;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class KotlinExpressionFacadeTest {
    @Test
    public void onePlusOneEqualsTwo() {
        int onePlusOne = Integer.parseInt(KotlinExpressionFacade.execute(
                "1 + 1"
        ).toString());

        assertEquals(2, onePlusOne);
    }


    @Test
    public void malformedExpression() {
        assertThrows(
                RuntimeException.class,
                () -> KotlinExpressionFacade.execute("1 + \"1\""));
    }
}
