package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.testutil.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import java.util.*;

import static org.junit.Assert.*;

public class CausalitySolverTest {
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
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, false, true, cause, null), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("BH", false)))), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_SHIsCauseBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("BH", false)))), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_NotBTIsCauseForNotBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
        Formula phi = f.literal("BS", false);
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause, new HashSet<>()),
                causalitySolverResult);
    }

    @Test
    public void Should_FulfillAC1AndAC2Only_When_BTAndSTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("ST")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, false, cause, new HashSet<>()),
                causalitySolverResult);
    }

    @Test
    public void Should_NotFulfillACs_When_NotBTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(false, false, true, cause, null), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseBSOrSH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.or(f.variable("BS"), f.variable("SH"));
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("BH", false)))), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_STIsCauseBSAndBH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.and(f.variable("BS"), f.variable("BH"));
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(false, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("ST", true)))), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_LIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(arsonists, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, false, true, cause, null), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllAcs_When_LAndMDIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(arsonists, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause, new HashSet<>()),
                causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseForBSInExtendedModelWITHOUTWind() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true),
                f.literal("W_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("BH", false)))), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAC2AC3_When_STIsCauseForBSInExtendedModelWITHWind() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true),
                f.literal("W_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(billySuzy, context, phi, cause);
        assertEquals(new CausalitySolverResult(false, true, true, cause, new HashSet<>()),
                causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_CIsCauseForDGivenAAndNotB() throws Exception {
        CausalModel guns = ExampleProvider.guns();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");
        CausalitySolverResult causalitySolverResult = CausalitySolver.solve(guns, context, phi, cause);
        assertEquals(new CausalitySolverResult(true, true, true, cause, new HashSet<>()),
                causalitySolverResult);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInBillySuzy_When_BillyAndSuzyThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));

        Set<Literal> evaluationExpected = new HashSet<>(Arrays.asList(f.literal("BT_exo", true),
                f.literal("ST_exo", true), f.literal("BT", true),
                f.literal("ST", true), f.literal("BH", false),
                f.literal("SH", true), f.literal("BS", true)));
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInBillySuzy_When_SuzyThrowsOnly() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", true)));

        Set<Literal> evaluationExpected = new HashSet<>(Arrays.asList(f.literal("BT_exo", false),
                f.literal("ST_exo", true), f.literal("BT", false),
                f.literal("ST", true), f.literal("BH", false),
                f.literal("SH", true), f.literal("BS", true)));
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInArsonistsDisjunctive_When_LightningOnly() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));

        Set<Literal> evaluationExpected = new HashSet<>(Arrays.asList(f.literal("L_exo", true),
                f.literal("MD_exo", false), f.literal("L", true),
                f.literal("MD", false), f.literal("FF", true)));
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(arsonists, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnAllCauses_WhenSuzyBillyThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Formula phi = f.variable("BS");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>(Arrays.asList(
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("ST"))),
                        new HashSet<>(Collections.singletonList(f.literal("BH", false)))),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("SH"))),
                        new HashSet<>(Collections.singletonList(f.literal("BH", false)))),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("BS"))), new HashSet<>())));
        Set<CausalitySolverResult> allCausesActual = CausalitySolver.getAllCauses(billySuzy, context, phi);
        assertEquals(allCausesExpected, allCausesActual);
    }

    @Test
    public void Should_ReturnNoCause_WhenSuzyBillyDoNotThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Formula phi = f.variable("BS");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>();
        Set<CausalitySolverResult> allCausesActual = CausalitySolver.getAllCauses(billySuzy, context, phi);
        assertEquals(allCausesExpected, allCausesActual);
    }

    @Test
    public void Should_ReturnAllCauses_WhenLandMDInConjunctiveScenario() throws Exception{
        CausalModel arsonists = ExampleProvider.arsonists(false);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Formula phi = f.variable("FF");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>(Arrays.asList(
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("L"))), new HashSet<>()),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("MD"))), new HashSet<>()),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("FF"))), new HashSet<>())));
        Set<CausalitySolverResult> allCausesActual = CausalitySolver.getAllCauses(arsonists, context, phi);
        assertEquals(allCausesExpected, allCausesActual);
    }

    @Test
    public void Should_ReturnAllCauses_WhenLandMDInDisjunctiveScenario() throws Exception{
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Formula phi = f.variable("FF");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>(Arrays.asList(
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD"))), new HashSet<>()),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("FF"))), new HashSet<>())));
        Set<CausalitySolverResult> allCausesActual = CausalitySolver.getAllCauses(arsonists, context, phi);
        assertEquals(allCausesExpected, allCausesActual);
    }
}