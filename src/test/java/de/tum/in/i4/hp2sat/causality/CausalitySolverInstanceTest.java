package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.util.ExampleProvider;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.SATSolverType.GLUCOSE;
import static de.tum.in.i4.hp2sat.causality.SATSolverType.MINISAT;
import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CausalitySolverInstanceTest {
    FormulaFactory f;
    BruteForceCausalitySolver bruteForceCausalitySolver;
    SATCausalitySolver SATCausalitySolver;
    List<SolvingStrategy> solvingStrategies = Arrays.asList(BRUTE_FORCE, BRUTE_FORCE_OPTIMIZED_W, SAT,
            SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL,
            SAT_OPTIMIZED_CLAUSES, SAT_OPTIMIZED_CLAUSES_MINIMAL, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_AC3_MINIMAL);
    List<SATSolverType> satSolverTypes = Arrays.asList(MINISAT, GLUCOSE);

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
        bruteForceCausalitySolver = new BruteForceCausalitySolver();
        SATCausalitySolver = new SATCausalitySolver();
    }

    private void testSolve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
                           CausalitySolverResult causalitySolverResultExpected) throws Exception {
        // all have same expected result
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected = solvingStrategies.stream()
                .collect(Collectors.toMap(Function.identity(), s -> new HashSet<>
                        (Collections.singletonList(causalitySolverResultExpected))));
        testSolve(causalModel, context, phi, cause, causalitySolverResultsExpected);
    }

    private void testSolve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
                           Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected) throws
            Exception {
        for (SolvingStrategy solvingStrategy : solvingStrategies) {
            CausalitySolverResult causalitySolverResultActual = null;
            if (solvingStrategy == BRUTE_FORCE || solvingStrategy == BRUTE_FORCE_OPTIMIZED_W) {
                causalitySolverResultActual =
                        bruteForceCausalitySolver.solve(causalModel, context, phi, cause, solvingStrategy);
            } else if (Arrays.asList(SAT, SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_W,
                    SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_CLAUSES, SAT_OPTIMIZED_CLAUSES_MINIMAL, SAT_OPTIMIZED_AC3,
                    SAT_OPTIMIZED_AC3_MINIMAL)
                    .contains(solvingStrategy)) {
                for (SATSolverType satSolverType : satSolverTypes) {
                    causalitySolverResultActual = SATCausalitySolver.solve(causalModel, context, phi, cause,
                            solvingStrategy, satSolverType);
                    Matcher[] matchers = causalitySolverResultsExpected.get
                            (solvingStrategy).stream().map(CoreMatchers::is).toArray(Matcher[]::new);
                    assertThat("Error for " + solvingStrategy + "/" + satSolverType, causalitySolverResultActual,
                            CoreMatchers.anyOf(matchers));
                }

                causalitySolverResultActual =
                        SATCausalitySolver.solve(causalModel, context, phi, cause, solvingStrategy);
            }
            Matcher[] matchers = causalitySolverResultsExpected.get
                    (solvingStrategy).stream().map(CoreMatchers::is).toArray(Matcher[]::new);
            assertThat("Error for " + solvingStrategy, causalitySolverResultActual,
                    CoreMatchers.anyOf(matchers));
        }
    }

    private void testGetAllCauses(CausalModel causalModel, Set<Literal> context, Formula phi,
                                  Set<CausalitySolverResult> causalitySolverResultsExpected) throws Exception {
        // all have same expected result
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpectedMap = solvingStrategies.stream()
                .collect(Collectors.toMap(Function.identity(), s -> causalitySolverResultsExpected));
        testGetAllCauses(causalModel, context, phi, causalitySolverResultsExpectedMap);
    }

    private void testGetAllCauses(CausalModel causalModel, Set<Literal> context, Formula phi,
                                  Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected)
            throws Exception {
        for (SolvingStrategy solvingStrategy : solvingStrategies) {
            Set<CausalitySolverResult> causalitySolverResultsActual = null;
            if (solvingStrategy == BRUTE_FORCE || solvingStrategy == BRUTE_FORCE_OPTIMIZED_W) {
                causalitySolverResultsActual =
                        bruteForceCausalitySolver.getAllCauses(causalModel, context, phi, solvingStrategy, f);
            } else if (Arrays.asList(SAT, SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_W,
                    SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_CLAUSES, SAT_OPTIMIZED_CLAUSES_MINIMAL, SAT_OPTIMIZED_AC3,
                    SAT_OPTIMIZED_AC3_MINIMAL)
                    .contains(solvingStrategy)) {
                causalitySolverResultsActual =
                        SATCausalitySolver.getAllCauses(causalModel, context, phi, solvingStrategy, f);
            }
            assertEquals("Error for " + solvingStrategy, causalitySolverResultsExpected.get(solvingStrategy),
                    causalitySolverResultsActual);
        }
    }

    // #################################################################################################################
    // ################################################ ROCK-THROWING ##################################################
    // #################################################################################################################
    //region ROCK-THROWING
    //region [ROCK-THROWING] ST_exo = 1; BT_exo = 1
    @Test
    public void Should_FulfillAllACs_When_ST_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };

        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_BT_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_NotFulfillACs_When_NotBT_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_SH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("ST"),
                                f.literal("BH", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };

        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_STandBT_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_STandSH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("SH")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));

                    }
                };

        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_BTandNotBH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false)));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_SHandBH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("SH"), f.variable("BH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1Only_When_SHandNotBH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("SH"), f.literal("BH", false)));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_STandBTandSH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT"),
                f.variable("SH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1Only_When_STandBTandNotBH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT"),
                f.literal("BH", false)));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_ST_IsCauseFor_BSOrSH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.or(f.variable("BS"), f.variable("SH"));

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };

        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_ST_IsCauseFor_BSAndBH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.and(f.variable("BS"), f.variable("BH"));

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(false, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("SH", true))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(false, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL1 = causalitySolverResultExpectedEval;
        // TODO check if it really makes sense that we can put a part of X into W
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL2 =
                new CausalitySolverResult(false, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                causalitySolverResultExpectedSATMINIMAL2)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                        causalitySolverResultExpectedSATMINIMAL2)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                        causalitySolverResultExpectedSATMINIMAL2)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                        causalitySolverResultExpectedSATMINIMAL2)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                        causalitySolverResultExpectedSATMINIMAL2)));

                    }
                };

        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_BT_IsCauseFor_TRUE() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.verum();
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAC2AC3Only_When_BTIsCauseForFALSE() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.falsum();
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ROCK-THROWING] ST_exo = 1; BT_exo = 0
    @Test
    public void Should_FulfillAllACs_When_ST_IsCauseFor_BS_Given_NotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_STandBT_IsCauseFor_BS_Given_NotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ROCK-THROWING] ST_exo = 0; BT_exo = 1
    @Test
    public void Should_FulfillAllACs_When_BT_IsCauseFor_BS_Given_NotSTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1A2Only_When_BTandBH_IsCauseFor_BS_Given_NotSTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("BH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ROCK-THROWING] ST_exo = 0; BT_exo = 0
    @Test
    public void Should_FulfillAllACs_When_NotST_IsCauseFor_NotBS_Given_NotSTExoAndNotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("ST", false)));
        Formula phi = f.literal("BS", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotBT_IsCauseFor_NotBS_Given_NotSTExoAndNotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
        Formula phi = f.literal("BS", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_NotSTandNotBT_IsCauseFor_NotBS_Given_NotSTExoAndNotBTExo()
            throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("ST", false),
                f.literal("BT", false)));
        Formula phi = f.literal("BS", false);
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotST_IsCauseFor_NotSHandNotBS_Given_NotSTExoAndNotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("ST", false)));
        Formula phi = f.and(f.not(f.variable("SH")), f.not(f.variable("BS")));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotBT_IsCauseFor_NotSHandNotBS_Given_NotSTExoAndNotBTExo()
            throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
        Formula phi = f.and(f.not(f.variable("SH")), f.not(f.variable("BS")));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ########################################## ROCK-THROWING (END) ##################################################
    // #################################################################################################################

    @Test
    public void Should_FulfillAC1AC3Only_When_LIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(arsonists, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAcs_When_LAndMDIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(arsonists, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAC2AC3Only_When_LAndMDIsCauseForFF_GivenNotLExoAndNotMDExo() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(arsonists, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseForBSInExtendedModelWITHOUTWind() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true),
                f.literal("W_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };

        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC2AC3_When_STIsCauseForBSInExtendedModelWITHWind() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true),
                f.literal("W_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_CIsCauseForDGivenAAndNotB() throws Exception {
        CausalModel guns = ExampleProvider.guns();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_AIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("B", false), f.literal("G", false),
                                f.literal("H", false))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_AandBIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("C")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("G", false), f.literal("H", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("B", false), f.literal("G", false),
                                f.literal("H", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };
        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_AIsCauseForCInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("B", false))));
        // IMPORTANT: There are many more possibilities for W in this case, but these 2 seem sufficent for passing
        CausalitySolverResult causalitySolverResultExpectedSAT1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("B", false), f.literal("G", false),
                                f.literal("H", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT2 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("F"),
                                f.literal("B", false), f.literal("H", false))));
        Set<CausalitySolverResult> causalitySolverResultsExpectedSAT = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, causalitySolverResultsExpectedSAT);
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, causalitySolverResultsExpectedSAT);
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W, causalitySolverResultsExpectedSAT);
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3, causalitySolverResultsExpectedSAT);
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };
        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_AAndDIsCauseForCInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("D")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("B", false))));
        // IMPORTANT: There are many more possibilities for W in this case, but these 2 seem sufficent for passing
        CausalitySolverResult causalitySolverResultExpectedSAT1 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("B", false), f.literal("G", false),
                                f.literal("H", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT2 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.variable("F"),
                                f.literal("B", false), f.literal("H", false))));
        Set<CausalitySolverResult> causalitySolverResultsExpectedSAT = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, causalitySolverResultsExpectedSAT);
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, causalitySolverResultsExpectedSAT);
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W, causalitySolverResultsExpectedSAT);
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3, causalitySolverResultsExpectedSAT);
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };
        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_BIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAC3Only_When_GAndHIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("G"), f.variable("H")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAC3Only_When_NotGAndNotHIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("G", false),
                f.literal("H", false)));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_B1IsCauseForXInDummyModel2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy2();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true), f.literal("D_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B1")));
        Formula phi = f.variable("X");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D1", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("D1", false),
                                f.literal("C1", false), f.literal("D2", false),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C1", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL2 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL3 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D1", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL4 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D2", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                causalitySolverResultExpectedSATMINIMAL2, causalitySolverResultExpectedSATMINIMAL3,
                                causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                causalitySolverResultExpectedSATMINIMAL2, causalitySolverResultExpectedSATMINIMAL3,
                                causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                    }
                };

        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllAC1AC2Only_When_A1AndB1IsCauseForXInDummyModel2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy2();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true), f.literal("D_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("B1"), f.variable("A1")));
        Formula phi = f.variable("X");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D1", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("D1", false),
                                f.literal("C1", false), f.literal("D2", false),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL1 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C1", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL2 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL3 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D1", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL4 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D2", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                causalitySolverResultExpectedSATMINIMAL2, causalitySolverResultExpectedSATMINIMAL3,
                                causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                causalitySolverResultExpectedSATMINIMAL2, causalitySolverResultExpectedSATMINIMAL3,
                                causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                    }
                };

        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_B1IsCauseForYInDummyModel2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy2();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true), f.literal("D_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B1")));
        Formula phi = f.variable("Y");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C1", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("D1", false),
                                f.literal("C1", false), f.literal("D2", false),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT_OPTIMIZED_CLAUSES =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("C1", false),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C1", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL2 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL3 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D1", false))));
        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL4 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D2", false))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                causalitySolverResultExpectedSATMINIMAL2, causalitySolverResultExpectedSATMINIMAL3,
                                causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
                                causalitySolverResultExpectedSATMINIMAL2, causalitySolverResultExpectedSATMINIMAL3,
                                causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_CLAUSES, new HashSet<>(
                                Collections.singletonList(causalitySolverResultExpectedSAT_OPTIMIZED_CLAUSES)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                        put(SAT_OPTIMIZED_AC3, new HashSet<>(
                                Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL, new HashSet<>(Arrays.asList(
                                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4)));
                    }
                };

        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_AIsCauseForCInDummyXORModel_GivenNotB() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    // TODO does it make sense that phi is in W?
    @Test
    public void Should_FulfillAC2AC3Only_When_AIsCauseForCInDummyXORModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(false, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(false, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("C", false), f.variable("B"))));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_CLAUSES,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };
        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_AandBIsCauseForCInDummyXORModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_AandBIsCauseForCInDummyXORModel_GivenNotAExo() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1Only_When_AandBIsCauseForCInDummyXNORModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXNOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_InBenchmarkModels() throws Exception {
        CausalModel benchmarkModel = ExampleProvider.benchmarkModel();
        // all exogenous variables are true
        Set<Literal> context = benchmarkModel.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("S")));
        Formula phi = f.variable("A");

        // test for real SAT approach only as for the others this is taking too long
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        CausalitySolverResult causalitySolverResultActual = SATCausalitySolver.solve(benchmarkModel, context, phi,
                cause, SolvingStrategy.SAT);
        assertEquals(causalitySolverResultExpected, causalitySolverResultActual);

        CausalModel binaryTreeBenchmarkModelDepth7 = ExampleProvider.generateBinaryTreeBenchmarkModel(7);
        CausalModel binaryTreeBenchmarkModelDepth8 = ExampleProvider.generateBinaryTreeBenchmarkModel(8);
        CausalModel binaryTreeBenchmarkModelDepth9 = ExampleProvider.generateBinaryTreeBenchmarkModel(9);
        Formula phiBenchmarkModelBinaryTree = f.variable("0");

        CausalitySolverResult causalitySolverResultExpectedDepth7 =
                new CausalitySolverResult(true, false, true,
                        new HashSet<>(Collections.singletonList(f.variable("254"))), null);
        CausalitySolverResult causalitySolverResultActualDepth7 =
                SATCausalitySolver.solve(binaryTreeBenchmarkModelDepth7,
                        binaryTreeBenchmarkModelDepth7.getExogenousVariables().stream().map(e -> (Literal) e)
                                .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree,
                        new HashSet<>(Collections.singletonList(f.variable("254"))), SolvingStrategy.SAT);
        assertEquals(causalitySolverResultExpectedDepth7, causalitySolverResultActualDepth7);

        CausalitySolverResult causalitySolverResultExpectedDepth8 =
                new CausalitySolverResult(true, false, true,
                        new HashSet<>(Collections.singletonList(f.variable("510"))), null);
        CausalitySolverResult causalitySolverResultActualDepth8 =
                SATCausalitySolver.solve(binaryTreeBenchmarkModelDepth8,
                        binaryTreeBenchmarkModelDepth8.getExogenousVariables().stream().map(e -> (Literal) e)
                                .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree,
                        new HashSet<>(Collections.singletonList(f.variable("510"))), SolvingStrategy.SAT);
        assertEquals(causalitySolverResultExpectedDepth8, causalitySolverResultActualDepth8);

        CausalitySolverResult causalitySolverResultExpectedDepth9 =
                new CausalitySolverResult(true, false, true,
                        new HashSet<>(Collections.singletonList(f.variable("1022"))), null);
        CausalitySolverResult causalitySolverResultActualDepth9 =
                SATCausalitySolver.solve(binaryTreeBenchmarkModelDepth9,
                        binaryTreeBenchmarkModelDepth9.getExogenousVariables().stream().map(e -> (Literal) e)
                                .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree,
                        new HashSet<>(Collections.singletonList(f.variable("1022"))), SolvingStrategy.SAT);
        assertEquals(causalitySolverResultExpectedDepth9, causalitySolverResultActualDepth9);
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
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context, f);

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
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context, f);

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
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(arsonists, context, f);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnAllCauses_WhenSuzyBillyThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Formula phi = f.variable("BS");
        Set<CausalitySolverResult> allCausesExpectedEval = new HashSet<>(Arrays.asList(
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("ST"))),
                        new HashSet<>(Collections.singletonList(f.literal("BH", false)))),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("SH"))),
                        new HashSet<>(Collections.singletonList(f.literal("BH", false)))),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("BS"))), new HashSet<>())));
        Set<CausalitySolverResult> allCausesExpectedSAT = new HashSet<>(Arrays.asList(
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("ST"))),
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false)))),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("SH"))),
                        new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT"),
                                f.literal("BH", false)))),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("BS"))), new HashSet<>())));
        Map<SolvingStrategy, Set<CausalitySolverResult>> allCausesExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, allCausesExpectedEval);
                        put(BRUTE_FORCE_OPTIMIZED_W, allCausesExpectedEval);
                        put(SAT, allCausesExpectedSAT);
                        put(SAT_MINIMAL, allCausesExpectedEval);
                        put(SAT_COMBINED, allCausesExpectedSAT);
                        put(SAT_COMBINED_MINIMAL, allCausesExpectedEval);
                        put(SAT_OPTIMIZED_W, allCausesExpectedSAT);
                        put(SAT_OPTIMIZED_W_MINIMAL, allCausesExpectedEval);
                        put(SAT_OPTIMIZED_CLAUSES, allCausesExpectedSAT);
                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL, allCausesExpectedEval);
                        put(SAT_OPTIMIZED_AC3, allCausesExpectedSAT);
                        put(SAT_OPTIMIZED_AC3_MINIMAL, allCausesExpectedEval);
                    }
                };

        testGetAllCauses(billySuzy, context, phi, allCausesExpected);
    }

    @Test
    public void Should_ReturnNoCause_WhenSuzyBillyDoNotThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Formula phi = f.variable("BS");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>();

        testGetAllCauses(billySuzy, context, phi, allCausesExpected);
    }

    @Test
    public void Should_ReturnAllCauses_WhenLandMDInConjunctiveScenario() throws Exception {
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

        testGetAllCauses(arsonists, context, phi, allCausesExpected);
    }

    @Test
    public void Should_ReturnAllCauses_WhenLandMDInDisjunctiveScenario() throws Exception {
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Formula phi = f.variable("FF");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>(Arrays.asList(
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD"))), new HashSet<>()),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("FF"))), new HashSet<>())));

        testGetAllCauses(arsonists, context, phi, allCausesExpected);
    }
}