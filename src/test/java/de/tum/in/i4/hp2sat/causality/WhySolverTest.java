package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.util.ExampleProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WhySolverTest {
	FormulaFactory f;
	WhySolverCNF solver;

	@Before
	public void setUp() throws Exception {
		f = new FormulaFactory();
		solver = new WhySolverCNF();
	}
	

	private void testSolve(CausalModel causalModel, Set<Literal> context, Formula phi,
			CausalitySolverResult ... causalitySolverResultsExpected) throws Exception {
		List<CausalitySolverResult> causalitySolverResultActual = null;
		causalitySolverResultActual = solver.solveWhy(causalModel, context, phi, null, SolvingStrategy.ILP);
		assertTrue("The returned cause is not among the known causes",Arrays.asList(causalitySolverResultsExpected).containsAll(causalitySolverResultActual));
	
	}
	private void testNoneCause(CausalModel causalModel, Set<Literal> context, Formula phi,
			CausalitySolverResult ... causalitySolverResultsExpected) throws Exception {
		List<CausalitySolverResult> causalitySolverResultActual = null;
		causalitySolverResultActual = solver.solveWhy(causalModel, context, phi, null, SolvingStrategy.ILP);
//		System.out.println("Actual"+causalitySolverResultActual);
//		System.out.println(causalitySolverResultsExpected[0]);
		assertTrue("Wrong cause among the known causes", !Arrays.asList(causalitySolverResultsExpected).containsAll(causalitySolverResultActual));
	
	}

	@Test
	public void Why_BS_TT() throws Exception {
		CausalModel billySuzy = ExampleProvider.billySuzy();
		Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", true), f.literal("ST_exo", true)));
		Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
		Set<Literal> cause2 = new HashSet<>(Collections.singletonList(f.variable("SH")));
		Set<Literal> w = new HashSet<>(Arrays.asList(f.literal("BH",false)));
		Formula phi = f.variable("BS");
		CausalitySolverResult causalitySolverResultExpected = new CausalitySolverResult(true, true, true, cause,w);
		CausalitySolverResult causalitySolverResultExpected2 = new CausalitySolverResult(true, true, true, cause2, w);
		testSolve(billySuzy, context, phi, causalitySolverResultExpected,causalitySolverResultExpected2);
	}

	@Test
	public void Why_BS_TT_worngcause() throws Exception {
		CausalModel billySuzy = ExampleProvider.billySuzy();
		Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", true), f.literal("ST_exo", true)));
		Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
		Formula phi = f.variable("BS");

		CausalitySolverResult causalitySolverResultExpected = new CausalitySolverResult(true, true, true, cause,
				null);
		testNoneCause(billySuzy, context, phi, causalitySolverResultExpected);
	}


	@Test
	public void Why_BS_FT_wrongcause() throws Exception {
		CausalModel billySuzy = ExampleProvider.billySuzy();
		Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", true), f.literal("ST_exo", false)));
//		Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("BH")));
		Formula phi = f.variable("BS");
		// those are not the causes
		Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
		Set<Literal> cause2 = new HashSet<>(Collections.singletonList(f.variable("SH")));
		Set<Literal> w = new HashSet<>(Arrays.asList(f.literal("BH",false)));
		
		CausalitySolverResult causalitySolverResultExpected = new CausalitySolverResult(true, true, true, cause,w);
		CausalitySolverResult causalitySolverResultExpected2 = new CausalitySolverResult(true, true, true, cause2, w);
		testNoneCause(billySuzy, context, phi, causalitySolverResultExpected,causalitySolverResultExpected2);
	}
	
	
	@Test
	public void Why_BS_FT() throws Exception {
		CausalModel billySuzy = ExampleProvider.billySuzy();
		Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", true), f.literal("ST_exo", false)));
		Formula phi = f.variable("BS");
		// those are the causes
		Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
		Set<Literal> cause2 = new HashSet<>(Collections.singletonList(f.variable("BH")));
		Set<Literal> w = new HashSet<Literal>();
		
		CausalitySolverResult causalitySolverResultExpected = new CausalitySolverResult(true, true, true, cause,w);
		CausalitySolverResult causalitySolverResultExpected2 = new CausalitySolverResult(true, true, true, cause2, w);
		testSolve(billySuzy, context, phi, causalitySolverResultExpected,causalitySolverResultExpected2);
	}

	
	@Test
	public void Why_NotBS_FF() throws Exception {
		CausalModel billySuzy = ExampleProvider.billySuzy();
		Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", false), f.literal("ST_exo", false)));
		Set<Literal> cause = new HashSet<>(Arrays.asList(f.literal("XT", false)));
		Formula phi = f.literal("BS", false);
		CausalitySolverResult causalitySolverResultExpected = new CausalitySolverResult(true, true, true, cause,
				new HashSet<>());
		testSolve(billySuzy, context, phi, causalitySolverResultExpected);
	}
	
	
	
	@Test
	public void Why_BS_TF_wrongcause() throws Exception {
		CausalModel billySuzy = ExampleProvider.billySuzy();
		Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", false), f.literal("ST_exo", true)));
//		Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("BT"), f.variable("BH")));
		Formula phi = f.variable("BS");
		// those are not the causes
		Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
		Set<Literal> cause2 = new HashSet<>(Collections.singletonList(f.variable("BH")));
		Set<Literal> w = new HashSet<>(Arrays.asList(f.literal("SH",false)));
		
		CausalitySolverResult causalitySolverResultExpected = new CausalitySolverResult(true, true, true, cause,w);
		CausalitySolverResult causalitySolverResultExpected2 = new CausalitySolverResult(true, true, true, cause2, w);
		testNoneCause(billySuzy, context, phi, causalitySolverResultExpected,causalitySolverResultExpected2);
	}
	
	
	@Test
	public void Why_BS_TF() throws Exception {
		CausalModel billySuzy = ExampleProvider.billySuzy();
		Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", false), f.literal("ST_exo", true)));
		Formula phi = f.variable("BS");
		// those are the causes
		Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
		Set<Literal> cause2 = new HashSet<>(Collections.singletonList(f.variable("SH")));
		Set<Literal> w = new HashSet<Literal>();
		
		CausalitySolverResult causalitySolverResultExpected = new CausalitySolverResult(true, true, true, cause,w);
		CausalitySolverResult causalitySolverResultExpected2 = new CausalitySolverResult(true, true, true, cause2, w);
		testSolve(billySuzy, context, phi, causalitySolverResultExpected,causalitySolverResultExpected2);
	}

	

    @Test
    public void Why_BSorSH_TT() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
    	Set<Literal> w = new HashSet<>(Collections.singletonList(f.literal("BH", false)));
    	Set<Literal> w2 = new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false)));
        Formula phi = f.or(f.variable("BS"), f.variable("SH"));

        CausalitySolverResult causalitySolverResultExpected1 =
                new CausalitySolverResult(true, true, true, cause,
                        w);
        CausalitySolverResult causalitySolverResultExpected2 =
                new CausalitySolverResult(true, true, true, cause,w2);


        testSolve(billySuzy, context, phi, causalitySolverResultExpected1,causalitySolverResultExpected2);
    }

    @Test
    public void Why_BSandSH_TT() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
        Formula phi = f.and(f.variable("BS"), f.variable("SH"));

        CausalitySolverResult causalitySolverResultExpected1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>());
  
   
        testSolve(billySuzy, context, phi, causalitySolverResultExpected1);
    }

    @Test
    public void Why_DFF_TT_wrongcause() throws Exception {
        CausalModel arsonists = ExampleProvider.forestFire(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("L")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<Literal>());
        testNoneCause(arsonists, context, phi, causalitySolverResultExpected);
    }

    @Test
    public void Why_DFF_TT() throws Exception {
        CausalModel arsonists = ExampleProvider.forestFire(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", true), f.literal("MD_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(arsonists, context, phi, causalitySolverResultExpected);
    }

    @Test
    public void Why_DFF_FF() throws Exception {
        CausalModel arsonists = ExampleProvider.forestFire(true);
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("L_exo", false), f.literal("MD_exo", false)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("L"), f.variable("MD")));
        Formula phi = f.variable("FF");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testNoneCause(arsonists, context, phi, causalitySolverResultExpected);
    }
    @Test
    public void  Why_BS_TTT() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true),
                f.literal("W_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected1 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpected2 =
                new CausalitySolverResult(true, true, true, new HashSet<>(Collections.singletonList(f.variable("SH"))),
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpected3 =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
        

        testSolve(billySuzy, context, phi, causalitySolverResultExpected1, causalitySolverResultExpected2);
    }

    @Test
    public void Why_BS_TTT_wrongCause() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzyExtended();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true),
                f.literal("W_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("XT")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testNoneCause(billySuzy, context, phi, causalitySolverResultExpected);
    }
    @Test
    public void Why_D_TFT() throws Exception {
        CausalModel guns = ExampleProvider.prisoners();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false),
                f.literal("C_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Formula phi = f.variable("D");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(guns, context, phi,  causalitySolverResultExpected);
    }

    @Test
    public void Why_F_TT() throws Exception {
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
        testSolve(dummyModel, context, phi, causalitySolverResultExpected);
    }
//

    @Test
    public void Why_C_TT() throws Exception {
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

        testSolve(dummyModel, context, phi, causalitySolverResultExpectedEval,causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2);
    }
//
    @Test
    public void Why_C_TT_wrongcause() throws Exception {
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
         
        testNoneCause(dummyModel, context, phi,  causalitySolverResultExpectedSAT1, causalitySolverResultExpectedSAT2);
    }

    @Test
    public void Why_F_TT_wrongcause() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("B")));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, false, true, cause, null);
        testNoneCause(dummyModel, context, phi, causalitySolverResultExpected);
    }

    @Test
    public void Why_X_TTTT() throws Exception {
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
        
        testSolve(dummyModel, context, phi, causalitySolverResultExpectedEval,causalitySolverResultExpectedSAT, causalitySolverResultExpectedSATMINIMAL1,causalitySolverResultExpectedSATMINIMAL2,causalitySolverResultExpectedSATMINIMAL3,causalitySolverResultExpectedSATMINIMAL4);
    }

    @Test
    public void Why_Y_TTTT() throws Exception {
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
      

        testSolve(dummyModel, context, phi, causalitySolverResultExpectedEval,causalitySolverResultExpectedSAT, causalitySolverResultExpectedSATMINIMAL1,causalitySolverResultExpectedSATMINIMAL2,causalitySolverResultExpectedSATMINIMAL3,causalitySolverResultExpectedSATMINIMAL4);

    }
//
    @Test
    public void Why_C_TF() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", false)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("A")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
        testSolve(dummyModel, context, phi, causalitySolverResultExpected);
    }


    @Test
    public void Why_DummyXNORModel_TT_wrongCause1() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(false, true, true, cause, new HashSet<>());
        testNoneCause(dummyModel, context, phi, causalitySolverResultExpected);
    }

    @Test
    public void Why_DummyXNORModel_TT_wrongCause() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyXNOR();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("A_exo", true), f.literal("B_exo", true)));
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("A"), f.variable("B")));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, false, false, cause, null);
        testNoneCause(dummyModel, context, phi, causalitySolverResultExpected);
    }

    @Test
    public void Should_FulfillAC1AC3Only_When_InBenchmarkModels() throws Exception {
        CausalModel benchmarkModel = ExampleProvider.benchmarkModel();
        // all exogenous variables are true
        Set<Literal> context = benchmarkModel.getExogenousVariables().stream().map(e -> (Literal) e)
                .collect(Collectors.toSet());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("S")));
        Formula phi = f.variable("A");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, cause, null);
           testNoneCause(benchmarkModel, context, phi, causalitySolverResultExpected);

           CausalModel binaryTreeBenchmarkModelDepth7 = ExampleProvider.generateBinaryTreeBenchmarkModel(7);
           CausalModel binaryTreeBenchmarkModelDepth8 = ExampleProvider.generateBinaryTreeBenchmarkModel(8);
           CausalModel binaryTreeBenchmarkModelDepth9 = ExampleProvider.generateBinaryTreeBenchmarkModel(9);
           CausalModel binaryTreeBenchmarkModelDepth10 = ExampleProvider.generateBinaryTreeBenchmarkModel(10);
           CausalModel binaryTreeBenchmarkModelDepth11 = ExampleProvider.generateBinaryTreeBenchmarkModel(11);
           CausalModel binaryTreeBenchmarkModelDepth12 = ExampleProvider.generateBinaryTreeBenchmarkModel(12);
           
           
           Formula phiBenchmarkModelBinaryTree = f.variable("n_0");

           CausalitySolverResult causalitySolverResultExpectedDepth7 =
                   new CausalitySolverResult(true, true, true,
                           new HashSet<>(Collections.singletonList(f.variable("n_254"))), null);
          
              testNoneCause(binaryTreeBenchmarkModelDepth7, binaryTreeBenchmarkModelDepth7.getExogenousVariables().stream().map(e -> (Literal) e)
                      .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree, causalitySolverResultExpectedDepth7);
           
           
           CausalitySolverResult causalitySolverResultExpectedDepth8 =
                   new CausalitySolverResult(true, false, true,
                           new HashSet<>(Collections.singletonList(f.variable("n_510"))), null);
   

           testNoneCause(binaryTreeBenchmarkModelDepth8, binaryTreeBenchmarkModelDepth8.getExogenousVariables().stream().map(e -> (Literal) e)
                   .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree, causalitySolverResultExpectedDepth8);
        
        
           
           CausalitySolverResult causalitySolverResultExpectedDepth9 =
                   new CausalitySolverResult(true, false, true,
                           new HashSet<>(Collections.singletonList(f.variable("n_1022"))), null);
   

           testNoneCause(binaryTreeBenchmarkModelDepth9, binaryTreeBenchmarkModelDepth9.getExogenousVariables().stream().map(e -> (Literal) e)
                   .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree, causalitySolverResultExpectedDepth9);
           
           
           
           CausalitySolverResult causalitySolverResultExpectedDepth10 =
                   new CausalitySolverResult(true, false, true,
                           new HashSet<>(Collections.singletonList(f.variable("n_2046"))), null);
   

           testNoneCause(binaryTreeBenchmarkModelDepth10, binaryTreeBenchmarkModelDepth10.getExogenousVariables().stream().map(e -> (Literal) e)
                   .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree, causalitySolverResultExpectedDepth10);
//           
//           CausalitySolverResult causalitySolverResultExpectedDepth11 =
//                   new CausalitySolverResult(true, false, true,
//                           new HashSet<>(Collections.singletonList(f.variable("n_4094"))), null);
//   
//
//           testNoneCause(binaryTreeBenchmarkModelDepth11, binaryTreeBenchmarkModelDepth11.getExogenousVariables().stream().map(e -> (Literal) e)
//                   .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree, causalitySolverResultExpectedDepth11);
           
//           CausalitySolverResult causalitySolverResultExpectedDepth12 =
//                   new CausalitySolverResult(true, false, true,
//                           new HashSet<>(Collections.singletonList(f.variable("n_8190"))), null);
//   
//
//           testNoneCause(binaryTreeBenchmarkModelDepth12, binaryTreeBenchmarkModelDepth12.getExogenousVariables().stream().map(e -> (Literal) e)
//                   .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree, causalitySolverResultExpectedDepth12);
           
    }
    @Ignore
    @Test
    public void TestLoad() throws Exception {

           CausalModel binaryTreeBenchmarkModelDepth12 = ExampleProvider.generateBinaryTreeBenchmarkModel(12);
           
           
           Formula phiBenchmarkModelBinaryTree = f.variable("n_0");

    
    CausalitySolverResult causalitySolverResultExpectedDepth12 =
            new CausalitySolverResult(true, false, true,
                    new HashSet<>(Collections.singletonList(f.variable("n_8190"))), null);


    testNoneCause(binaryTreeBenchmarkModelDepth12, binaryTreeBenchmarkModelDepth12.getExogenousVariables().stream().map(e -> (Literal) e)
            .collect(Collectors.toSet()), phiBenchmarkModelBinaryTree, causalitySolverResultExpectedDepth12);
    
}
    
//
    
//    this case maybe wrong, it checks the cause of something that has not happened, i.e., phi violates AC1
//    @Test
    public void Why_BS_FF() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", false), f.literal("ST_exo", false)));
        Formula phi = f.variable("BS");
		
		testSolve(billySuzy, context, phi);
    }

}