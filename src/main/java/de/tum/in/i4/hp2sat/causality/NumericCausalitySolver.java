package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.io.parsers.ParserException;
import org.logicng.util.Pair;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.NumericCausalModel;
import org.mariuszgromada.math.mxparser.parsertokens.BinaryRelation;
import org.mariuszgromada.math.mxparser.parsertokens.Operator;
import org.mariuszgromada.math.mxparser.parsertokens.ParserSymbol;
import org.mariuszgromada.math.mxparser.parsertokens.Token;

import java.util.*;

import static de.tum.in.i4.hp2sat.causality.ILPSolverType.GUROBI;
//TODO fix the generality of CausalitySolver
public class NumericCausalitySolver extends CausalitySolver {
	private static final String DISTANCE_VAR_NAME = "res";
	private static final String IND_1_PREFIX = "IND1_";
	private static final String IND_2_PREFIX = "IND2_";
	private static final String SLACK_1_PREFIX = "SLA1_";
	private static final String SLACK_2_PREFIX = "SLA2_";
	private static final String DELTA_PREFIX = "DEL_";
	private static final String ABS_PREFIX = "ABS_";
	private static final String MIN_PREFIX = "MIN_";
	private  int DEFAULT_BIG_M= 1000000000;
	private  Double DEFAULT_UPPER_BOUND =  100000000.0;
	private  Double DEFAULT_LOWER_BOUND = -100000000.0;
	

	/**
	 * Overrides {@link CausalitySolver#solve(CausalModel, Set, Formula, Set, SolvingStrategy)}.
	 * Default SATSolver: MINISAT
	 *
	 * @param causalModel     the underlying causal model
	 * @param context         the context
	 * @param phi             the phi
	 * @param cause           the cause
	 * @param solvingStrategy the applied solving strategy
	 * @return for each AC, true if fulfilled, false else
	 * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
	 */
	@Override
	CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
			SolvingStrategy solvingStrategy) throws InvalidCausalModelException {
		throw new UnsupportedOperationException("should be changed in the interface, for now use the other solve function");
	}

	
	
	public CausalitySolverResult solve(NumericCausalModel causalModel,  Map<String, Double> context, Expression phi, Set<Argument> cause,
			SolvingStrategy solvingStrategy) throws InvalidCausalModelException, GRBException {
		return solve(causalModel, context, phi, cause, solvingStrategy, GUROBI);
	}

	
	
	/**
	 * Checks AC1, AC2 and AC3 given a causal model, a cause, a context and phi and a solving strategy.
	 *
	 * @param causalModel     the underlying causal model
	 * @param context         the context
	 * @param phi             the phi
	 * @param cause           the cause
	 * @param solvingStrategy the applied solving strategy
	 * @param satSolverType   the to be used SAT solver
	 * @return for each AC, true if fulfilled, false else
	 * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
	 * @throws GRBException 
	 */
	CausalitySolverResult solve(NumericCausalModel causalModel, Map<String, Double> context, Expression phi,
			Set<Argument> cause, SolvingStrategy solvingStrategy, ILPSolverType solverType)
					throws InvalidCausalModelException, GRBException {
		// set the values of the exo arguments
		setContext(causalModel, context);
		Pair<Boolean, Boolean> ac1Tuple = fulfillsAC1(causalModel, phi, cause);
		boolean ac1 = ac1Tuple.first() && ac1Tuple.second();
		boolean ac2 = false,ac3=false;
		Set<Argument> w ;
		Set<Argument> minimalCause ;
		// in ILP we want to check the two conditions in one-go
		Pair<Set<Argument>, Set<Argument>> ac2ac3 = fulfillsAC2AC3(causalModel, phi, cause,  solvingStrategy, solverType);

		minimalCause = ac2ac3.first();
		w = ac2ac3.second();

		if ( minimalCause!=null ){
			if (minimalCause.isEmpty()){// means we did n't have to flip anything, couldn't happen because of minimum distance constraint
				ac2=ac3=false;
				System.out.println("The effect is not at all affected by the cause");
			}
			else if (minimalCause.size() == cause.size()){// this means ac2 and ac3 are fulfilled 
				ac2=true;
				ac3=true;
				System.out.println("The cause is minimal and counterfactual.");
			}
			else // there is a more minimal cause
			{
				ac2 = true; // maybe this is a harsh conclusion 
				ac3 = false;
				System.out.println("There is a minimal subset of the cause, i.e,:"+ minimalCause);
			}

		}else{ // for the sake of unit-testing
			minimalCause = cause;
			w= new HashSet<>();
			}

		CausalitySolverResult<Argument> causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, minimalCause, w);
		return causalitySolverResult;
	}


	private void setContext(NumericCausalModel causalModel, Map<String, Double> context ) {
		
		// loop the exo vars and set their values according to the input map;
		// this should work since the argument is a call by reference 
		causalModel.getExogenousVariables().stream().forEach(n-> { n.setArgumentValue(context.get(n.getArgumentName()));});

	}
	
	
    Pair<Boolean, Boolean> fulfillsAC1(NumericCausalModel causalModel, Expression phi, Set<Argument> cause) {
//    	System.out.println(phi.calculate());
        boolean phiEvaluation = (phi.calculate() == 1.0)?  true: false;
        boolean causeEval= true;
        for (Argument argument : cause) {
        	// lookup by name the causal model variable
        	Argument modelVar = causalModel.getVariableEquationMap().get(argument.getArgumentName());
//        	System.out.println("cause equation "+modelVar.getArgumentExpressionString());
//        	System.out.println("input cause value "+argument.getArgumentValue());
			if (argument.getArgumentValue()!= modelVar.getArgumentValue())
				causeEval= false;
		}
        
//        System.out.println("AC1: Phi"+ phiEvaluation + " cause Evaluation "+causeEval);
 
        return new Pair<>(phiEvaluation, causeEval);
    }



	/**
	 * Checks if AC2 and AC3 are fulfilled. Combined approach that takes advantage of synergies between the separate
	 * approaches.
	 *
	 * @param causalModel     the underlying causal model
	 * @param phi             the phi
	 * @param cause           the cause for which we check AC2
	 * @param context         the context
	 * @param evaluation      the original evaluation of variables
	 * @param solvingStrategy the solving strategy
	 * @param satSolverType   the to be used SAT solver
	 * @param f               a formula factory
	 * @return a tuple of set W and a boolean value indicating whether AC3 is fulfilled or not
	 * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
	 * @throws GRBException 
	 */
	private Pair<Set<Argument>, Set<Argument>> fulfillsAC2AC3(NumericCausalModel causalModel, Expression phi, Set<Argument> cause,
			  SolvingStrategy solvingStrategy, ILPSolverType iLPSolverType) throws InvalidCausalModelException, GRBException {
		
		
		Pair<Set<Argument>, Set<Argument>> result;
		Set<String> phiNames = createListOfPhiNames(phi);
		// in this implementation we will not use a SAT formula
		//1. Generate the Gurobi model
		//create and configure the ilp parameters
		//create the ILP model vars and the auxiliary vars 
		GRBModel model = createGRBModel(causalModel, cause, phiNames);
		//add the constraints based on the formula
		addLPConstraints( model, causalModel, cause, phi);
		// write the model to file for debugging (should be stopped in benchmarks)
		model.write("./ILP-models/Numeric_"+causalModel.getName()+".lp");
		try {
			result = solveILP(model, causalModel,phi, cause);
		} catch (ParserException | GRBException e) {
			result= null;
			e.printStackTrace();
		}


		return result;
	}

	private Set<String> createListOfPhiNames(Expression phi) {
		Set<String> names = new HashSet<>();
		for (int i = 0; i < phi.getArgumentsNumber(); i++) {
			names.add(phi.getArgument(i).getArgumentName());
		}
		
		return names;
	}




	/** This function  solves the model using Groupi solver. 
	 * Lastly, interpret the reults to return two sets one for the minimal cause and one for the non minimal w
	 * AC2 and AC3 results are not directly returned, but they can be infered from minimal cause set (size >1 AC2 is satisfied, == cause.size ac3 is satisfied)
	 * @param cm
	 * @param phi
	 * @param satFormula
	 * @param cause
	 * @param evaluation
	 * @return: a pair, the first element is the minimal cause, the second is the non minimal w
	 * @throws ParserException
	 * @throws GRBException
	 * @throws InvalidCausalModelException
	 */
	public Pair<Set<Argument>, Set<Argument>>  solveILP(GRBModel model, NumericCausalModel cm, Expression phi, Set<Argument> cause ) throws ParserException, GRBException, InvalidCausalModelException {
		//	printProblem(satFormula, cause, evaluation);
	
		Set<Argument> minimalCause = new HashSet<>();
		Set<Argument> w = new HashSet<>();
		
		// solve the  model
		model.optimize();
		/**
		 * not Phi, context, equivalence or fixed value, no restrictions on x vector values but one should be different
		 * Then we find a solution that minimizes the delta d
		 * An optimal solution will allow us to conclude:
		 *  - Feasibility means x or a subset of x (size d if the vars are integers) fulfills ac2 {not phi, some W} and fulfills ac3 (if optimal)
		 *  - to determine minimal x, we choose the parts in the solution that are different than actual values  
		 *  - to determine W in that solution we take all vars (not in x) which values stayed the same, this is not a minimal w
		 *  - for a minimal w, add another objective variable 'fixed_size' fs, we need to minimize it, and it can be zero
		 *  - TODO: minimal w, we need to use the actual world values and the flipped world values to influence W. However this raises the issue of weather minimal x plays a role.
		 **/
		// status codes: http://www.gurobi.com/documentation/8.0/refman/optimization_status_codes.html
		// 	OPTIMAL	2	Model was solved to optimality (subject to tolerances), and an optimal solution is available.
		//INFEASIBLE	3	Model was proven to be infeasible.
		if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
			System.out.println ("Model is infeasible.");
			minimalCause = null;
			w=null;
		}else if ( model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
			int nSolutions = model.get(GRB.IntAttr.SolCount);
			GRBVar[] fvars = model.getVars();
			String[] vnames = model.get(GRB.StringAttr.VarName, fvars);
//			for (int e = 0; e < nSolutions; e++) {
			minimalCause = new HashSet<>();
			w = new HashSet<>();
//			model.set(GRB.IntParam.SolutionNumber, e);
			System.out.println(model.get(GRB.IntAttr.SolCount)+ "Solution found, distance value = "+ model .get(GRB.DoubleAttr.ObjVal)+" status= "+ model.get(GRB.IntAttr.Status));
				
			// use Xn when quering the nth solution
			double[] x = model.get(GRB.DoubleAttr.X, fvars);
			
			//by only the distance value "res" we can judge ac3 and conclude that this is a cause or not. so we can have impls that return from here.
			// interpret the solution
			for (int j = 0; j < fvars.length; j++) {
				String varName = vnames[j];
				if (cause.stream().anyMatch(element -> element.getArgumentName().equals(varName))) { // a solution value for a cause
					System.out.println(varName+" is part of the cause set");
					// get the variable in the original evaluation 
					Argument partialCause = cause.stream().filter(l -> l.getArgumentName().equals(varName)).findAny().get();
					if (partialCause.getArgumentValue() != x[j] ) {// then this part of the cause is  flipped
						System.out.println("Partial cause found "+ varName +" actual value is "+partialCause.getArgumentValue()+" solved value "+ x[j] );
						minimalCause.add(partialCause);
					} 	else{
						//TODO should we add those as W as an over-approximation 
						w.add(partialCause);
//						System.out.println("Model was solved without flipping "+ varName +" actual value is "+partialCause.phase() +" solved value "+ x[j]+". violation of ac3." );
					}
					continue;
				}
				else if (phi.getArgumentIndex(varName) != -1){// is from the effect variables
					continue;
				}
				else if (cm.getExogenousVariables().stream().filter(l->l.getArgumentName().equals(varName)).findAny().isPresent()){// exo variable
					continue;
				}
				else { 
					// last group of vars, unkown and possible W
					// get the variable literal in the original evaluation 
					Argument potentialWmember = cm.getEquationsSorted().stream().filter(l -> l.getArgumentName().equals(varName)).findAny()
							.orElse(null);
					if (potentialWmember==null){// could be auxiliary vars this one of the vars added to the ILP e.g.res
						continue;
					}
					// this could also be done by checking the auxilary vars value
					if ( potentialWmember.getArgumentValue() == x[j] ) {// value stayed the same
					//	System.out.println("W memeber found "+ varName +" actual value is "+potentialWmember.getArgumentValue()+" solved value "+ x[j] );
						w.add(potentialWmember);
					} 	
					continue;
				}
			}
		}
//		}
			else{
			System.out.println("Unhandled ILP status "+ model.get(GRB.IntAttr.Status) +" check ILP log" );
			minimalCause = null;
			w=null;
		}

		
		
		// Dispose of model and environment
		model.dispose();
		model.getEnv().dispose();

		return new Pair<>(minimalCause, w); 
	}



	/**
	 * a function that turns a custom causal model to Gurobi causal model the
	 * adds the original, the auxiliary variables, and the constraints for the auxiliary variables (except the distance)
	 * 
	 * @param cm
	 * @param model
	 */
	public GRBModel createGRBModel(NumericCausalModel cm, Set<Argument> cause, Set<String> phiNames) throws GRBException {
		
		// basic GRB model, parameters of the model should be set here
		GRBEnv env = new GRBEnv("./Numeric-ILP-LOG/"+cm.getName()+".log");
		GRBModel model = new GRBModel(env);

		// start with exo vars
		cm.getExogenousVariables().forEach(item -> {
			try {
				//TODO integer vars for now 
				model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, item.getArgumentName());

			} catch (GRBException e) {
				e.printStackTrace();
			}
		});
		
		GRBLinExpr expr = new GRBLinExpr();
		GRBLinExpr causeExpr = new GRBLinExpr();
		cm.getEquationsSorted().forEach(e -> {
			try {
				//TODO bounds
				GRBVar variable = model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, e.getArgumentName());
				// for cause vars we add auxiliary variables for the distance calculation
				// res = segma Di Di = Min (1, Absolute (x-x')); 
				
				// namely delta = x - x' abs = abs (delta) min= min (1, abs) 
				//TODO how about continuous variables won't work with min 1
				
				if (cause.stream().anyMatch(x-> x.getArgumentName().equals(e.getArgumentName()))) {
					System.out.println(e.getArgumentName()+" is a cause var");
					// TODO bounds
					GRBVar delta = model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, DELTA_PREFIX + e.getArgumentName());
					GRBVar abs = model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, ABS_PREFIX + e.getArgumentName());
					// TODO Binary ?
					GRBVar min = model.addVar(0.0, 1.00, 0.0, GRB.BINARY, MIN_PREFIX + e.getArgumentName());
					
					// Add the constraints of the distance
					// the delta equation
					expr.addTerm(1.0,variable);
					expr.addConstant(- e.getArgumentValue());
					model.addConstr(delta, GRB.EQUAL, expr, "delta_"+e.getArgumentName());
					expr.clear();
					// abs = abs(delta)  addGenConstrAbs(y, x)
					model.addGenConstrAbs(abs, delta, "abs_"+e.getArgumentName());
					model.addGenConstrMin(min, new GRBVar[] {abs},1.0, "min_"+e.getArgumentName());
					
					// append to an expression for the sigma
					causeExpr.addTerm(1.0, min);
				}	// for vars not in the effect add 4 vars: 2 binary and 2 integer?
				else if (!phiNames.contains(e.getArgumentName())) {
//					System.out.println(e.getArgumentName()+" is a normal var");
					GRBVar indicator1 = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, IND_1_PREFIX + e.getArgumentName());
					GRBVar indicator2 = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, IND_2_PREFIX + e.getArgumentName());
					// TODO bounds are set based on default setting
					GRBVar slack1 = model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, SLACK_1_PREFIX + e.getArgumentName());
					GRBVar slack2 = model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, SLACK_2_PREFIX + e.getArgumentName());
					// does this work here after adding the vars directly
					// add the constraints of the auxiliary vars
					//wk ≤ M(1− y)  --> M - My 			
					expr.addConstant(DEFAULT_BIG_M);
					expr.addTerm(-DEFAULT_BIG_M, indicator1);
					model.addConstr(slack1, GRB.LESS_EQUAL, expr, "SLACK_11_"+ e.getArgumentName());
					expr.clear();
					//wk ≥ −M(1− y)--> -M + My
					expr.addConstant(-DEFAULT_BIG_M);
					expr.addTerm(DEFAULT_BIG_M, indicator1);
					model.addConstr(slack1, GRB.GREATER_EQUAL, expr, "SLACK_12_"+ e.getArgumentName());
					expr.clear();

					// slack and indictaor two 
					//w2 ≤ M(1− y)  --> M - My 			
					expr.addConstant(DEFAULT_BIG_M);
					expr.addTerm(-DEFAULT_BIG_M, indicator2);
					model.addConstr(slack2, GRB.LESS_EQUAL, expr, "SLACK_21_"+ e.getArgumentName());
					expr.clear();
					//wk ≥ −M(1− y)--> -M + My
					expr.addConstant(-DEFAULT_BIG_M);
					expr.addTerm(DEFAULT_BIG_M, indicator2);
					model.addConstr(slack2, GRB.GREATER_EQUAL, expr, "SLACK_22_"+ e.getArgumentName());
					expr.clear();
					
					
					// connect the two indicators with a constraint 
					expr.addTerm(1, indicator1);
					expr.addTerm(1, indicator2);
					model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "Indicators_"+ e.getArgumentName());
					expr.clear();
				}
				expr.clear();
			} catch (GRBException e1) {
				e1.printStackTrace();
			}
		});
		
		//1. distance = segma
		GRBVar distance = model.addVar(1.0, cause.size(), 0.0, GRB.INTEGER, DISTANCE_VAR_NAME);
		model.addConstr(distance, GRB.EQUAL, causeExpr, "R_result");
		
		// options for the solving
		// TODO keep as a different solving strategy
		// use systematic search to find kth best solutions
		model.set(GRB.IntParam.PoolSearchMode, 2);
		// k is defined here:
		model.set(GRB.IntParam.PoolSolutions, 5);
		// don't look for any solution with objective value other than the optimal (Gap =0)
		model.set(GRB.DoubleParam.PoolGap,0);
		
		// Apparently should call this method so that the model is in sync
		model.update();
		return model;

	}

	
	/**
	 * a function that converts CNF clauses to GRB constraints
	 * 
	 * @param fm
	 * @param model
	 */
	public void addLPConstraints(GRBModel model, NumericCausalModel cm, Set<Argument> cause, Expression phi) throws GRBException {

	
		// add constraints to the ILP model based 
		// loop the model variables to add constraints
		//1. Exos: Ex1 = 5 --> Ex1 - 5 = 0
		//TODO we can add this as part of vars loop
		cm.getExogenousVariables().forEach(item -> {
			try {
				GRBLinExpr	exoExpr = new GRBLinExpr();
				exoExpr.addTerm(1.0, model.getVarByName(item.getArgumentName()));
				model.addConstr(exoExpr, GRB.EQUAL, item.getArgumentValue(), "EXO_" +item.getArgumentName() );
			} catch (GRBException e) {
				e.printStackTrace();
			}
		});

		//3. Models equations 
		// loop all the endogenous variables
		Iterator<Argument> iter = cm.getEquationsSorted().iterator();
		while (iter.hasNext()) {
			Argument variable = iter.next();
			String varName = variable.getArgumentName();
			// ignore the variable if it is a cause var
			//TODO double check if this works as a reference comparison or should do a name lookup
			if (cause.stream().anyMatch(e-> e.getArgumentName().equals(variable.getArgumentName()))) {
				// we already added the distance, maybe can be done here instead
//				System.out.println(variable.getArgumentName()+ " is a cause being skipped");
				continue;
			}
			// effect variables formulas; we don't need the disjunctive constraint  
			if (phi.getArgumentIndex(varName)!= -1) { 
//				System.out.println(variable.getArgumentName()+ "is a phi var");
				addLinearEquality(variable, cm.getExpression(variable), model, false);
								
			} else {// Variables not in cause or effect 
//				System.out.println(variable.getArgumentName()+ " is a normal var");
				addLinearEquality(variable, cm.getExpression(variable), model, true);
			}

		}
				
		//2. Effect: should be "negated", i.e., takes a different value 
		// we formalize this in a constraint as |PhiExpression - value|>0 e.g. X=4 --> |x-4|>0 or x-y=1 --> |x-y-1|>0
		addPhiNegation(phi,model);
		
		// Set objective: minimize res
		GRBLinExpr expr = new GRBLinExpr();
		expr.addTerm(1.0, model.getVarByName(DISTANCE_VAR_NAME));
		model.setObjective(expr, GRB.MINIMIZE);
		model.update();
	}
	
	
	// this function can be used for representing the linear equalities of variables. We use this 
	// for Phi vars and the disjunctive constraints
	// Examples OF fX in mind:  X + y +4 ;-x - y- 4;3*Y + 5*x -40; -3 *Y - 5*X + 40 ; 3 + Z; 400; y
	private  void addLinearEquality (Argument variable, Expression equation, GRBModel model, boolean addSlack) throws GRBException {
		// we need to turn the equation to the equality: Fx - V = 0 
		
		List<Token> tokens = equation.getCopyOfInitialTokens();
		// defaults
		Double currentCoefficient = 1.0;
		double sign = 1.0; // 1.0 or -1.0 
		GRBLinExpr expr = new GRBLinExpr();
		GRBVar currentArgument = null;
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			// loop the tokens if tokentypeid= 101 which is an argument; 
			// then get the Gurobi variable of this;
			// each term is an operator, coefficient, and an argument. A term could also be a constant 
			if (token.tokenTypeId == Argument.TYPE_ID) {
				currentArgument = model.getVarByName(token.tokenStr);
				// special cases:   we are at the end
				if ( i==tokens.size()-1) { 
//					System.out.println(i+" "+token.tokenStr+  " "+variable.getArgumentName());
					expr.addTerm(currentCoefficient*sign, currentArgument);
					currentArgument=null;
				}
				
			} else if (token.tokenTypeId == ParserSymbol.NUMBER_TYPE_ID) {
				// if it was type id = 0 then it is a 
				//number that can be a coefficient or a constant in an equation 
				currentCoefficient = token.tokenValue;
				if (i==tokens.size()-1) {// if this is the last element (number) could also work for the first
					expr.addConstant(currentCoefficient*sign);
				}
			}
			else if (token.tokenTypeId == Operator.TYPE_ID) {// + - * /...
				if ((token.tokenId == Operator.PLUS_ID) || (token.tokenId == Operator.MINUS_ID) ) {
					// when i get a +- not at the beginning i add the previous Term or constant, and use the operator as sign for next
					if ( i != 0 ) { 
						if (currentArgument!= null) {
							expr.addTerm(currentCoefficient*sign, currentArgument);
							}
						else if (currentCoefficient!=null) {
							expr.addConstant(currentCoefficient*sign);
						}
					}
					// set next term sign
					sign = (token.tokenId == Operator.PLUS_ID)? 1.0: -1.0;
					currentCoefficient= 1.0;
					currentArgument = null;
				}else {

					// this is a *; should we do anything
				}

			}
//			else if (token.NOT_MATCHED==1) { // TODO handle unmatched tokens
//
//			}
		}
		// The expression so far simulates Fx; Now Fx-V = 0 
		expr.addTerm(-1.0, model.getVarByName(variable.getArgumentName()));
		if (addSlack) {
			expr.addTerm(1.0, model.getVarByName(SLACK_1_PREFIX+variable.getArgumentName()));
			// the constraints of the slack and the indicator vars are already added; this would be another location to add them
			// Now lets add the second constraint to account fow W; V - v + w2 = 0
			GRBLinExpr contingencyExpression = new GRBLinExpr();
			contingencyExpression.addTerm(1, model.getVarByName(variable.getArgumentName()));
			contingencyExpression.addConstant(-variable.getArgumentValue());
			contingencyExpression.addTerm(1,model.getVarByName(SLACK_2_PREFIX+variable.getArgumentName()));
			
			model.addConstr(contingencyExpression, GRB.EQUAL, 0.0, "R_2_" +variable.getArgumentName());
		}
		// finalize the constraint and add it 
		model.addConstr(expr, GRB.EQUAL, 0.0, "R_" +variable.getArgumentName());

	}
	
	// for now assume the phi expression is as x-4=0 if not the client of this function should convert it
	//e.g. x-y=1 --> x-y-1=0; x-5 != 0
		private  void addPhiNegation ( Expression equation, GRBModel model) throws GRBException {
			List<Token> tokens = equation.getCopyOfInitialTokens();
			Double currentCoefficient = 1.0;
			double sign = 1.0; // 1.0 or -1.0 
			GRBLinExpr expr = new GRBLinExpr();
			GRBVar currentArgument = null;
			boolean isContrastive= false;
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				// each term is an operator, coefficient, and an argument. A term could also be a constant 
				if (token.tokenTypeId == Argument.TYPE_ID) {
					currentArgument = model.getVarByName(token.tokenStr);
					if ( i==tokens.size()-3) { 
						expr.addTerm(currentCoefficient*sign, currentArgument);
						currentArgument=null;
					}
					
				} else if (token.tokenTypeId == ParserSymbol.NUMBER_TYPE_ID) {
					// if it was type id = 0 then it is a 
					//number that can be a coefficient or a constant in an equation 
					currentCoefficient = token.tokenValue;
					if (i==tokens.size()-3) {// if this is the last element (number) excluding = and 0
						expr.addConstant(currentCoefficient*sign);
					}
				}
				else if (token.tokenTypeId == Operator.TYPE_ID) {// + - * /...
					// if it was 1 then it is an operator 
					if ((token.tokenId == Operator.PLUS_ID) || (token.tokenId == Operator.MINUS_ID) ) {
						// when i get a +- not at the beginning i add the previous Term or constant, and use the operator as sign for next
						if ( i != 0 ) { 
							if (currentArgument!= null)
								expr.addTerm(currentCoefficient*sign, currentArgument);
							else if (currentCoefficient!=null)
								expr.addConstant(currentCoefficient*sign);
						}
						// set next term sign
						sign = (token.tokenId == Operator.PLUS_ID)? 1.0: -1.0;
						currentCoefficient= null;
						currentArgument = null;
					}else {

						// this is a *; should we do anything
					}

				}
				
				else if (token.tokenTypeId == BinaryRelation.TYPE_ID) {// =;!=
					// in case of not equal as a contrastive should be handled
					if ((token.tokenId == BinaryRelation.NEQ_ID)) {
					// this is a phi expression with an inequality != or ~=, so its contrastive
					// we do nothing but set a flag that should govern the expression operator 
						isContrastive =true; 
						System.out.println("contrastive");
						}

				}
				
				else if (token.NOT_MATCHED==1) { // TODO handle unmatched tokens

				}
				// there is also the = in this case; we are not handling because we assume x-8=0 format
			}
			
			//TODO Bounds
			GRBVar phi =  model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, "PHI");
			// Phi = X-4 assumed to equal 0
			model.addConstr(expr, GRB.EQUAL, phi, "R_PHI_value" );
			expr.clear();
			// PhiAbs = |phi|
			GRBVar phiAbs =  model.addVar(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0.0, GRB.INTEGER, "PHI_ABS");
			model.addGenConstrAbs(phiAbs, phi, "R_PHI_ABS");
			// PhiAbs>0
			//TODO for continuous vars maybe the delta is less than one 
			if (!isContrastive)
				model.addConstr(phiAbs, GRB.GREATER_EQUAL, 1.0, "R_PHI_intervention" );
			else // this is the case for contrastive query; phi is of the form y-4 != 0; its negation is y-4 =0
				model.addConstr(phiAbs, GRB.EQUAL, 0.0, "R_PHI_intervention" );
			// in summary for the effect we add two variables and 3 constraints

		}



		public int getDEFAULT_BIG_M() {
			return DEFAULT_BIG_M;
		}



		public void setDEFAULT_BIG_M(int dEFAULT_BIG_M) {
			DEFAULT_BIG_M = dEFAULT_BIG_M;
		}



		public Double getDEFAULT_UPPER_BOUND() {
			return DEFAULT_UPPER_BOUND;
		}



		public void setDEFAULT_UPPER_BOUND(Double dEFAULT_UPPER_BOUND) {
			DEFAULT_UPPER_BOUND = dEFAULT_UPPER_BOUND;
		}



		public Double getDEFAULT_LOWER_BOUND() {
			return DEFAULT_LOWER_BOUND;
		}



		public void setDEFAULT_LOWER_BOUND(Double dEFAULT_LOWER_BOUND) {
			DEFAULT_LOWER_BOUND = dEFAULT_LOWER_BOUND;
		}



		public NumericCausalitySolver(int dEFAULT_BIG_M, Double dEFAULT_UPPER_BOUND, Double dEFAULT_LOWER_BOUND) {
			super();
			DEFAULT_BIG_M = dEFAULT_BIG_M;
			DEFAULT_UPPER_BOUND = dEFAULT_UPPER_BOUND;
			DEFAULT_LOWER_BOUND = dEFAULT_LOWER_BOUND;
		}
		public NumericCausalitySolver() {
		}
}
