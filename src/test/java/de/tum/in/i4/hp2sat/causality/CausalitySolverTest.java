package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.testutil.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Constant;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;

import static org.junit.Assert.*;

public class CausalitySolverTest {
    FormulaFactory f;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
    }

    @Test
    public void Should_FulfillAC1Only_When_BTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.verum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, false, false), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.verum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAC1AndAC2Only_When_BTAndSTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.verum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("ST")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, false), causalitySolverResult);
    }

    @Test
    public void Should_NotFulfillACs_When_NotBTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.verum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(false, false, false), causalitySolverResult);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInBillySuzy_When_BillyAndSuzyThrow() throws Exception{
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.verum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));

        Set<Literal> evaluationExpected = new HashSet<>(Arrays.asList(f.literal("BT_exo", true),
                f.literal("ST_exo", true), f.literal("BT", true),
                f.literal("ST", true), f.literal("BH", false),
                f.literal("SH", true), f.literal("BS", true)));
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInBillySuzy_When_SuzyThrowsOnly() throws Exception{
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.falsum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));

        Set<Literal> evaluationExpected = new HashSet<>(Arrays.asList(f.literal("BT_exo", false),
                f.literal("ST_exo", true), f.literal("BT", false),
                f.literal("ST", true), f.literal("BH", false),
                f.literal("SH", true), f.literal("BS", true)));
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInArsonistsDisjunctive_When_LightningOnly() throws Exception{
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("L_exo"), f.verum());
        context.put(f.variable("MD_exo"), f.falsum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("FF")));

        Set<Literal> evaluationExpected = new HashSet<>(Arrays.asList(f.literal("L_exo", true),
                f.literal("MD_exo", false), f.literal("L", true),
                f.literal("MD", false), f.literal("FF", true)));
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(arsonists, context);

        assertEquals(evaluationExpected, evaluationActual);
    }
}