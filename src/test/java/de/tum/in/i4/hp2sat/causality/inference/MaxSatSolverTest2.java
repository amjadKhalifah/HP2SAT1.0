package de.tum.in.i4.hp2sat.causality.inference;

import de.tum.in.i4.hp2sat.causality.CausalModel;
import de.tum.in.i4.hp2sat.causality.CausalitySolverResult;
import de.tum.in.i4.hp2sat.causality.MaxSATCausalitySolver;
import de.tum.in.i4.hp2sat.util.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.stream.Collectors;




/** This is a class to try new test cases with bigger cardinalities of x and w
 * @author Ibrahim Amjad
 *
 */
public class MaxSatSolverTest2 {
    FormulaFactory f;
    MaxSATCausalitySolver solver;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
        solver = new MaxSATCausalitySolver();
    }


	private void testSolve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
			CausalitySolverResult causalitySolverResultsExpected) throws Exception {

		CausalitySolverResult causalitySolverResultActual = null;
		causalitySolverResultActual = solver.solve(causalModel, context, phi, cause, null);
<<<<<<< Updated upstream
		assertTrue("Error for Maxsat Expected is "+causalitySolverResultsExpected+", while actual is "+causalitySolverResultActual, causalitySolverResultActual.equals2(causalitySolverResultsExpected));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
=======
		
		System.out.println("CM: "+causalModel.getName()+" size:"+ causalModel.getEquationsSorted().size()+" context size: "+ contextVars.size()+" cause size "+ cause.size()+ "phi size: "+phi.numberOfOperands()
		+" "+ causalitySolverResultActual.toStringSummary() );
		
		assertTrue("Error for MaxSat Expected is "+causalitySolverResultsExpected+", while actual is "+causalitySolverResultActual, causalitySolverResultActual.equals2(causalitySolverResultsExpected));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
>>>>>>> Stashed changes
		
	}




    // #################################################################################################################
    // ################################### DUMMY MODEL COMBINED WITH BINARY TREE #######################################
    // #################################################################################################################
    //region DUMMY MODEL COMBINED WITH BINARY TREE
    @Test
    public void noncf_min0_cause1_effect1() throws Exception {
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
    public void cfminimal_min2_cause2_effect1() throws Exception {
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
    public void CFNONminimal_min2_Cause5_effect1() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.literal("n_4092", false),f.literal("n_4091", false),f.literal("n_4090", false)));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    @Test
    public void CFNONminimal_min2_Cause10_effect1() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.literal("n_4092", false),f.literal("n_4091", false),f.literal("n_4090", false),
        		f.literal("n_4089", false),f.literal("n_4088", false),f.literal("n_4087", false),f.literal("n_4086", false), f.literal("n_4085", false)));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    
    @Test
    public void CFNONminimal_min2_Cause15_effect1() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.literal("n_4092", false),f.literal("n_4091", false),f.literal("n_4090", false),
        		f.literal("n_4089", false),f.literal("n_4088", false),f.literal("n_4087", false),f.literal("n_4086", false), f.literal("n_4085", false),
        		f.literal("n_4084", false),f.literal("n_4083", false),f.literal("n_4060", false),f.literal("n_1070", false), f.literal("n_220", false)));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    
    
    @Test
    public void CFNONminimal_min2_Cause20_effect1() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.literal("n_4092", false),f.literal("n_4091", false),f.literal("n_4090", false),
        		f.literal("n_4089", false),f.literal("n_4088", false),f.literal("n_4087", false),f.literal("n_4086", false), f.literal("n_4085", false),
        		f.literal("n_4084", false),f.literal("n_4083", false),f.literal("n_4060", false),f.literal("n_1070", false), f.literal("n_220", false),
        		f.literal("n_3084", false),f.literal("n_3555", false),f.literal("n_3027", false),f.literal("n_3201", false), f.literal("n_3350", false)));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"),
                                f.variable("H"))));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    @Test
    public void CFNONminimal_min2_Cause20_effect2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.literal("n_4092", false),f.literal("n_4091", false),f.literal("n_4090", false),
        		f.literal("n_4089", false),f.literal("n_4088", false),f.literal("n_4087", false),f.literal("n_4086", false), f.literal("n_4085", false),
        		f.literal("n_4084", false),f.literal("n_4083", false),f.literal("n_4060", false),f.literal("n_1070", false), f.literal("n_220", false),
        		f.literal("n_3084", false),f.literal("n_3555", false),f.literal("n_3027", false),f.literal("n_3201", false), f.literal("n_3350", false)));
        Formula phi = f.or(f.variable("F"),f.variable("H"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G")
                                )));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    
    @Test
    public void CFNONminimal_min2_Cause50_effect2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(
        		Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.literal("n_4092", false),f.literal("n_4091", false),f.literal("n_4090", false),
        		f.literal("n_4089", false),f.literal("n_4088", false),f.literal("n_4087", false),f.literal("n_4086", false), f.literal("n_4085", false),
        		f.literal("n_4084", false),f.literal("n_4083", false),f.literal("n_4060", false),f.literal("n_1070", false), f.literal("n_220", false),
        		f.literal("n_3084", false),f.literal("n_3555", false),f.literal("n_3027", false),f.literal("n_3201", false), f.literal("n_3350", false),
        		f.literal("n_2222", false),f.literal("n_2980", false),f.literal("n_2630", false),f.literal("n_2783", false), f.literal("n_2109", false),
        		f.literal("n_2084", false),f.literal("n_2476", false),f.literal("n_2466", false),f.literal("n_2485", false), f.literal("n_2409", false),
        		f.literal("n_1900", false),f.literal("n_1980", false),f.literal("n_1630", false),f.literal("n_1783", false), f.literal("n_1109", false),
        		f.literal("n_1084", false),f.literal("n_1476", false),f.literal("n_1466", false),f.literal("n_1485", false), f.literal("n_1409", false),
        		f.literal("n_222", false),f.literal("n_980", false),f.literal("n_630", false),f.literal("n_783", false), f.literal("n_109", false),
        		f.literal("n_84", false),f.literal("n_476", false),f.literal("n_466", false),f.literal("n_485", false), f.literal("n_409", false)   		
        		        		));
        Formula phi = f.or(f.variable("F"),f.variable("H"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G")
                                )));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    
    @Test
    public void CFNONminimal_min3_Cause50_effect2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo","4092_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(
        		Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.variable("n_4092"),f.literal("n_4091", false),f.literal("n_4090", false),
        		f.literal("n_4089", false),f.literal("n_4088", false),f.literal("n_4087", false),f.literal("n_4086", false), f.literal("n_4085", false),
        		f.literal("n_4084", false),f.literal("n_4083", false),f.literal("n_4060", false),f.literal("n_1070", false), f.literal("n_220", false),
        		f.literal("n_3084", false),f.literal("n_3555", false),f.literal("n_3027", false),f.literal("n_3201", false), f.literal("n_3350", false),
        		f.literal("n_2222", false),f.literal("n_2980", false),f.literal("n_2630", false),f.literal("n_2783", false), f.literal("n_2109", false),
        		f.literal("n_2084", false),f.literal("n_2476", false),f.literal("n_2466", false),f.literal("n_2485", false), f.literal("n_2409", false),
        		f.literal("n_1900", false),f.literal("n_1980", false),f.literal("n_1630", false),f.literal("n_1783", false), f.literal("n_1109", false),
        		f.literal("n_1084", false),f.literal("n_1476", false),f.literal("n_1466", false),f.literal("n_1485", false), f.literal("n_1409", false),
        		f.literal("n_222", false),f.literal("n_980", false),f.literal("n_630", false),f.literal("n_783", false), f.literal("n_109", false),
        		f.literal("n_84", false),f.literal("n_476", false),f.literal("n_466", false),f.literal("n_485", false), f.literal("n_409", false)   		
        		        		));
        Formula phi = f.or(f.variable("F"),f.variable("H"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"), f.variable("n_4092"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G")
                                )));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    @Test
    public void CFNONminimal_min5_Cause50_effect2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo","4092_exo","4091_exo","4090_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(
        		Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.variable("n_4092"),f.literal("n_4091", true),f.literal("n_4090", true),
        		f.literal("n_4089", false),f.literal("n_4088", false),f.literal("n_4087", false),f.literal("n_4086", false), f.literal("n_4085", false),
        		f.literal("n_4084", false),f.literal("n_4083", false),f.literal("n_4060", false),f.literal("n_1070", false), f.literal("n_220", false),
        		f.literal("n_3084", false),f.literal("n_3555", false),f.literal("n_3027", false),f.literal("n_3201", false), f.literal("n_3350", false),
        		f.literal("n_2222", false),f.literal("n_2980", false),f.literal("n_2630", false),f.literal("n_2783", false), f.literal("n_2109", false),
        		f.literal("n_2084", false),f.literal("n_2476", false),f.literal("n_2466", false),f.literal("n_2485", false), f.literal("n_2409", false),
        		f.literal("n_1900", false),f.literal("n_1980", false),f.literal("n_1630", false),f.literal("n_1783", false), f.literal("n_1109", false),
        		f.literal("n_1084", false),f.literal("n_1476", false),f.literal("n_1466", false),f.literal("n_1485", false), f.literal("n_1409", false),
        		f.literal("n_222", false),f.literal("n_980", false),f.literal("n_630", false),f.literal("n_783", false), f.literal("n_109", false),
        		f.literal("n_84", false),f.literal("n_476", false),f.literal("n_466", false),f.literal("n_485", false), f.literal("n_409", false)   		
        		        		));
        Formula phi = f.or(f.variable("F"),f.variable("H"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"), f.variable("n_4092")
                		, f.variable("n_4091"), f.variable("n_4090"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G")
                                )));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    @Test
    public void CFNONminimal_min10_Cause50_effect2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo","4092_exo","4091_exo","4090_exo",
        		"4089_exo","4088_exo", "4087_exo","4086_exo","4085_exo","4084_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(
        		Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.variable("n_4092"),f.literal("n_4091", true),f.literal("n_4090", true),
        		f.literal("n_4089", true),f.literal("n_4088", true),f.literal("n_4087", true),f.literal("n_4086", true), f.literal("n_4085", true),
        		f.literal("n_4084", true),f.literal("n_4083", false),f.literal("n_4060", false),f.literal("n_1070", false), f.literal("n_220", false),
        		f.literal("n_3084", false),f.literal("n_3555", false),f.literal("n_3027", false),f.literal("n_3201", false), f.literal("n_3350", false),
        		f.literal("n_2222", false),f.literal("n_2980", false),f.literal("n_2630", false),f.literal("n_2783", false), f.literal("n_2109", false),
        		f.literal("n_2084", false),f.literal("n_2476", false),f.literal("n_2466", false),f.literal("n_2485", false), f.literal("n_2409", false),
        		f.literal("n_1900", false),f.literal("n_1980", false),f.literal("n_1630", false),f.literal("n_1783", false), f.literal("n_1109", false),
        		f.literal("n_1084", false),f.literal("n_1476", false),f.literal("n_1466", false),f.literal("n_1485", false), f.literal("n_1409", false),
        		f.literal("n_222", false),f.literal("n_980", false),f.literal("n_630", false),f.literal("n_783", false), f.literal("n_109", false),
        		f.literal("n_84", false),f.literal("n_476", false),f.literal("n_466", false),f.literal("n_485", false), f.literal("n_409", false)   		
        		        		));
        Formula phi = f.or(f.variable("F"),f.variable("H"));

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, false, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"), f.variable("n_4092")
                		, f.variable("n_4091"), f.variable("n_4090"),f.variable("n_4089"), f.variable("n_4088"),
                		f.variable("n_4087"), f.variable("n_4086"),f.variable("n_4084"),f.variable("n_4085"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G")
                                )));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    @Test
    public void CFNONminimal_min10_Cause10_effect1() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("B_exo","4094_exo", "4093_exo","4092_exo","4091_exo","4090_exo",
        		"4089_exo","4088_exo", "4087_exo","4086_exo","4085_exo","4084_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(
        		Arrays.asList(f.variable("n_4093"), f.variable("n_4094"),f.variable("n_4092"),f.literal("n_4091", true),f.literal("n_4090", true),
        		f.literal("n_4089", true),f.literal("n_4088", true),f.literal("n_4087", true),f.literal("n_4086", true), f.literal("n_4085", true),
        		f.literal("n_4084", true)       		        		));
        Formula phi = f.variable("F");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, new HashSet<>(Arrays.asList(f.variable("n_4093"), f.variable("n_4094"), f.variable("n_4092")
                		, f.variable("n_4091"), f.variable("n_4090"),f.variable("n_4089"), f.variable("n_4088"),
                		f.variable("n_4087"), f.variable("n_4086"),f.variable("n_4084"),f.variable("n_4085"))),
                        new HashSet<>(Arrays.asList(f.variable("E"),
                                f.variable("B"), f.variable("G"), f.variable("H")
                                )));
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    // this test needs a memory congiuration -Xms4g -Xmx12g
    @Test
    public void CFNONminimal_min10_Cause10_effect1_combined2() throws Exception {
        CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree2();
        FormulaFactory f = dummyModel.getFormulaFactory();
        Set<String> contextVars = new HashSet<>(Arrays.asList("p1_4094_exo", "p1_4093_exo","p1_4092_exo","p1_4091_exo","p1_4090_exo",
        		"p1_4089_exo","p1_4088_exo", "p1_4087_exo","p1_4086_exo","p1_4085_exo","p1_4084_exo"));
        Set<Literal> context = dummyModel.getExogenousVariables().stream()
                .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
        
       
        Set<Literal> cause = new HashSet<>(
        		Arrays.asList(f.variable("p1_n_4093"), f.variable("p1_n_4094"),f.variable("p1_n_4092"),f.literal("p1_n_4091", true),f.literal("p1_n_4090", true),
        		f.literal("p1_n_4089", true),f.literal("p1_n_4088", true),f.literal("p1_n_4087", true),f.literal("p1_n_4086", true), f.literal("p1_n_4085", true),
        		f.literal("p1_n_4084", true)));
        Formula phi = f.variable("C");

        CausalitySolverResult causalitySolverResultExpected =
                new CausalitySolverResult(true, true, true, new HashSet<>(Arrays.asList(f.variable("p1_n_4093"), f.variable("p1_n_4094"), f.variable("p1_n_4092")
                		, f.variable("p1_n_4091"), f.variable("p1_n_4090"),f.variable("p1_n_4089"), f.variable("p1_n_4088"),
                		f.variable("p1_n_4087"), f.variable("p1_n_4086"),f.variable("p1_n_4084"),f.variable("p1_n_4085"))),
                        new HashSet<>());
        testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
    }
    
    
    
//     set varibales on the two sides
    @Test
    public void CFNONminimal_min1_Cause20_effect1_combined_2branches() throws Exception {
    	 CausalModel dummyModel = ExampleProvider.dummyCombinedWithBinaryTree2();
         FormulaFactory f = dummyModel.getFormulaFactory();
         Set<String> contextVars = new HashSet<>(Arrays.asList("p1_4094_exo", "p1_4093_exo","p1_4092_exo","p1_4091_exo","p1_4090_exo",
         		"p1_4089_exo","p1_4088_exo", "p1_4087_exo","p1_4086_exo","p1_4085_exo","p1_4084_exo",
         		"p2_4094_exo", "p2_4093_exo","p2_4092_exo","p2_4091_exo","p2_4090_exo",
         		"p2_4089_exo","p2_4088_exo", "p2_4087_exo","p2_4086_exo","p2_4085_exo","p2_4084_exo"));
         Set<Literal> context = dummyModel.getExogenousVariables().stream()
                 .map(v -> contextVars.contains(v.name()) ? v : v.negate()).collect(Collectors.toSet());
         
        
         Set<Literal> cause = new HashSet<>(
         		Arrays.asList(f.variable("p1_n_4093"), f.variable("p1_n_4094"),f.variable("p1_n_4092"),f.literal("p1_n_4091", true),f.literal("p1_n_4090", true),
         		f.literal("p1_n_4089", true),f.literal("p1_n_4088", true),f.literal("p1_n_4087", true),f.literal("p1_n_4086", true), f.literal("p1_n_4085", true),
         		f.literal("p1_n_4084", true),f.variable("p2_n_4093"), f.variable("p2_n_4094"),f.variable("p2_n_4092"),f.literal("p2_n_4091", true),f.literal("p2_n_4090", true),
         		f.literal("p2_n_4089", true),f.literal("p2_n_4088", true),f.literal("p2_n_4087", true),f.literal("p2_n_4086", true), f.literal("p2_n_4085", true),
         		f.literal("p2_n_4084", true)));
         Formula phi = f.variable("C");

         CausalitySolverResult causalitySolverResultExpected =
                 new CausalitySolverResult(true, true, true, new HashSet<>(Arrays.asList(f.variable("p1_n_4093"), f.variable("p1_n_4094"), f.variable("p1_n_4092")
                 		, f.variable("p1_n_4091"), f.variable("p1_n_4090"),f.variable("p1_n_4089"), f.variable("p1_n_4088"),
                 		f.variable("p1_n_4087"), f.variable("p1_n_4086"),f.variable("p1_n_4084"),f.variable("p1_n_4085"))),
                         new HashSet<>());
         testSolve(dummyModel, context, phi, cause, causalitySolverResultExpected);
         }




}