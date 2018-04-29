package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.testutil.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class SATCausalitySolverTest {
    FormulaFactory f;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_BTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = SATCausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, false, true, cause, null), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = SATCausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("BH", false)))), causalitySolverResult);
    }

}