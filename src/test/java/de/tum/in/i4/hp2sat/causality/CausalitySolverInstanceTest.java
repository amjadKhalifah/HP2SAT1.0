package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.util.ExampleProvider;
import de.tum.in.i4.hp2sat.util.Util;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.SATSolverType.*;
import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CausalitySolverInstanceTest {
    BruteForceCausalitySolver bruteForceCausalitySolver;
    SATCausalitySolver SATCausalitySolver;
    List<SolvingStrategy> solvingStrategies = Arrays.asList(BRUTE_FORCE, BRUTE_FORCE_OPTIMIZED_W, SAT,
            SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL,
            SAT_OPTIMIZED_FORMULAS, SAT_OPTIMIZED_FORMULAS_MINIMAL, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_AC3_MINIMAL);
    List<SATSolverType> satSolverTypes = Arrays.asList(MINISAT, GLUCOSE, MINICARD, CLEANLING);

    @Before
    public void setUp() throws Exception {
        bruteForceCausalitySolver = new BruteForceCausalitySolver();
        SATCausalitySolver = new SATCausalitySolver();
    }

    private void testSolve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
                           CausalitySolverResult causalitySolverResultExpected, SolvingStrategy... excludedStrategies) throws Exception {
        // all have same expected result
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected = solvingStrategies.stream()
                .collect(Collectors.toMap(Function.identity(), s -> new HashSet<>
                        (Collections.singletonList(causalitySolverResultExpected))));
        testSolve(causalModel, context, phi, cause, causalitySolverResultsExpected, excludedStrategies);
    }

    private void testSolve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
                           Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected,
                           SolvingStrategy... excludedStrategies) throws
            Exception {
        for (SolvingStrategy solvingStrategy : solvingStrategies) {
            if (Arrays.asList(excludedStrategies).contains(solvingStrategy)) {
                continue;
            }
            CausalitySolverResult causalitySolverResultActual = null;
            if (solvingStrategy == BRUTE_FORCE || solvingStrategy == BRUTE_FORCE_OPTIMIZED_W) {
                causalitySolverResultActual =
                        bruteForceCausalitySolver.solve(causalModel, context, phi, cause, solvingStrategy);
            } else if (Arrays.asList(SAT, SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_W,
                    SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_FORMULAS, SAT_OPTIMIZED_FORMULAS_MINIMAL, SAT_OPTIMIZED_AC3,
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
                        bruteForceCausalitySolver.getAllCauses(causalModel, context, phi, solvingStrategy,
                                causalModel.getFormulaFactory());
            } else if (Arrays.asList(SAT, SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_W,
                    SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_FORMULAS, SAT_OPTIMIZED_FORMULAS_MINIMAL, SAT_OPTIMIZED_AC3,
                    SAT_OPTIMIZED_AC3_MINIMAL)
                    .contains(solvingStrategy)) {
                causalitySolverResultsActual =
                        SATCausalitySolver.getAllCauses(causalModel, context, phi, solvingStrategy,
                                causalModel.getFormulaFactory());
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
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
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_NotFulfillACs_When_NotST_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("ST", false)));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_NotFulfillACs_When_NotBT_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_NotFulfillACs_When_NotBH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BH", false)));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_SH_IsCauseFor_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_BT_IsCauseFor_BS_Given_NotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_STAndBT_IsCauseFor_BS_Given_NotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
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
    public void Should_FulfillAC3Only_When_ST_IsCauseFor_BS_Given_NotSTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_BT_IsCauseFor_BS_Given_NotSTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("BH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillA2Only_When_STAndBT_IsCauseFor_BS_Given_NotSTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillACs_When_ST_IsCauseFor_NotBH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.literal("BH", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillACs_When_SH_IsCauseFor_NotBH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Formula phi = f.literal("BH", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_STAndSH_IsCauseFor_NotBH() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("SH")));
        Formula phi = f.literal("BH", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ROCK-THROWING] ST_exo = 0; BT_exo = 0
    @Test
    public void Should_FulfillAllACs_When_NotST_IsCauseFor_NotBS_Given_NotSTExoAndNotBTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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

    // #################################################################################################################
    // ######################################## FOREST FIRE DISJUNCTIVE ################################################
    // #################################################################################################################
    //region FOREST FIRE DISJUNCTIVE
    //region [FOREST FIRE DISJUNCTIVE] L_exo = 1; MD_exo = 1
    @Test
    public void Should_FulfillAC1AC3Only_When_L_IsCauseFor_FF_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_MD_IsCauseFor_FF_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_LAndMD_IsCauseFor_FF_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [FOREST FIRE DISJUNCTIVE] L_exo = 1; MD_exo = 0
    @Test
    public void Should_FulfillAllACs_When_L_IsCauseFor_FF_Given_NotMDExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_MD_IsCauseFor_FF_Given_NotMDExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_LAndMD_IsCauseFor_FF_Given_NotMDExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [FOREST FIRE DISJUNCTIVE] L_exo = 0; MD_exo = 1
    @Test
    public void Should_FulfillAC3Only_When_L_IsCauseFor_FF_Given_NotLExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_MD_IsCauseFor_FF_Given_NotLExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_LAndMD_IsCauseFor_FF_Given_NotLExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [FOREST FIRE DISJUNCTIVE] L_exo = 0; MD_exo = 0
    @Test
    public void Should_FulfillAllACs_When_L_IsCauseFor_FF_Given_NotLExoAndNotMDExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_MD_IsCauseFor_FF_Given_NotLExoAndNotMDExo_DISJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_LAndMD_IsCauseFor_FF_Given_NotLExoAndNotMDExo_DISJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotL_IsCauseFor_NotFF_Given_NotLExoAndNotMDExo_DISJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("L", false)));
        Formula phi = f.literal("FF", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotMD_IsCauseFor_NotFF_Given_NotLExoAndNotMDExo_DISJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("MD", false)));
        Formula phi = f.literal("FF", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_NotLAndNotMD_IsCauseFor_NotFF_Given_NotLExoAndNotMDExo_DISJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("L", false),
                f.literal("MD", false)));
        Formula phi = f.literal("FF", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_LAndMD_IsCauseFor_FF_Given_NotLExoAndNotMDExo_DISJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ###################################### FOREST FIRE DISJUNCTIVE (end) ############################################
    // #################################################################################################################

    // #################################################################################################################
    // ######################################## FOREST FIRE CONJUNCTIVE ################################################
    // #################################################################################################################
    //region FOREST FIRE CONJUNCTIVE
    //region [FOREST FIRE CONJUNCTIVE] L_exo = 1; MD_exo = 1
    @Test
    public void Should_FulfillAllACs_When_L_IsCauseFor_FF_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_MD_IsCauseFor_FF_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_LAndMD_IsCauseFor_FF_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [FOREST FIRE CONJUNCTIVE] L_exo = 1; MD_exo = 0
    @Test
    public void Should_FulfillAllAC2AC3Only_When_L_IsCauseFor_FF_Given_NotMDExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_MD_IsCauseFor_FF_Given_NotMDExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_LAndMD_IsCauseFor_FF_Given_NotMDExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [FOREST FIRE CONJUNCTIVE] L_exo = 0; MD_exo = 1
    @Test
    public void Should_FulfillAC2AC3Only_When_L_IsCauseFor_FF_Given_NotLExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_MD_IsCauseFor_FF_Given_NotLExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_LAndMD_IsCauseFor_FF_Given_NotLExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [FOREST FIRE CONJUNCTIVE] L_exo = 0; MD_exo = 0
    @Test
    public void Should_FulfillAllACs_When_L_IsCauseFor_FF_Given_NotLExoAndNotMDExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_MD_IsCauseFor_FF_Given_NotLExoAndNotMDExo_CONJUNCTIVE() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_LAndMD_IsCauseFor_FF_Given_NotLExoAndNotMDExo_CONJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_NotL_IsCauseFor_NotFF_Given_NotLExoAndNotMDExo_CONJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("L", false)));
        Formula phi = f.literal("FF", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_NotMD_IsCauseFor_NotFF_Given_NotLExoAndNotMDExo_CONJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("MD", false)));
        Formula phi = f.literal("FF", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotLAndNotMD_IsCauseFor_NotFF_Given_NotLExoAndNotMDExo_CONJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("L", false),
                f.literal("MD", false)));
        Formula phi = f.literal("FF", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_LAndMD_IsCauseFor_FF_Given_NotLExoAndNotMDExo_CONJUNCTIVE()
            throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(forestFire, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ###################################### FOREST FIRE CONJUNCTIVE (end) ############################################
    // #################################################################################################################

    // #################################################################################################################
    // ################################################# PRISONERS #####################################################
    // #################################################################################################################
    //region PRISONERS
    //region [PRISONERS] A_exo = 1; B_exo = 1; C_exo = 1
    @Test
    public void Should_FulfillAC1AC3Only_When_A_IsCauseFor_D() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_B_IsCauseFor_D() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAC1AC3Only_When_C_IsCauseFor_D() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_AAndBAndC_IsCauseFor_D() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [PRISONERS] A_exo = 1; B_exo = 1; C_exo = 0
    @Test
    public void Should_FulfillAllACs_When_A_IsCauseFor_D_AExoAndBExoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_B_IsCauseFor_D_AExoAndBExoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAC3Only_When_C_IsCauseFor_D_AExoAndBExoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_AAndBAndC_IsCauseFor_D_AExoAndBExoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [PRISONERS] A_exo = 1; B_exo = 0; C_exo = 1
    @Test
    public void Should_FulfillAC1AC3Only_When_A_IsCauseFor_D_Given_AExoAndNotBexoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_B_IsCauseFor_D_Given_AExoAndNotBexoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAllACs_When_C_IsCauseFor_D_Given_AExoAndNotBexoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
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
    public void Should_FulfillAC2Only_When_AAndBAndC_IsCauseFor_D_Given_AExoAndNotBexoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [PRISONERS] A_exo = 1; B_exo = 0; C_exo = 0
    @Test
    public void Should_FulfillAC2AC3Only_When_A_IsCauseFor_D_Given_AExoAndNotBexoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_B_IsCauseFor_D_Given_AExoAndNotBexoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAC2AC3Only_When_C_IsCauseFor_D_Given_AExoAndNotBexoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_AAndBAndC_IsCauseFor_D_Given_AExoAndNotBexoAndNotCExo()
            throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [PRISONERS] A_exo = 0; B_exo = 1; C_exo = 1
    @Test
    public void Should_FulfillAC3Only_When_A_IsCauseFor_D_Given_NotAExoAndBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_B_IsCauseFor_D_Given_NotAExoAndBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAllACs_When_C_IsCauseFor_D_Given_NotAExoAndBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_AAndBAndC_IsCauseFor_D_Given_NotAExoAndBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [PRISONERS] A_exo = 0; B_exo = 1; C_exo = 0
    @Test
    public void Should_FulfillAC2AC3Only_When_A_IsCauseFor_D_Given_NotAExoAndBExoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_B_IsCauseFor_D_Given_NotAExoAndBExoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAC2AC3Only_When_C_IsCauseFor_D_Given_NotAExoAndBExoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_AAndBAndC_IsCauseFor_D_Given_NotAExoAndBExoAndNotCExo()
            throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [PRISONERS] A_exo = 0; B_exo = 0; C_exo = 1
    @Test
    public void Should_FulfillAC3Only_When_A_IsCauseFor_D_Given_NotAExoAndNotBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAAC3Only_When_B_IsCauseFor_D_Given_NotAExoAndNotBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAllACs_When_C_IsCauseFor_D_Given_NotAExoAndNotBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_AAndBAndC_IsCauseFor_D_Given_NotAExoAndNotBExoAndCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [PRISONERS] A_exo = 0; B_exo = 0; C_exo = 0
    @Test
    public void Should_FulfillAC2AC3Only_When_A_IsCauseFor_D_Given_NotAExoAndNotBexoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_B_IsCauseFor_D_Given_NotAExoAndNotBexoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAC2AC3Only_When_C_IsCauseFor_D_Given_NotAExoAndNotBexoAndNotCExo() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_AAndBAndC_IsCauseFor_D_Given_NotAExoAndNotBexoAndNotCExo()
            throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        FormulaFactory f = guns.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false),
                f.literal("C_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B"),
                f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ############################################### PRISONERS (end) #################################################
    // #################################################################################################################

    // #################################################################################################################
    // ######################################### ASSASSIN FIRST VARIANT ################################################
    // #################################################################################################################
    //region ASSASSIN FIRST VARIANT
    //region [ASSASSIN FIRST VARIANT] A_exo = 1; B_exo = 1
    @Test
    public void Should_FulfillAC3Only_When_NotA_IsCauseFor_VS_Given_AExoAndBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_B_IsCauseFor_VS_Given_AExoAndBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_NotAAndB_IsCauseFor_VS_GivenAExoAndBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ASSASSIN FIRST VARIANT] A_exo = 1; B_exo = 0
    @Test
    public void Should_FulfillAC2AC3Only_When_NotA_IsCauseFor_VS_Given_AExoAndNotBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_B_IsCauseFor_VS_Given_AExoAndNotBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_NotAAndB_IsCauseFor_VS_GivenAExoAndBNotExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ASSASSIN FIRST VARIANT] A_exo = 0; B_exo = 1
    @Test
    public void Should_FulfillAC1AC3Only_When_NotA_IsCauseFor_VS_Given_NotAExoAndBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3_When_B_IsCauseFor_VS_Given_NotAExoAndBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotAAndB_IsCauseFor_VS_GivenNotAExoAndBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ASSASSIN FIRST VARIANT] A_exo = 0; B_exo = 0
    @Test
    public void Should_FulfillAllACs_When_NotA_IsCauseFor_VS_Given_NotAExoAndNotBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_B_IsCauseFor_VS_Given_NotAExoAndNotBExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_NotAAndB_IsCauseFor_VS_GivenNotAExoAndBNotExo_FIRST() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(true);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ###################################### ASSASSIN FIRST VARIANT (end) #############################################
    // #################################################################################################################

    // #################################################################################################################
    // ######################################## ASSASSIN SECOND VARIANT ################################################
    // #################################################################################################################
    //region ASSASSIN SECOND VARIANT
    //region [ASSASSIN SECOND VARIANT] A_exo = 1; B_exo = 1
    @Test
    public void Should_FulfillAC3Only_When_NotA_IsCauseFor_VS_Given_AExoAndBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_B_IsCauseFor_VS_Given_AExoAndBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.variable("A"))));
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_NotAAndB_IsCauseFor_VS_GivenAExoAndBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ASSASSIN SECOND VARIANT] A_exo = 1; B_exo = 0
    @Test
    public void Should_FulfillAllACs_When_NotA_IsCauseFor_VS_Given_AExoAndNotBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_B_IsCauseFor_VS_Given_AExoAndNotBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_NotAAndB_IsCauseFor_VS_GivenAExoAndBNotExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ASSASSIN SECOND VARIANT] A_exo = 0; B_exo = 1
    @Test
    public void Should_FulfillAC1AC3Only_When_NotA_IsCauseFor_VS_Given_NotAExoAndBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3_When_B_IsCauseFor_VS_Given_NotAExoAndBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_NotAAndB_IsCauseFor_VS_GivenNotAExoAndBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [ASSASSIN SECOND VARIANT] A_exo = 0; B_exo = 0
    @Test
    public void Should_FulfillAllACs_When_NotA_IsCauseFor_VS_Given_NotAExoAndNotBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("A", false)));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_B_IsCauseFor_VS_Given_NotAExoAndNotBExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_NotAAndB_IsCauseFor_VS_GivenNotAExoAndBNotExo_SECOND() throws Exception {
        CausalModel assassin = ExampleProvider.assassin(false);
        FormulaFactory f = assassin.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("A", false), f.variable("B")));
        Formula phi = f.variable("VS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(assassin, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ##################################### ASSASSIN SECOND VARIANT (end) #############################################
    // #################################################################################################################

    // #################################################################################################################
    // ################################################ RAILROAD #######################################################
    // #################################################################################################################
    //region RAILROAD
    //region [RAILROAD] LB_exo = 1; F_exo = 1 RB_exo = 1
    @Test
    public void Should_FulfillAC2AC3Only_When_LB_IsCauseFor_NotA() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", true),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_F_IsCauseFor_NotA() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", true),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_RB_IsCauseFor_NotA() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", true),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [RAILROAD] LB_exo = 1; F_exo = 1 RB_exo = 0
    @Test
    public void Should_FulfillAC2AC3Only_When_LB_IsCauseFor_NotA_Given_LBExoAndFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", true),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_F_IsCauseFor_NotA_Given_LBExoAndFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", true),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.variable("A"))));
        // exclude strategies that yield a non-minimal W to ease testing
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected, SAT, SAT_COMBINED,
                SAT_OPTIMIZED_FORMULAS, SAT_OPTIMIZED_W, SAT_OPTIMIZED_AC3);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_RB_IsCauseFor_NotA_Given_LBExoAndFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", true),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [RAILROAD] LB_exo = 1; F_exo = 0 RB_exo = 1
    @Test
    public void Should_FulfillAllACs_When_LB_IsCauseFor_NotA_Given_LBExoAndNotFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", false),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_F_IsCauseFor_NotA_Given_LBExoAndNotFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", false),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_RB_IsCauseFor_NotA_Given_LBExoAndNotFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", false),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [RAILROAD] LB_exo = 1; F_exo = 0 RB_exo = 0
    @Test
    public void Should_FulfillAllACs_When_LB_IsCauseFor_NotA_Given_LBExoAndNotFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", false),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_F_IsCauseFor_NotA_Given_LBExoAndNotFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", false),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_RB_IsCauseFor_NotA_Given_LBExoAndNotFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", true), f.literal("F_exo", false),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [RAILROAD] LB_exo = 0; F_exo = 1 RB_exo = 1
    @Test
    public void Should_FulfillAC3Only_When_LB_IsCauseFor_NotA_Given_NotLBExoAndFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", true),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_F_IsCauseFor_NotA_Given_NotLBExoAndFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", true),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_RB_IsCauseFor_NotA_Given_NotLBExoAndFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", true),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [RAILROAD] LB_exo = 0; F_exo = 1 RB_exo = 0
    @Test
    public void Should_FulfillAC2AC3Only_When_LB_IsCauseFor_NotA_Given_NotLBExoAndFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", true),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_F_IsCauseFor_NotA_Given_NotLBExoAndFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", true),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_RB_IsCauseFor_NotA_Given_NotLBExoAndFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", true),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [RAILROAD] LB_exo = 0; F_exo = 0 RB_exo = 1
    @Test
    public void Should_FulfillAC2AC3Only_When_LB_IsCauseFor_NotA_Given_NotLBExoAndFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", false),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_F_IsCauseFor_NotA_Given_NotLBExoAndFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", false),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_RB_IsCauseFor_NotA_Given_NotLBExoAndFExoAndRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", false),
                f.literal("RB_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [RAILROAD] LB_exo = 0; F_exo = 0 RB_exo = 0
    @Test
    public void Should_FulfillAC2AC3Only_When_LB_IsCauseFor_NotA_Given_NotLBExoAndNotFExoAndNotRBExo()
            throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", false),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("LB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_F_IsCauseFor_NotA_Given_NotLBExoAndNotFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", false),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("F")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2AC3Only_When_RB_IsCauseFor_NotA_Given_NotLBExoAndNotFExoAndNotRBExo()
            throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", false),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("RB")));
        Formula phi = f.literal("A", false);

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_F_IsCauseFor_A_Given_NotLBExoAndNotFExoAndNotRBExo() throws Exception {
        CausalModel railroad = ExampleProvider.railroad();
        FormulaFactory f = railroad.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("LB_exo", false), f.literal("F_exo", false),
                f.literal("RB_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("F", false)));
        Formula phi = f.variable("A");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(railroad, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ############################################# RAILROAD (end) ####################################################
    // #################################################################################################################

    // #################################################################################################################
    // ############################################ STEAL MASTER KEY ###################################################
    // #################################################################################################################
    //region STEAL MASTER KEY
    //region [STEAL MASTER KEY] all exogenous variables 1
    @Test
    public void Should_FulfillAllAC1AC3Only_When_FSU1_IsCauseFor_SMK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U1")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_FSU1AndFNU1_IsCauseFor_SMK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U1"), f.variable("FN_U1")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU1AndFNU1AndAU1_IsCauseFor_SMK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U1"), f.variable("FN_U1"),
                f.variable("A_U1")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>(Arrays.asList(
                        f.literal("DK_U2", false), f.literal("DK_U3", false),
                        f.literal("SD_U2", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU1AndFNU1AndADU1_IsCauseFor_SMK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U1"), f.variable("FN_U1"),
                f.variable("AD_U1")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>(Arrays.asList(
                        f.literal("DK_U2", false), f.literal("DK_U3", false),
                        f.literal("SD_U2", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_FSU2AndFNU2AndAU2_IsCauseFor_SMK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2"),
                f.variable("A_U2")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_FSU3AndFNU3AndAU3_IsCauseFor_SMK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3"),
                f.variable("A_U3")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_FSU1AndFNU1AndAndAU1AndADU1_IsCauseFor_SMK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U1"), f.variable("FN_U1"),
                f.variable("A_U1"), f.variable("AD_U1")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>(Arrays.asList(
                        f.literal("DK_U2", false), f.literal("DK_U3", false),
                        f.literal("SD_U2", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU1AndFNU1_IsCauseFor_DK() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U1"), f.variable("FN_U1")));
        Formula phi = f.variable("DK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>(Arrays.asList(
                        f.literal("DK_U2", false), f.literal("DK_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_AU1_IsCauseFor_SD() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A_U1")));
        Formula phi = f.variable("SD");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>(Arrays.asList(
                        f.literal("SD_U2", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_AU1AndADU1_IsCauseFor_SD() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A_U1"), f.variable("AD_U1")));
        Formula phi = f.variable("SD");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>(Arrays.asList(
                        f.literal("SD_U2", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU1AndFNU1AndAU1_IsCauseFor_DKU1OrSDU1() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (Literal) v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U1"), f.variable("FN_U1"),
                f.variable("A_U1")));
        Formula phi = f.or(f.variable("DK_U1"), f.variable("SD_U1"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion

    //region [STEAL MASTER KEY] all U2/U3 exogenous variables 1
    @Test
    public void Should_FulfillAllAC1AC3Only_When_FSU2_IsCauseFor_SMK_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_FSU2AndFNU2_IsCauseFor_SMK_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU2AndFNU2AndAU2_IsCauseFor_SMK_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2"),
                f.variable("A_U2")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>(Arrays.asList(
                        f.literal("DK_U3", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU2AndFNU2AndADU2_IsCauseFor_SMK_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2"),
                f.variable("AD_U2")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>(Arrays.asList(
                        f.literal("DK_U3", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_FSU3AndFNU3AndAU3_IsCauseFor_SMK_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3"),
                f.variable("A_U3")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_FSU2AndFNU2AndAndAU2AndADU2_IsCauseFor_SMK_Given_U2U3Exos()
            throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2"),
                f.variable("A_U2"), f.variable("AD_U2")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>(Arrays.asList(
                        f.literal("DK_U3", false), f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU2AndFNU2_IsCauseFor_DK_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2")));
        Formula phi = f.variable("DK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("DK_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_AU2_IsCauseFor_SD_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A_U2")));
        Formula phi = f.variable("SD");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_AU2AndADU2_IsCauseFor_SD_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A_U2"), f.variable("AD_U2")));
        Formula phi = f.variable("SD");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("SD_U3", false))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected,
                BRUTE_FORCE, SAT, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_W, SAT_COMBINED, SAT_OPTIMIZED_FORMULAS);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU2AndFNU2AndAU2_IsCauseFor_DKU2OrSDU2_Given_U2U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U2/U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> v.name().contains("U1") ? v.negate() : v).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2"),
                f.variable("A_U2")));
        Formula phi = f.or(f.variable("DK_U2"), f.variable("SD_U2"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion

    //region [STEAL MASTER KEY] all U3 exogenous variables 1
    @Test
    public void Should_FulfillAllAC1AC3Only_When_FSU3_IsCauseFor_SMK_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_FSU3AndFNU3_IsCauseFor_SMK_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU3AndFNU3AndAU3_IsCauseFor_SMK_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3"),
                f.variable("A_U3")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU3AndFNU3AndADU3_IsCauseFor_SMK_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3"),
                f.variable("AD_U3")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_FSU3AndFNU3AndAndAU3AndADU3_IsCauseFor_SMK_Given_U3Exos()
            throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3"),
                f.variable("A_U3"), f.variable("AD_U3")));
        Formula phi = f.variable("SMK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU3AndFNU3_IsCauseFor_DK_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3")));
        Formula phi = f.variable("DK");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllACs_When_AU3_IsCauseFor_SD_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A_U3")));
        Formula phi = f.variable("SD");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_AU3AndADU3_IsCauseFor_SD_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A_U3"), f.variable("AD_U3")));
        Formula phi = f.variable("SD");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_FSU3AndFNU3AndAU3_IsCauseFor_DKU3OrSDU3_Given_U3Exos() throws Exception {
        CausalModel stealMasterKey = ExampleProvider.stealMasterKey();
        FormulaFactory f = stealMasterKey.getFormulaFactory();
        // set all U3 exogenous variables to 1
        Set<Literal> context = stealMasterKey.getExogenousVariables().stream()
                .map(v -> (v.name().contains("U1") || v.name().contains("U2")) ? v.negate() : v)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3"),
                f.variable("A_U3")));
        Formula phi = f.or(f.variable("DK_U3"), f.variable("SD_U3"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ########################################### STEAL MASTER KEY (end) ##############################################
    // #################################################################################################################

    // #################################################################################################################
    // ################################################## LEAKAGE ######################################################
    // #################################################################################################################
    //region LEAKAGE
    //region [LEKAGE] NO Preemption
    @Test
    public void Test_ForAllMinimalCutSets() throws Exception {
        CausalModel leakage = ExampleProvider.leakage(false);
        FormulaFactory f = leakage.getFormulaFactory();
        Util<Literal> util = new Util<>();

        List<List<Variable>> testSets = Arrays.asList(
                Arrays.asList(f.variable("X1"), f.variable("X2")),
                Arrays.asList(f.variable("X3"), f.variable("X11")),
                Arrays.asList(f.variable("X4"), f.variable("X11")),
                Arrays.asList(f.variable("X5"), f.variable("X11")),
                Arrays.asList(f.variable("X6"), f.variable("X11")),
                Arrays.asList(f.variable("X7"), f.variable("X11")),
                Arrays.asList(f.variable("X8"), f.variable("X11")),
                Arrays.asList(f.variable("X9"), f.variable("X11")),
                Arrays.asList(f.variable("X10"), f.variable("X11")),
                Arrays.asList(f.variable("X12"), f.variable("X17")),
                Arrays.asList(f.variable("X13"), f.variable("X17")),
                Arrays.asList(f.variable("X14"), f.variable("X17")),
                Arrays.asList(f.variable("X15"), f.variable("X17")),
                Arrays.asList(f.variable("X16"), f.variable("X17")),
                Arrays.asList(f.variable("X18"), f.variable("X19")),
                Arrays.asList(f.variable("X20"), f.variable("X21")),
                Arrays.asList(f.variable("X22"), f.variable("X23")),
                Arrays.asList(f.variable("X24"), f.variable("X25")),
                Collections.singletonList(f.variable("X26")),
                Arrays.asList(f.variable("X1"), f.variable("X2"), f.variable("X3"),
                        f.variable("X11")),
                Arrays.asList(f.variable("X10"), f.variable("X11"), f.variable("X12"),
                        f.variable("X17"))
        );

        for (List<Variable> testSet : testSets) {
            // negate all variables except for the cause exos
            Set<Literal> context = leakage.getExogenousVariables().stream()
                    .map(v -> testSet.stream().anyMatch(c -> (c.name() + "_exo").equals(v.name())) ? v : v.negate())
                    .collect(Collectors.toSet());
            Formula phi = f.variable("X41");
            List<Set<Literal>> allCauses = util.generatePowerSet(new HashSet<>(testSet)).stream()
                    .filter(s -> s.size() > 0).collect(Collectors.toList());
            for (Set<Literal> cause : allCauses) {
                CausalitySolverResult causalitySolverResultExpected;
                if (testSet.size() <= 2) {

                    if (cause.size() > 1) {
                        causalitySolverResultExpected =
                                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
                    } else {
                        causalitySolverResultExpected =
                                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
                    }
                } else {
                    if (cause.size() == 1) {
                        causalitySolverResultExpected =
                                new CausalitySolverResult(true, false, true, cause, null);
                    } else if (cause.size() == 2) {
                        List<Integer> indices = cause.stream().map(c -> testSet.indexOf(c.variable()))
                                .collect(Collectors.toList());
                        if (indices.containsAll(Arrays.asList(0, 2)) || indices.containsAll(Arrays.asList(0, 3)) ||
                                indices.containsAll(Arrays.asList(1, 2)) || indices.containsAll(Arrays.asList(1,3))) {
                            causalitySolverResultExpected =
                                    new CausalitySolverResult(true, true, true, cause, new HashSet<>());
                        } else {
                            causalitySolverResultExpected =
                                    new CausalitySolverResult(true, false, true, cause, null);
                        }
                    } else {
                        causalitySolverResultExpected =
                                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
                    }
                }
                testSolve(leakage, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
            }
        }
    }
    //endregion
    //region [LEAKAGE] Preemption
    @Test
    public void Should_FulfillAllACs_When_X3_IsCauseFor_X41() throws Exception {
        CausalModel leakage = ExampleProvider.leakage(true);
        FormulaFactory f = leakage.getFormulaFactory();
        // set all exogenous variables to 1
        List<String> contextVars = Arrays.asList("X1_exo", "X2_exo", "X3_exo", "X11_exo", "X24_exo", "X25_exo",
                "X26_exo");
        Set<Literal> context = leakage.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate())
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("X3")));
        Formula phi = f.variable("X41");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("X38", false),
                                f.literal("X26", false), f.literal("X40", false))));
        testSolve(leakage, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE, SAT, SAT_OPTIMIZED_W,
                SAT_OPTIMIZED_FORMULAS, SAT_COMBINED, SAT_OPTIMIZED_AC3);
    }

    @Test
    public void Should_FulfillAllACs_When_X11_IsCauseFor_X41() throws Exception {
        CausalModel leakage = ExampleProvider.leakage(true);
        FormulaFactory f = leakage.getFormulaFactory();
        List<String> contextVars = Arrays.asList("X1_exo", "X2_exo", "X3_exo", "X11_exo", "X24_exo", "X25_exo",
                "X26_exo");
        Set<Literal> context = leakage.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate())
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("X11")));
        Formula phi = f.variable("X41");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("X38", false),
                                f.literal("X26", false), f.literal("X40", false))));
        testSolve(leakage, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE, SAT, SAT_OPTIMIZED_W,
                SAT_OPTIMIZED_FORMULAS, SAT_COMBINED, SAT_OPTIMIZED_AC3);
    }

    @Test
    public void Should_FulfillAllAC1AC2Only_When_X3_And_X11_IsCauseFor_X41() throws Exception {
        CausalModel leakage = ExampleProvider.leakage(true);
        FormulaFactory f = leakage.getFormulaFactory();
        List<String> contextVars = Arrays.asList("X1_exo", "X2_exo", "X3_exo", "X11_exo", "X24_exo", "X25_exo",
                "X26_exo");
        Set<Literal> context = leakage.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate())
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("X3"), f.variable("X11")));
        Formula phi = f.variable("X41");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("X38", false),
                                f.literal("X26", false), f.literal("X40", false))));
        testSolve(leakage, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE, SAT, SAT_OPTIMIZED_W,
                SAT_OPTIMIZED_FORMULAS, SAT_COMBINED, SAT_OPTIMIZED_AC3);
    }

    @Test
    public void Should_FulfillAllAC1AC2Only_When_X1_X2_X3_X11_IsCauseFor_X41() throws Exception {
        CausalModel leakage = ExampleProvider.leakage(true);
        FormulaFactory f = leakage.getFormulaFactory();
        List<String> contextVars = Arrays.asList("X1_exo", "X2_exo", "X3_exo", "X11_exo", "X24_exo", "X25_exo",
                "X26_exo");
        Set<Literal> context = leakage.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate())
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("X1"), f.variable("X2"),
                f.variable("X3"), f.variable("X11")));
        Formula phi = f.variable("X41");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("X26", false),
                                f.literal("X40", false))));
        testSolve(leakage, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE, SAT, SAT_OPTIMIZED_W,
                SAT_OPTIMIZED_FORMULAS, SAT_COMBINED, SAT_OPTIMIZED_AC3);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ############################################### LEAKAGE (end) ###################################################
    // #################################################################################################################

    // #################################################################################################################
    // ############################################## BINARY TREE ######################################################
    // #################################################################################################################
    //region BINARY TREE
    //region [BINARY TREE] height 4 (depth 3)
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L14_IsCauseFor_Root0_DEPTH3() throws Exception {
        CausalModel binaryTreeDepth3 = ExampleProvider.generateBinaryTreeBenchmarkModel(3);
        FormulaFactory f = binaryTreeDepth3.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth3.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("14")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth3, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L13AndL14_IsCauseFor_Root0_DEPTH3() throws Exception {
        CausalModel binaryTreeDepth3 = ExampleProvider.generateBinaryTreeBenchmarkModel(3);
        FormulaFactory f = binaryTreeDepth3.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth3.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("13"), f.variable("14")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth3, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L11AndL12AndL13AndL14_IsCauseFor_Root0_DEPTH3() throws Exception {
        CausalModel binaryTreeDepth3 = ExampleProvider.generateBinaryTreeBenchmarkModel(3);
        FormulaFactory f = binaryTreeDepth3.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth3.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("11"), f.variable("12"),
                f.variable("13"), f.variable("14")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth3, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion

    //region [BINARY TREE] height 5 (depth 4)
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L31_IsCauseFor_Root0_DEPTH4() throws Exception {
        CausalModel binaryTreeDepth4 = ExampleProvider.generateBinaryTreeBenchmarkModel(4);
        FormulaFactory f = binaryTreeDepth4.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth4.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("30")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth4, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L30AndL31_IsCauseFor_Root0_DEPTH4() throws Exception {
        CausalModel binaryTreeDepth4 = ExampleProvider.generateBinaryTreeBenchmarkModel(4);
        FormulaFactory f = binaryTreeDepth4.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth4.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("29"), f.variable("30")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth4, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L27AndL28AndL29AndL30_IsCauseFor_Root0_DEPTH4() throws Exception {
        CausalModel binaryTreeDepth4 = ExampleProvider.generateBinaryTreeBenchmarkModel(4);
        FormulaFactory f = binaryTreeDepth4.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth4.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("27"), f.variable("28"),
                f.variable("29"), f.variable("30")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth4, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion

    //region [BINARY TREE] height 6 (depth 5)
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L62_IsCauseFor_Root0_DEPTH5() throws Exception {
        CausalModel binaryTreeDepth5 = ExampleProvider.generateBinaryTreeBenchmarkModel(5);
        FormulaFactory f = binaryTreeDepth5.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth5.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("62")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth5, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L61AndL62_IsCauseFor_Root0_DEPTH5() throws Exception {
        CausalModel binaryTreeDepth5 = ExampleProvider.generateBinaryTreeBenchmarkModel(5);
        FormulaFactory f = binaryTreeDepth5.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth5.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("61"), f.variable("62")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth5, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L59AndL60AndL61AndL62_IsCauseFor_Root0_DEPTH5() throws Exception {
        CausalModel binaryTreeDepth5 = ExampleProvider.generateBinaryTreeBenchmarkModel(5);
        FormulaFactory f = binaryTreeDepth5.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth5.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("59"), f.variable("60"),
                f.variable("61"), f.variable("62")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth5, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion

    //region [BINARY TREE] height 7 (depth 6)
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L126_IsCauseFor_Root0_DEPTH6() throws Exception {
        CausalModel binaryTreeDepth6 = ExampleProvider.generateBinaryTreeBenchmarkModel(6);
        FormulaFactory f = binaryTreeDepth6.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth6.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("126")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth6, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L125AndL126_IsCauseFor_Root0_DEPTH6() throws Exception {
        CausalModel binaryTreeDepth6 = ExampleProvider.generateBinaryTreeBenchmarkModel(6);
        FormulaFactory f = binaryTreeDepth6.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth6.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("125"), f.variable("126")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth6, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L123AndL124AndL125AndL126_IsCauseFor_Root0_DEPTH6() throws Exception {
        CausalModel binaryTreeDepth6 = ExampleProvider.generateBinaryTreeBenchmarkModel(6);
        FormulaFactory f = binaryTreeDepth6.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth6.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("123"), f.variable("124"),
                f.variable("125"), f.variable("126")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth6, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion

    //region [BINARY TREE] height 8 (depth 7)
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L254_IsCauseFor_Root0_DEPTH7() throws Exception {
        CausalModel binaryTreeDepth7 = ExampleProvider.generateBinaryTreeBenchmarkModel(7);
        FormulaFactory f = binaryTreeDepth7.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth7.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("254")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth7, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L253AndL254_IsCauseFor_Root0_DEPTH7() throws Exception {
        CausalModel binaryTreeDepth7 = ExampleProvider.generateBinaryTreeBenchmarkModel(7);
        FormulaFactory f = binaryTreeDepth7.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth7.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("253"), f.variable("254")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth7, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L251AndL252AndL253AndL254_IsCauseFor_Root0_DEPTH7() throws Exception {
        CausalModel binaryTreeDepth7 = ExampleProvider.generateBinaryTreeBenchmarkModel(7);
        FormulaFactory f = binaryTreeDepth7.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth7.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("251"), f.variable("252"),
                f.variable("253"), f.variable("254")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth7, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion

    //region [BINARY TREE] height 9 (depth 8)
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L510_IsCauseFor_Root0_DEPTH8() throws Exception {
        CausalModel binaryTreeDepth8 = ExampleProvider.generateBinaryTreeBenchmarkModel(8);
        FormulaFactory f = binaryTreeDepth8.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth8.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("510")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth8, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L509AndL510_IsCauseFor_Root0_DEPTH8() throws Exception {
        CausalModel binaryTreeDepth8 = ExampleProvider.generateBinaryTreeBenchmarkModel(8);
        FormulaFactory f = binaryTreeDepth8.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth8.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("509"), f.variable("510")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth8, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L507AndL508AndL509AndL510_IsCauseFor_Root0_DEPTH8() throws Exception {
        CausalModel binaryTreeDepth8 = ExampleProvider.generateBinaryTreeBenchmarkModel(8);
        FormulaFactory f = binaryTreeDepth8.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth8.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("507"), f.variable("508"),
                f.variable("509"), f.variable("510")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth8, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W);
    }
    //endregion

    //region [BINARY TREE] height 10 (depth 9)
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L1022_IsCauseFor_Root0_DEPTH9() throws Exception {
        CausalModel binaryTreeDepth9 = ExampleProvider.generateBinaryTreeBenchmarkModel(9);
        FormulaFactory f = binaryTreeDepth9.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth9.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("1022")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth9, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L1021AndL1022_IsCauseFor_Root0_DEPTH9() throws Exception {
        CausalModel binaryTreeDepth9 = ExampleProvider.generateBinaryTreeBenchmarkModel(9);
        FormulaFactory f = binaryTreeDepth9.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth9.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("1021"), f.variable("1022")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth9, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L1019AndL1020AndL1021AndL1022_IsCauseFor_Root0_DEPTH9()
            throws Exception {
        CausalModel binaryTreeDepth9 = ExampleProvider.generateBinaryTreeBenchmarkModel(9);
        FormulaFactory f = binaryTreeDepth9.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth9.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("1019"), f.variable("1020"),
                f.variable("1021"), f.variable("1022")));
        Formula phi = f.variable("0");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth9, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W);
    }
    //endregion

    //region [BINARY TREE] intermediate phi
    @Test
    public void Should_FulfillAllAC1AC3Only_When_L13AndL14_IsCauseFor_N2_DEPTH3() throws Exception {
        CausalModel binaryTreeDepth3 = ExampleProvider.generateBinaryTreeBenchmarkModel(3);
        FormulaFactory f = binaryTreeDepth3.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth3.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("13"), f.variable("14")));
        Formula phi = f.variable("2");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth3, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L29AndL30_IsCauseFor_N6_DEPTH4() throws Exception {
        CausalModel binaryTreeDepth4 = ExampleProvider.generateBinaryTreeBenchmarkModel(4);
        FormulaFactory f = binaryTreeDepth4.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth4.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("29"), f.variable("30")));
        Formula phi = f.variable("6");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth4, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L61AndL62_IsCauseFor_N14_DEPTH5() throws Exception {
        CausalModel binaryTreeDepth5 = ExampleProvider.generateBinaryTreeBenchmarkModel(5);
        FormulaFactory f = binaryTreeDepth5.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth5.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("61"), f.variable("62")));
        Formula phi = f.variable("14");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth5, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L125AndL126_IsCauseFor_N30_DEPTH6() throws Exception {
        CausalModel binaryTreeDepth6 = ExampleProvider.generateBinaryTreeBenchmarkModel(6);
        FormulaFactory f = binaryTreeDepth6.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth6.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("125"), f.variable("126")));
        Formula phi = f.variable("30");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth6, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L253AndL254_IsCauseFor_N62_DEPTH7() throws Exception {
        CausalModel binaryTreeDepth7 = ExampleProvider.generateBinaryTreeBenchmarkModel(7);
        FormulaFactory f = binaryTreeDepth7.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth7.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("253"), f.variable("254")));
        Formula phi = f.variable("62");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth7, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L509AndL510_IsCauseFor_N126_DEPTH8() throws Exception {
        CausalModel binaryTreeDepth8 = ExampleProvider.generateBinaryTreeBenchmarkModel(8);
        FormulaFactory f = binaryTreeDepth8.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth8.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("509"), f.variable("510")));
        Formula phi = f.variable("126");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth8, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_FulfillAllAC1AC3Only_When_L1021AndL1022_IsCauseFor_N254_DEPTH9() throws Exception {
        CausalModel binaryTreeDepth9 = ExampleProvider.generateBinaryTreeBenchmarkModel(9);
        FormulaFactory f = binaryTreeDepth9.getFormulaFactory();
        // set all exogenous variables to 1
        Set<Literal> context = binaryTreeDepth9.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("1021"), f.variable("1022")));
        Formula phi = f.variable("254");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(binaryTreeDepth9, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ############################################ BINARY TREE (end) ##################################################
    // #################################################################################################################

    // #################################################################################################################
    // ############################################ DUMMY MODEL 1 ######################################################
    // #################################################################################################################
    //region DUMMY MODEL 1
    //region [DUMMY MODEL 1] A_exo = 1, B_exo = 1
    @Test
    public void Should_FulfillAllACs_When_A_IsCause_For_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        FormulaFactory f = dummyModel.getFormulaFactory();
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
    public void Should_FulfillAC1AC2Only_When_AAndC_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        FormulaFactory f = dummyModel.getFormulaFactory();
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
                        /*
                        * even if a non-minimal strategy is applied, the result might be minimal (happens with CLEANLING
                        * */
                        put(SAT, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSAT,
                                causalitySolverResultExpectedEval)));
                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSAT,
                                causalitySolverResultExpectedEval)));
                        put(SAT_COMBINED_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSAT,
                                        causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_W_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_FORMULAS, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSAT,
                                        causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSAT,
                                        causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };
        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC3Only_When_B_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_NotGAndNotH_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        FormulaFactory f = dummyModel.getFormulaFactory();
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
    public void Should_FulfillAllACs_When_A_IsCauseFor_C() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("B", false))));
        // IMPORTANT: There are many more possibilities for W in this case, but these 3 seem sufficent for passing
        CausalitySolverResult causalitySolverResultExpectedSAT1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("B", false), f.literal("G", false),
                                f.literal("H", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT2 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("F"),
                                f.literal("B", false), f.literal("H", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT3 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("B", false), f.variable("D"),
                                f.variable("F"), f.literal("G", false)
                        )));
        Set<CausalitySolverResult> causalitySolverResultsExpectedSAT = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2,
                causalitySolverResultExpectedSAT3));
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3, causalitySolverResultsExpectedSAT);
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };
        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_AAndD_IsCauseFor_C() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("D")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("B", false))));
        // IMPORTANT: There are many more possibilities for W in this case, but these 3 seem sufficent for passing
        CausalitySolverResult causalitySolverResultExpectedSAT1 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("E", false),
                                f.literal("B", false), f.literal("G", false),
                                f.literal("H", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT2 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.variable("F"),
                                f.literal("B", false), f.literal("H", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT3 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.variable("F"),
                                f.literal("B", false), f.literal("G", false))));
        Set<CausalitySolverResult> causalitySolverResultsExpectedSAT = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2,
                causalitySolverResultExpectedSAT3));
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT_OPTIMIZED_AC3, causalitySolverResultsExpectedSAT);
                        put(SAT_OPTIMIZED_AC3_MINIMAL,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                    }
                };
        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllAC3Only_When_GAndH_IsCauseFor_C() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("G"), f.variable("H")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ########################################## DUMMY MODEL 1 (end) ##################################################
    // #################################################################################################################

    // #################################################################################################################
    // ########################################### DUMMY MODEL XNOR ####################################################
    // #################################################################################################################
    //region DUMMY MODEL XNOR
    //region A_exo = 1, B_exo = 1
    @Test
    public void Should_FulfillAC1Only_When_AAndB_IsCauseFor_C() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXNOR();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_A_IsCauseFor_C_XNOR() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXNOR();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_B_IsCauseFor_C_XNOR() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXNOR();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    //endregion
    //endregion
    // #################################################################################################################
    // ######################################### DUMMY MODEL XNOR (end) ################################################
    // #################################################################################################################

    // #################################################################################################################
    // ################################### DUMMY MODEL COMBINED WITH BINARY TREE #######################################
    // #################################################################################################################
    //region DUMMY MODEL COMBINED WITH BINARY TREE
    @Test
    @Ignore
    public void Should_FulfillAC1AC3Only_When_L4094_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("B_exo", true), f.literal("4094_exo", true),
                f.literal("4093_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("4094")));
        Formula phi = f.variable("F");

        // IMPORTANT: THIS TEST CASE WILL FAIL AS WE DO NOT SPECIFY W, BECAUSE IT IS TOO LARGE!!!
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W, SAT_MINIMAL, SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_FORMULAS,
                SAT_OPTIMIZED_FORMULAS_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_AC3_MINIMAL);
    }

    @Test
    @Ignore
    public void Should_FulfillAC1AC3Only_When_L4093_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("B_exo", true), f.literal("4094_exo", true),
                f.literal("4093_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("4093")));
        Formula phi = f.variable("F");

        // IMPORTANT: THIS TEST CASE WILL FAIL AS WE DO NOT SPECIFY W, BECAUSE IT IS TOO LARGE!!!
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W, SAT_MINIMAL, SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_FORMULAS,
                SAT_OPTIMIZED_FORMULAS_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_AC3_MINIMAL);
    }

    @Test
    @Ignore
    public void Should_FulfillAllACs_When_L4093AndL4094_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("B_exo", true), f.literal("4094_exo", true),
                f.literal("4093_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("4093"), f.variable("4094")));
        Formula phi = f.variable("F");

        // IMPORTANT: THIS TEST CASE WILL FAIL AS WE DO NOT SPECIFY W, BECAUSE IT IS TOO LARGE!!!
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W, SAT, SAT_MINIMAL, SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL,
                SAT_OPTIMIZED_FORMULAS, SAT_OPTIMIZED_FORMULAS_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL,
                SAT_OPTIMIZED_AC3_MINIMAL);
    }

    @Test
    @Ignore
    public void Should_FulfillAC1AC2Only_When_NOTL4092AndL4093AndL4094_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("B_exo", true), f.literal("4094_exo", true),
                f.literal("4093_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("4092", false), f.variable("4093"), f.variable
                ("4094")));
        Formula phi = f.variable("F");

        // IMPORTANT: THIS TEST CASE WILL FAIL AS WE DO NOT SPECIFY W, BECAUSE IT IS TOO LARGE!!!
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, null);
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE,
                BRUTE_FORCE_OPTIMIZED_W, SAT, SAT_MINIMAL, SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL,
                SAT_OPTIMIZED_FORMULAS, SAT_OPTIMIZED_FORMULAS_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL,
                SAT_OPTIMIZED_AC3_MINIMAL);
    }
    //endregion
    // #################################################################################################################
    // ################################ DUMMY MODEL COMBINED WITH BINARY TREE (end) ####################################
    // #################################################################################################################

    @Test
    public void Should_FulfillAllACs_When_STIsCauseForBSInExtendedModelWITHOUTWind() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        FormulaFactory f = billySuzy.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
    public void Should_FulfillAllACs_When_B1IsCauseForXInDummyModel2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy2();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true), f.literal("D_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B1")));
        Formula phi = f.variable("X");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D1", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("D1", false),
                                f.literal("C1", false), f.literal("D2", false),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT2 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("B2"), f.variable("Y"),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT_OPTIMIZED_FORMULAS =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("B2"),
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

        Set<CausalitySolverResult> causalitySolverResultsExpectedSAT = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2,
                causalitySolverResultExpectedSAT_OPTIMIZED_FORMULAS));
        Set<CausalitySolverResult> causalitySolverResultsExpectedSATMINIMAL = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_COMBINED, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_COMBINED_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_W_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_AC3, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_AC3_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                    }
                };

        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllAC1AC2Only_When_A1AndB1IsCauseForXInDummyModel2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy2();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true), f.literal("D_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("B1"), f.variable("A1")));
        Formula phi = f.variable("X");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Collections.singletonList(f.literal("D1", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT1 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.literal("D1", false),
                                f.literal("C1", false), f.literal("D2", false),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT2 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.variable("B2"), f.variable("Y"),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT3 =
                new CausalitySolverResult(true, true, false, cause,
                        new HashSet<>(Arrays.asList(f.variable("B2"), f.literal("C2", false))));
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

        Set<CausalitySolverResult> causalitySolverResultsExpectedSAT = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2,
                causalitySolverResultExpectedSAT3));
        Set<CausalitySolverResult> causalitySolverResultsExpectedSATMINIMAL = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_COMBINED, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_COMBINED_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_W_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_AC3, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_AC3_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                    }
                };

        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_B1IsCauseForYInDummyModel2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy2();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true),
                f.literal("C_exo", true), f.literal("D_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B1")));
        Formula phi = f.variable("Y");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("C1", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.literal("D1", false),
                                f.literal("C1", false), f.literal("D2", false),
                                f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT2 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("A2"), f.variable("A1"),
                                f.variable("X"), f.literal("C2", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT_OPTIMIZED_FORMULAS =
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

        Set<CausalitySolverResult> causalitySolverResultsExpectedSAT = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2,
                causalitySolverResultExpectedSATMINIMAL2, causalitySolverResultExpectedSAT_OPTIMIZED_FORMULAS));
        Set<CausalitySolverResult> causalitySolverResultsExpectedSATMINIMAL = new HashSet<>(Arrays.asList(
                causalitySolverResultExpectedSATMINIMAL1, causalitySolverResultExpectedSATMINIMAL2,
                causalitySolverResultExpectedSATMINIMAL3, causalitySolverResultExpectedSATMINIMAL4));
        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
                    {
                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(BRUTE_FORCE_OPTIMIZED_W,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
                        put(SAT, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_COMBINED, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_COMBINED_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_W,
                                new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_W_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                        put(SAT_OPTIMIZED_AC3, new HashSet<>(causalitySolverResultsExpectedSAT));
                        put(SAT_OPTIMIZED_AC3_MINIMAL, new HashSet<>(causalitySolverResultsExpectedSATMINIMAL));
                    }
                };

        testSolve(dummyModel, context, phi, cause, causalitySolverResultsExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_AIsCauseForCInDummyXORModel_GivenNotB() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        FormulaFactory f = dummyModel.getFormulaFactory();
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
        FormulaFactory f = dummyModel.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS,
                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL,
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
        FormulaFactory f = dummyModel.getFormulaFactory();
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
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", false), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_InBenchmarkModels() throws Exception {
        CausalModel benchmarkModel = ExampleProvider.benchmarkModel();
        FormulaFactory f = benchmarkModel.getFormulaFactory();
        // all exogenous variables are true
        Set<Literal> context = benchmarkModel.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("S")));
        Formula phi = f.variable("A");

        // test for real SAT approach only as for the others this is taking too long
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, true, cause, null);
        testSolve(benchmarkModel, context, phi, cause, causalitySolverResultExpected, BRUTE_FORCE);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInBillySuzy_When_BillyAndSuzyThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
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
        FormulaFactory f = billySuzy.getFormulaFactory();
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
    public void Should_ReturnEvaluationForEquationsInforestFireDisjunctive_When_LightningOnly() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", false)));

        Set<Literal> evaluationExpected = new HashSet<>(Arrays.asList(f.literal("L_exo", true),
                f.literal("MD_exo", false), f.literal("L", true),
                f.literal("MD", false), f.literal("FF", true)));
        Set<Literal> evaluationActual = CausalitySolver.evaluateEquations(forestFire, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnAllCauses_WhenSuzyBillyThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
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
                        put(SAT_OPTIMIZED_FORMULAS, allCausesExpectedSAT);
                        put(SAT_OPTIMIZED_FORMULAS_MINIMAL, allCausesExpectedEval);
                        put(SAT_OPTIMIZED_AC3, allCausesExpectedSAT);
                        put(SAT_OPTIMIZED_AC3_MINIMAL, allCausesExpectedEval);
                    }
                };

        testGetAllCauses(billySuzy, context, phi, allCausesExpected);
    }

    @Test
    public void Should_ReturnNoCause_WhenSuzyBillyDoNotThrow() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Formula phi = f.variable("BS");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>();

        testGetAllCauses(billySuzy, context, phi, allCausesExpected);
    }

    @Test
    public void Should_ReturnAllCauses_WhenLandMDInConjunctiveScenario() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(false);
        FormulaFactory f = forestFire.getFormulaFactory();
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

        testGetAllCauses(forestFire, context, phi, allCausesExpected);
    }

    @Test
    public void Should_ReturnAllCauses_WhenLandMDInDisjunctiveScenario() throws Exception {
        CausalModel forestFire = ExampleProvider.forestFire(true);
        FormulaFactory f = forestFire.getFormulaFactory();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Formula phi = f.variable("FF");
        Set<CausalitySolverResult> allCausesExpected = new HashSet<>(Arrays.asList(
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD"))), new HashSet<>()),
                new CausalitySolverResult(true, true, true,
                        new HashSet<>(Collections.singletonList(f.variable("FF"))), new HashSet<>())));

        testGetAllCauses(forestFire, context, phi, allCausesExpected);
    }
}