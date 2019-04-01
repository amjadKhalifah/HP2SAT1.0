package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.util.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.stream.Collectors;



public class ILPSolverTest {
    FormulaFactory f;
  ILPCausalitySolver solver;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
        solver = new ILPCausalitySolver();
    }


	private void testSolve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
			CausalitySolverResult causalitySolverResultsExpected) throws Exception {

		CausalitySolverResult causalitySolverResultActual = null;
		causalitySolverResultActual = solver.solve(causalModel, context, phi, cause, SolvingStrategy.ILP,
				ILPSolverType.GUROBI);
		assertTrue("Error for ILP / Gurobi Expected is "+causalitySolverResultsExpected+", while actual is "+causalitySolverResultActual, causalitySolverResultActual.equals2(causalitySolverResultsExpected));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
		
	}


	   @Test
	    public void Should_FulfillAC1AC2AC3Only_When_STIsCauseForBS() throws Exception {
	        CausalModel billySuzy = ExampleProvider.billySuzy();
	        Set<Literal> context = new HashSet<>(Arrays.asList(
	                f.literal("BT_exo", true), f.literal("ST_exo", true)));
	        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
	        Formula phi = f.variable("BS");

	        CausalitySolverResult causalitySolverResultExpected =
	                new CausalitySolverResult(true, true, true, cause, new HashSet<>(Arrays.asList(f.variable("BH"))));
	        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
	    }

    @Test
    public void Should_FulfillAC1AC3Only_When_BTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_STandBTIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("XT")));
        Formula phi = f.variable("BS");
        // different in results than SAT in this case in the more minimal cause and the W
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("XT"))), new HashSet<>(Arrays.asList(f.variable("BH"))));
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Test_Why_BS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("XT"), f.variable("SH"), f.literal("BH",false)));
        Formula phi = f.variable("BS");
        // different in results than SAT in this case in the more minimal cause and the W
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("SH"))), new HashSet<>(Arrays.asList(f.variable("BH"))));
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
    
    @Test
    public void Should_FulfillAC1AC2Only_When_STandSHIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("XT"), f.variable("SH")));
        Formula phi = f.variable("BS");

        // both are two minimum causes
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("SH"))),
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("BH"))));


        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpectedSAT);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_STandBTandSHIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("XT"), f.variable("BT"),
                f.variable("SH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("SH"))), new HashSet<>(Arrays.asList( f.variable("BH"))));
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
// check this case there is a more minimal cause but the W is not so correct based on other causes in the question
    @Test
    public void Should_FulfillAC1Only_When_STandBTandNotBHIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("XT"), f.variable("BT"),
                f.literal("BH", false)));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false,  new HashSet<>(Arrays.asList(f.variable("XT"))), new HashSet<>(Arrays.asList(f.variable("BH"))));
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_BTandNotBHIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false)));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
    @Test
    public void Should_FulfillAC1A2Only_When_BTandBHIsCauseForBSGivenNotSTExo() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("BH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false,  new HashSet<>(Arrays.asList(f.variable("BH"))), new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }
//
    @Test
    public void Should_FulfillAC1AC2Only_When_NotSTandNotBTIsCauseForNotBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("XT", false),
                f.literal("BT", false)));
        Formula phi = f.literal("BS", false);
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC2Only_When_SHandBHIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("SH"), f.variable("BH")));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, new HashSet<>(Arrays.asList(f.variable("SH"))), new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1Only_When_SHandNotBHIsCauseForBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("SH"), f.literal("BH", false)));
        Formula phi = f.variable("BS");
        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, null);
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

//    @Test
//    public void Should_FulfillAC2Only_When_STandBTIsCauseForBS_GivenNotBT() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", false), f.literal("ST_exo", true)));
//        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT")));
//        Formula phi = f.variable("BS");
//        CausalitySolverResult causalitySolverResultExpected =
//                new CausalitySolverResult(false, true, false, cause, new HashSet<>());
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
//    }
//    
    
    
    ////////////////////////////

//    @Test
//    public void Should_FulfillAllACs_When_STIsCauseBS() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", true), f.literal("ST_exo", true)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
//        Formula phi = f.variable("BS");
//
//        CausalitySolverResult causalitySolverResultExpectedEval =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
//        CausalitySolverResult causalitySolverResultExpectedSAT =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
//        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
//                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
//                    {
//                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(BRUTE_FORCE_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_COMBINED_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_W_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_CLAUSES,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                    }
//                };
//
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
//    }
//
//    @Test
//    public void Should_FulfillAllACs_When_BTIsCauseForBSGivenNotST() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", true), f.literal("ST_exo", false)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
//        Formula phi = f.variable("BS");
//
//        CausalitySolverResult causalitySolverResultExpected =
//                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
//    }
//
//    @Test
//    public void Should_FulfillAllACs_When_STIsCauseForBSGivenNotBT() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", false), f.literal("ST_exo", true)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
//        Formula phi = f.variable("BS");
//
//        CausalitySolverResult causalitySolverResultExpected =
//                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
//    }
//
//    @Test
//    public void Should_FulfillAllACs_When_NotSTIsCauseForNotSHandNotBS_GivenNotBT() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", false), f.literal("ST_exo", false)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("ST", false)));
//        Formula phi = f.not(f.or(f.variable("SH"), f.variable("BS")));
//
//        CausalitySolverResult causalitySolverResultExpected =
//                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
//    }
//
//    @Test
//    public void Should_FulfillAllACs_When_SHIsCauseBS() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", true), f.literal("ST_exo", true)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("SH")));
//        Formula phi = f.variable("BS");
//
//        CausalitySolverResult causalitySolverResultExpectedEval =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
//        CausalitySolverResult causalitySolverResultExpectedSAT =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("ST"),
//                                f.literal("BH", false))));
//        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
//                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
//                    {
//                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(BRUTE_FORCE_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_COMBINED_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_W_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_CLAUSES,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                    }
//                };
//
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
//    }
//
//    @Test
//    public void Should_FulfillAllACs_When_NotBTIsCauseForNotBS() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", false), f.literal("ST_exo", false)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
//        Formula phi = f.literal("BS", false);
//
//        CausalitySolverResult causalitySolverResultExpected =
//                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
//    }
//
//    @Test
//    public void Should_NotFulfillACs_When_NotBTIsCauseForBS() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", true), f.literal("ST_exo", true)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("BT", false)));
//        Formula phi = f.variable("BS");
//
//        CausalitySolverResult causalitySolverResultExpected =
//                new CausalitySolverResult(false, false, true, cause, null);
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
//    }
//
//    @Test
//    public void Should_FulfillAllACs_When_STIsCauseBSOrSH() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", true), f.literal("ST_exo", true)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
//        Formula phi = f.or(f.variable("BS"), f.variable("SH"));
//
//        CausalitySolverResult causalitySolverResultExpectedEval =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
//        CausalitySolverResult causalitySolverResultExpectedSAT =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
//        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
//                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
//                    {
//                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(BRUTE_FORCE_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_COMBINED_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_W_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_CLAUSES,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                    }
//                };
//
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
//    }
//
//    @Test
//    public void Should_FulfillAC2AC3Only_When_STIsCauseBSAndBH() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzy();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", true), f.literal("ST_exo", true)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
//        Formula phi = f.and(f.variable("BS"), f.variable("BH"));
//
//        CausalitySolverResult causalitySolverResultExpectedEval =
//                new CausalitySolverResult(false, true, true, cause,
//                        new HashSet<>(Collections.singletonList(f.literal("SH", true))));
//        CausalitySolverResult causalitySolverResultExpectedSAT =
//                new CausalitySolverResult(false, true, true, cause,
//                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
//        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL1 = causalitySolverResultExpectedEval;
//        // TODO check if it really makes sense that we can put a part of X into W
//        CausalitySolverResult causalitySolverResultExpectedSATMINIMAL2 =
//                new CausalitySolverResult(false, true, true, cause,
//                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
//        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
//                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
//                    {
//                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(BRUTE_FORCE_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_MINIMAL, new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
//                                causalitySolverResultExpectedSATMINIMAL2)));
//                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_COMBINED_MINIMAL,
//                                new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
//                                        causalitySolverResultExpectedSATMINIMAL2)));
//                        put(SAT_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_W_MINIMAL,
//                                new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
//                                        causalitySolverResultExpectedSATMINIMAL2)));
//                        put(SAT_OPTIMIZED_CLAUSES,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
//                                new HashSet<>(Arrays.asList(causalitySolverResultExpectedSATMINIMAL1,
//                                        causalitySolverResultExpectedSATMINIMAL2)));
//                    }
//                };
//
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
//    }
//
    @Test
    public void Should_FulfillAC1AC3Only_When_LIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.forestFire(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(arsonists, context, phi, cause, causalitySolverResultExpected);
    }
//
    @Test
    public void Should_FulfillAllAcs_When_LAndMDIsCauseForFF() throws Exception {
        CausalModel arsonists = ExampleProvider.forestFire(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(arsonists, context, phi, cause, causalitySolverResultExpected);
    }
//
//    @Test
//    public void Should_FulfillAllAC2AC3Only_When_LAndMDIsCauseForFF_GivenNotLExoAndNotMDExo() throws Exception {
//        CausalModel arsonists = ExampleProvider.forestFire(true);
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("L_exo", false), f.literal("MD_exo", false)));
//        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
//        Formula phi = f.variable("FF");
//
//        CausalitySolverResult causalitySolverResultExpected =
//                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
//        testSolve(arsonists, context, phi, cause, causalitySolverResultExpected);
//    }
//
//    @Test
//    public void Should_FulfillAllACs_When_STIsCauseForBSInExtendedModelWITHOUTWind() throws Exception {
//        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
//        Set<Literal> context = new HashSet<>(Arrays.asList(
//                f.literal("BT_exo", true), f.literal("ST_exo", true),
//                f.literal("W_exo", false)));
//        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
//        Formula phi = f.variable("BS");
//
//        CausalitySolverResult causalitySolverResultExpectedEval =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
//        CausalitySolverResult causalitySolverResultExpectedSAT =
//                new CausalitySolverResult(true, true, true, cause,
//                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
//        Map<SolvingStrategy, Set<CausalitySolverResult>> causalitySolverResultsExpected =
//                new HashMap<SolvingStrategy, Set<CausalitySolverResult>>() {
//                    {
//                        put(BRUTE_FORCE, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(BRUTE_FORCE_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_MINIMAL, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_COMBINED, new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_COMBINED_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_W,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_W_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                        put(SAT_OPTIMIZED_CLAUSES,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedSAT)));
//                        put(SAT_OPTIMIZED_CLAUSES_MINIMAL,
//                                new HashSet<>(Collections.singletonList(causalitySolverResultExpectedEval)));
//                    }
//                };
//
//        testSolve(billySuzy, context, phi, cause, causalitySolverResultsExpected);
//    }
//
    @Test
    public void Should_FulfillAC2AC3_When_STIsCauseForBSInExtendedModelWITHWind() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true),
                f.literal("W_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAllACs_When_CIsCauseForDGivenAAndNotB() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi, cause, causalitySolverResultExpected);
    }
//
    @Test
    public void Should_FulfillAllACs_When_AIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
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
                        new HashSet<>(Collections.singletonList(f.variable("B"))));

       
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpectedEval);
    }

    @Test
    public void Should_FulfillAllACs_When_AAndDIsCauseForCInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("D")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("A"))),
                        new HashSet<>(Collections.singletonList(f.variable("B"))));

      
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpectedEval);
    }

    @Test
    public void Should_FulfillAC3Only_When_BIsCauseInDummyModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, false, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
//
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
                        new HashSet<>(Collections.singletonList(f.variable("D1"))));

     

        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpectedEval);
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
                        new HashSet<>(Collections.singletonList(f.variable("C1"))));
    
  

        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpectedEval);
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

    //double check which one is correct
    @Test
    public void Should_FulfillNoAC_When_AandBIsCauseForCInDummyXORModel() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, false, new HashSet<>(Arrays.asList( f.variable("A"))), new HashSet<>(Arrays.asList( f.variable("B"))));
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
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList( f.variable("A"))), new HashSet<>(Arrays.asList( f.variable("B"))));
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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        CausalitySolverResult causalitySolverResultActual = solver.solve(benchmarkModel, context, phi,
                cause, SolvingStrategy.ILP);
        assertTrue("Error for ILP / Gurobi Expected is "+causalitySolverResultExpected+", while actual is "+causalitySolverResultActual, causalitySolverResultActual.equals2(causalitySolverResultExpected));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
		
        CausalModel binaryTreeBenchmarkModelDepth7 = ExampleProvider.generateBinaryTreeBenchmarkModel(7);
        CausalModel binaryTreeBenchmarkModelDepth8 = ExampleProvider.generateBinaryTreeBenchmarkModel(8);
        CausalModel binaryTreeBenchmarkModelDepth9 = ExampleProvider.generateBinaryTreeBenchmarkModel(9);
        Formula phiBenchmarkModelBinaryTree = f.variable("n_0");

        CausalitySolverResult causalitySolverResultExpectedDepth7 =
                new CausalitySolverResult(true, false, false,
                        new HashSet<>(Collections.singletonList(f.variable("n_254"))), new HashSet<>());
        CausalitySolverResult causalitySolverResultActualDepth7 =
                solver.solve(binaryTreeBenchmarkModelDepth7,
                        binaryTreeBenchmarkModelDepth7.getExogenousVariables().stream().map(e -> (Literal) e)
                                .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree,
                        new HashSet<>(Collections.singletonList(f.variable("n_254"))), SolvingStrategy.ILP);
        assertTrue("Error for ILP / Gurobi Expected is "+causalitySolverResultExpectedDepth7+", while actual is "+causalitySolverResultActualDepth7, causalitySolverResultActualDepth7.equals2(causalitySolverResultExpectedDepth7));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
    	
        

        CausalitySolverResult causalitySolverResultExpectedDepth8 =
                new CausalitySolverResult(true, false, false,
                        new HashSet<>(Collections.singletonList(f.variable("n_510"))), new HashSet<>());
        CausalitySolverResult causalitySolverResultActualDepth8 =
                solver.solve(binaryTreeBenchmarkModelDepth8,
                        binaryTreeBenchmarkModelDepth8.getExogenousVariables().stream().map(e -> (Literal) e)
                                .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree,
                        new HashSet<>(Collections.singletonList(f.variable("n_510"))), SolvingStrategy.ILP);
        assertTrue("Error for ILP / Gurobi Expected is "+causalitySolverResultExpectedDepth8+", while actual is "+causalitySolverResultActualDepth8, causalitySolverResultActualDepth8.equals2(causalitySolverResultExpectedDepth8));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
    	
        
        CausalitySolverResult causalitySolverResultExpectedDepth9 =
                new CausalitySolverResult(true, false, false,
                        new HashSet<>(Collections.singletonList(f.variable("n_1022"))), new HashSet<>());
        CausalitySolverResult causalitySolverResultActualDepth9 =
                solver.solve(binaryTreeBenchmarkModelDepth9,
                        binaryTreeBenchmarkModelDepth9.getExogenousVariables().stream().map(e -> (Literal) e)
                                .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree,
                        new HashSet<>(Collections.singletonList(f.variable("n_1022"))), SolvingStrategy.ILP);
        assertTrue("Error for ILP / Gurobi Expected is "+causalitySolverResultExpectedDepth9+", while actual is "+causalitySolverResultActualDepth9, causalitySolverResultActualDepth9.equals2(causalitySolverResultExpectedDepth9));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
    	
    }
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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
    }
//
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
                        f.variable("DK_U2"), f.variable("DK_U3"),
                        f.variable("SD_U2"), f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
    }
//
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
                        f.variable("DK_U2"), f.variable("DK_U3"),
                        f.variable("SD_U2"), f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("FS_U1"), f.variable("FN_U1"),
                        f.variable("A_U1"))), new HashSet<>(Arrays.asList(
                        f.variable("DK_U2"), f.variable("DK_U3"),
                        f.variable("SD_U2"), f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                        f.variable("DK_U2"), f.variable("DK_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                        f.variable("SD_U2"), f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
    }
//
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
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("A_U1"))), new HashSet<>(Arrays.asList(
                        f.variable("SD_U2" ), f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
    }
//

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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("FS_U2"), f.variable("FN_U2"),
                        f.variable("A_U2"))), new HashSet<>(Arrays.asList( f.variable("DK_U3"), f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                        new HashSet<>(Collections.singletonList(f.variable("DK_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                        new HashSet<>(Collections.singletonList(f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("A_U2"))),
                        new HashSet<>(Collections.singletonList(f.variable("SD_U3"))));
        // exclude brute-force approach (takes too long) and strategies that yield non-minimal W (ease testing)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, true, false,  new HashSet<>(Arrays.asList(f.variable("FS_U3"), f.variable("FN_U3"),
                        f.variable("A_U3"))), new HashSet<>());
        // exclude brute-force approach (takes too long)
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
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
                new CausalitySolverResult(true, true, false,  new HashSet<>(Arrays.asList(f.variable("A_U3"))), new HashSet<>());
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
        testSolve(stealMasterKey, context, phi, cause, causalitySolverResultExpected);
    }
    
    

    // #################################################################################################################
    // ################################### DUMMY MODEL COMBINED WITH BINARY TREE #######################################
    // #################################################################################################################
    //region DUMMY MODEL COMBINED WITH BINARY TREE
    @Test
    public void Should_FulfillAC1AC3Only_When_L4094_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("n_4094")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }


    @Test
    public void Should_FulfillAllACs_When_L4093AndL4094_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC2Only_When_NOTL4092AndL4093AndL4094_IsCauseFor_F() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("n_4092", false),
                f.variable("n_4093"), f.variable("n_4094")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4094"),
                        f.variable("n_4093"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"), f.variable("n_2045"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
//
    @Test
    public void Should_FulfillAC1AC2Only_When_L4094_IsCauseFor_F_Given_4094Exo_BExo() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("n_4094")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }

}