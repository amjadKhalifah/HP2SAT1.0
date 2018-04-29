package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.util.ExampleProvider;
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
    SATCausalitySolver satCausalitySolver;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
        satCausalitySolver = new SATCausalitySolver();
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_BTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
        assertEquals(new CausalitySolverResult(true, false, true, cause, null), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("BH", false)))), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_AIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("F");
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(dummyModel, context, phi, cause, SolvingStrategy.STANDARD);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("B", false), f.literal("G", false)))),
                causalitySolverResult);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_STIsCauseBSAndBH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.and(f.variable("BS"), f.variable("BH"));
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
        // TODO different result as for eval causality solver! not minimal!
        assertEquals(new CausalitySolverResult(false, true, true, cause,
                new HashSet<>(Arrays.asList(f.variable("BS"), f.literal("BH", false)))),
                causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_SHIsCauseBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
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
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
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
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
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
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
        assertEquals(new CausalitySolverResult(false, false, true, cause, null), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseBSOrSH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.or(f.variable("BS"), f.variable("SH"));
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
        assertEquals(new CausalitySolverResult(true, true, true, cause,
                new HashSet<>(Collections.singletonList(f.literal("BH", false)))), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_LIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(arsonists, context, phi, cause, SolvingStrategy.STANDARD);
        assertEquals(new CausalitySolverResult(true, false, true, cause, null), causalitySolverResult);
    }

    @Test
    public void Should_FulfillAllAcs_When_LAndMDIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(arsonists, context, phi, cause, SolvingStrategy.STANDARD);
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
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
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
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(billySuzy, context, phi, cause, SolvingStrategy.STANDARD);
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
        CausalitySolverResult causalitySolverResult = satCausalitySolver.solve(guns, context, phi, cause, SolvingStrategy.STANDARD);
        assertEquals(new CausalitySolverResult(true, true, true, cause, new HashSet<>()),
                causalitySolverResult);
    }
}