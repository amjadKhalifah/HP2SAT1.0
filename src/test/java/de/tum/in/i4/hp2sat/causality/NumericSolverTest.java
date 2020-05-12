package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.util.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.NumericCausalModel;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.stream.Collectors;



/**
 * @author Ibrahim Amjad
 *
 */
public class NumericSolverTest {
  NumericCausalitySolver solver;

    @Before
    public void setUp() throws Exception {
        solver = new NumericCausalitySolver();
    }


	private void testSolve(NumericCausalModel causalModel, Map<String, Double> context, Expression phi, Set<Argument> cause,
			CausalitySolverResult<Argument> causalitySolverResultsExpected) throws Exception {

		CausalitySolverResult<Argument> causalitySolverResultActual = null;
		causalitySolverResultActual = solver.solve(causalModel, context, phi, cause, SolvingStrategy.ILP_NUM);
		assertTrue("Error for ILP / Gurobi Expected is "+causalitySolverResultsExpected+", while actual is "+causalitySolverResultActual, causalitySolverResultActual.equals2(causalitySolverResultsExpected));//(,causalitySolverResultsExpected.equals2(causalitySolverResultActual));
		
	}


	   @Test
	    public void Should_FulfillAC1AC2AC3Only_When_STIsCauseForBS() throws Exception {
	        NumericCausalModel billySuzy = ExampleProvider.billySuzyNumeric();
	        
	        Map<String,Double> context = new HashMap<String, Double>();
	        context.put("BT_exo", 1.0);
	        context.put("ST_exo", 1.0);
	        
	      
	        Argument cause1 = new Argument("XT", 1.0);
	        Set<Argument> cause = new HashSet<>(Collections.singletonList(cause1));
	                
	        Expression phi = new Expression("BS-1=0", billySuzy.getVaribale("BS"));

	        CausalitySolverResult causalitySolverResultExpected =
	                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
	        testSolve(billySuzy, context, phi, cause, causalitySolverResultExpected);
	    }
	   
	   
	   @Test
	    public void Should_FulfillAC1AC2AC3Only_Dummy() throws Exception {
	        NumericCausalModel dummy = ExampleProvider.Numeric_dummy_1();
	        
	        Map<String,Double> context = new HashMap<String, Double>();
	        context.put("X_exo", 12.0);
	        context.put("Y_exo", 38.0);
	        
	      
	        Argument cause1 = new Argument("X", 12.0);
	        Argument cause2 = new Argument("Y", 38.0);
	        Set<Argument> cause = new HashSet<>(Arrays.asList(cause1,cause2));       
	        Expression phi = new Expression("H+183=0", dummy.getVaribale("H"));

	        CausalitySolverResult causalitySolverResultExpected =
	                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
	        testSolve(dummy, context, phi, cause, causalitySolverResultExpected);
	    }

	   
	   @Test
	    public void Should_FulfillAC1AC2AC3Only_NumericTree() throws Exception {
		   // 13 --> 16k endo + 8k exo
	        NumericCausalModel tree = ExampleProvider.generateNumericTreeBenchmarkModel(13);
	        // this map is OK if it didn't contain every exo
	        Map<String,Double> context = new HashMap<String, Double>();
	        context.put("exo_415", 12.0);
	        context.put("exo_270", 38.0);
	        
//	        // this statments sets the context from the map; it keeps older values if the exo is not in the map
	        Map<String,Double> context2 = tree.getExogenousVariables().stream().map(v-> (context.keySet().contains(v.getArgumentName())? new Argument(v.getArgumentName(),context.get(v.getArgumentName())) : v )) .collect(Collectors.toMap(Argument::getArgumentName, Argument::getArgumentValue));
//	        
	        
	        Argument cause1 = new Argument("n_370", 32.0);
	        Argument cause2 = new Argument("n_422", 38.0);
	        Set<Argument> cause = new HashSet<>(Arrays.asList(cause1,cause2));       
	        Expression phi = new Expression("n_0-256=0", tree.getVaribale("n_0"));

	        tree.print();
	        CausalitySolverResult causalitySolverResultExpected =
	                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
	        testSolve(tree, context2, phi, cause, causalitySolverResultExpected);
	      

	   }
	   
	   /** 	private static int DEFAULT_BIG_M= 10000000;
	public static Double DEFAULT_UPPER_BOUND =  1000000.0;
	public static Double DEFAULT_LOWER_BOUND = -1000000.0;
	 * @throws Exception
	 */
	@Test
	    public void NumericTreeNew9() throws Exception {
	        NumericCausalModel tree = ExampleProvider.generateNumericTreeBenchmarkModel(9);
	        // exos are set randomly in the generator function
	        Map<String,Double> context = new HashMap<String, Double>();
	        
//	        // this statements sets the context from the map; it keeps older values if the exo is not in the map
	        Map<String,Double> context2 = tree.getExogenousVariables().stream().map(v-> (context.keySet().contains(v.getArgumentName())? new Argument(v.getArgumentName(),context.get(v.getArgumentName())) : v )) .collect(Collectors.toMap(Argument::getArgumentName, Argument::getArgumentValue));
	        // since the exos and the formulas are random, we need to get somevalues from the model
//	        1- the number of causes: 1 2 5 10 15 20 50 --> BM Param
	        // should make sure the number of causes is ok 
	        Set<Argument> causes = new HashSet<>();     
	        int numberofcauses=16;	
	        Random r = new Random();
	        for (int i =0; i<numberofcauses; i++) {
	        	int index = r.nextInt(tree.getEquationsSorted().size());
	        	 Argument e = tree.getEquationsSorted().get(index);
	        	if (!e.getArgumentName().equals("n_0"))	     // reserved for phi   	 
	        		causes.add(new Argument(e.getArgumentName(), e.getArgumentValue()));
	        	else {
	        		 Argument e2 = tree.getEquationsSorted().get(index--);
	        		 causes.add(new Argument(e2.getArgumentName(), e2.getArgumentValue()));
	        	}
	        }
	        
	        solver.setDEFAULT_BIG_M(10000000);
	        solver.setDEFAULT_UPPER_BOUND(1000000.0);
	        solver.setDEFAULT_LOWER_BOUND(-1000000.0);
	        
	        
	        double rootValue = tree.getVaribale("n_0").getArgumentValue();
	    	Expression phi,phi2;
	        if (rootValue<0) {
	        	 phi = new Expression("n_0+"+(-1*rootValue)+"= 0", tree.getVaribale("n_0"));
	        	 phi2 = new Expression("n_0+"+((-1*rootValue)+100)+"!= 0", tree.getVaribale("n_0")); 
	        }
	        else {
	        	 phi = new Expression("n_0-"+rootValue+"= 0", tree.getVaribale("n_0"));
	        	 phi2 = new Expression("n_0-"+(rootValue+100)+"!= 0", tree.getVaribale("n_0")); 
	        }
	        tree.print();
	        CausalitySolverResult causalitySolverResultExpected =
	                new CausalitySolverResult(true, true, false, causes, new HashSet<>());
	        testSolve(tree, context2, phi2, causes, causalitySolverResultExpected);

	   }
	   
	   
	   @Test
	    public void NumericTreeNew10() throws Exception {
	        NumericCausalModel tree = ExampleProvider.generateNumericTreeBenchmarkModel(10);
	        // exos are set randomly in the generator function
	        Map<String,Double> context = new HashMap<String, Double>();
	        
//	        // this statements sets the context from the map; it keeps older values if the exo is not in the map
	        Map<String,Double> context2 = tree.getExogenousVariables().stream().map(v-> (context.keySet().contains(v.getArgumentName())? new Argument(v.getArgumentName(),context.get(v.getArgumentName())) : v )) .collect(Collectors.toMap(Argument::getArgumentName, Argument::getArgumentValue));
	        // since the exos and the formulas are random, we need to get somevalues from the model
//	        1- the number of causes: 1 2 5 10 15 20 50 --> BM Param
	        // should make sure the number of causes is ok 
	        Set<Argument> causes = new HashSet<>();     
	        int numberofcauses=16;	
	        Random r = new Random();
	        for (int i =0; i<numberofcauses; i++) {
	        	int index = r.nextInt(tree.getEquationsSorted().size());
	        	 Argument e = tree.getEquationsSorted().get(index);
	        	if (!e.getArgumentName().equals("n_0"))	     // reserved for phi   	 
	        		causes.add(new Argument(e.getArgumentName(), e.getArgumentValue()));
	        	else {
	        		 Argument e2 = tree.getEquationsSorted().get(index--);
	        		 causes.add(new Argument(e2.getArgumentName(), e2.getArgumentValue()));
	        	}
	        }
	        
	        solver.setDEFAULT_BIG_M(1000000000);
	        solver.setDEFAULT_UPPER_BOUND(100000000.0);
	        solver.setDEFAULT_LOWER_BOUND(-100000000.0); 
	        
	        double rootValue = tree.getVaribale("n_0").getArgumentValue();
	    	Expression phi,phi2;
	        if (rootValue<0) {
	        	 phi = new Expression("n_0+"+(-1*rootValue)+"= 0", tree.getVaribale("n_0"));
	        	 phi2 = new Expression("n_0+"+((-1*rootValue)+100)+"!= 0", tree.getVaribale("n_0")); 
	        }
	        else {
	        	 phi = new Expression("n_0-"+rootValue+"= 0", tree.getVaribale("n_0"));
	        	 phi2 = new Expression("n_0-"+(rootValue+100)+"!= 0", tree.getVaribale("n_0")); 
	        }
	        System.out.println(phi.calculate()+""+ phi2.calculate());
	        tree.print();
	        CausalitySolverResult causalitySolverResultExpected =
	                new CausalitySolverResult(true, true, false, causes, new HashSet<>());
//	        testSolve(tree, context2, phi, causes, causalitySolverResultExpected);
	        testSolve(tree, context2, phi, causes, causalitySolverResultExpected);

	   }
	   
	   
	   
	   
	   
	   @Test
	    public void bostonHousingExample() throws Exception {
	        NumericCausalModel bostonModel = ExampleProvider.bostonHousingNumeric();
	        // this map is OK if it didn't contain every exo
	        Map<String,Double> context = new HashMap<String, Double>();
	        context.put("crim_exo", 6.0);
	        context.put("rm_exo", 6575.0);
	        
//	        // this statments sets the context from the map; it keeps older values if the exo is not in the map
	        Map<String,Double> context2 = bostonModel.getExogenousVariables().stream().map(v-> (context.keySet().contains(v.getArgumentName())? new Argument(v.getArgumentName(),context.get(v.getArgumentName())) : v )) .collect(Collectors.toMap(Argument::getArgumentName, Argument::getArgumentValue));
//	        
	        
	        Argument cause1 = new Argument("crim", 6.0);
	        Argument cause2 = new Argument("rm", 6575.0);
	        Argument cause3 = new Argument("tax", 296000);
	        Argument cause4 = new Argument("lstat", 4980);
	        
	        System.out.println(bostonModel.getVaribale("medv").getArgumentValue());
	        Set<Argument> cause = new HashSet<>(Arrays.asList(cause2,cause3));       
	        
	        
	        // this a contrstive way of adding questions; mind the bounds and bigm
	        Expression phi = new Expression("medv-33000000!=0", bostonModel.getVaribale("medv"));

	        bostonModel.print();
	        CausalitySolverResult causalitySolverResultExpected =
	                new CausalitySolverResult(true, true, true, cause, new HashSet<>());
	        testSolve(bostonModel, context2, phi, cause, causalitySolverResultExpected);
	      

	   }


    
}