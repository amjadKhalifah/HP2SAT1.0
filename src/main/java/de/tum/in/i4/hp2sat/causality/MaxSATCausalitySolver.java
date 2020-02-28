package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.solvers.MaxSATSolver;
import org.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import org.logicng.solvers.maxsat.algorithms.MaxSAT.MaxSATResult;
import org.logicng.solvers.maxsat.algorithms.MaxSATConfig.Builder;
import static org.logicng.solvers.maxsat.algorithms.MaxSATConfig.Verbosity.SOME;
import org.logicng.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class MaxSATCausalitySolver extends CausalitySolver {

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
	public	CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
			SolvingStrategy solvingStrategy) throws InvalidCausalModelException {
		return solve(causalModel, context, phi, cause);
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
	 */
	CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
			Set<Literal> cause  )
					throws InvalidCausalModelException {
		FormulaFactory f = new FormulaFactory();
		Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context);
		Pair<Boolean, Boolean> ac1Tuple = fulfillsAC1(evaluation, phi, cause);
		boolean ac1 = ac1Tuple.first() && ac1Tuple.second();
		boolean ac2 = false,ac3=false;
		Set<Literal> w ;
		Set<Literal> minimalCause ;
		// in maxsat we want to check the two conditions in one-go
		Pair<Set<Literal>, Set<Literal>> ac2ac3 = fulfillsAC2AC3(causalModel, phi, cause, context, evaluation,
				 f);

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

		//TODO maybe here filter the w based on relevance to x.
		CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, minimalCause, w);
		return causalitySolverResult;
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
	 */
	private Pair<Set<Literal>, Set<Literal>> fulfillsAC2AC3(CausalModel causalModel, Formula phi, Set<Literal> cause,
			Set<Literal> context, Set<Literal> evaluation, FormulaFactory f) throws InvalidCausalModelException {
		// in ILP we don't handle the but-for explicitly, because we are considering the 
		Pair<Set<Literal>, Set<Literal>> result;
		Formula phiNegated = f.not(phi);
		MaxSATSolver formula = generateSATQuery(causalModel, phiNegated, cause, context, evaluation, true,f);
		try {
			result = solveMaxSat(causalModel,phi, formula, cause, evaluation);
		} catch (ParserException  e) {
			result= null;
			e.printStackTrace();
		}


		return result;
	}



	/**
	 * Generates a formula whose satisfiability indicates whether AC2 is fulfilled or not.
	 *
	 * @param causalModel the causal model
	 * @param notPhi      the negated phi
	 * @param cause       the cause
	 * @param context     the context
	 * @param evaluation  the original evaluation under the given context
	 * @param ac3         set to true if used within AC3 check
	 * @param f           a formula factory
	 * @return a formula
	 */
	private MaxSATSolver generateSATQuery(CausalModel causalModel, Formula notPhi, Set<Literal> cause,
			Set<Literal> context, Set<Literal> evaluation, 
			boolean ac3, FormulaFactory f) {
		//TODO which config 
//		final PrintStream logStream = new PrintStream("src/test/resources/partialweightedmaxsat/log.txt");
		final MaxSATConfig[] configs = new MaxSATConfig[3];
		configs[0] = new Builder().weight(MaxSATConfig.WeightStrategy.NONE).verbosity(SOME).build();
		configs[1] = new Builder().weight(MaxSATConfig.WeightStrategy.NORMAL).verbosity(SOME).build();
		configs[2] = new Builder().weight(MaxSATConfig.WeightStrategy.DIVERSIFY).verbosity(SOME).build();
		final MaxSATSolver solver = MaxSATSolver.wbo(configs[0]);
		
		
		
		// get all variables in cause
		Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
		// create map of variables and corresponding evaluation
		Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
				.collect(Collectors.toMap(Literal::variable, Function.identity()));
		// create formula: !phi AND context
		Formula formula = f.and(notPhi, f.and(context));
		// the negation of phi and the context are hard caluses
		solver.addHardFormula(formula);
		

		for (Equation equation : causalModel.getVariableEquationMap().values()) {
			// get value of variable in original iteration
			Literal originalValue = variableEvaluationMap.get(equation.getVariable());
			Formula equationFormula;
			/* When generating a SAT query for AC3, then for each variable not in the cause, we stick to the same
			 * scheme as for AC2, i.e. (V_originalValue OR (V <=> Formula_V)).*/
			
			if (!causeVariables.contains(equation.getVariable())) {
				equationFormula = f.or(originalValue, f.equivalence(equation.getVariable(), equation.getFormula()));
				solver.addHardFormula(equationFormula);
			}
		
			else {// in maxSAT cause vars are soft clauses added after
				// maxSAT first option: Xorig
				solver.addSoftFormula(originalValue, 1);
				// for option we can add it here 
			}
			
		}
		
		
		 /*
         * We want to extend the SAT formula such that it is only satisfiable for a subset of the cause. We do
         * this, by specifying that NOT all the cause variables are allowed NOT to follow their equation and NOT
         * be equal to their original value, and NOT all of them are allowed to follow their equation. Put
         * differently, at least one cause variables (but not all) must violate its equation while NOT
         * following its original value. Also, not all variables are allowed to obtain their original value.
         * */

         Formula formula3 = f.verum();
        for (Literal l : cause) {
            Variable causeVariable = l.variable();
            Literal originalValue = variableEvaluationMap.get(causeVariable);
            formula3 = f.and(formula3, originalValue);
        }
        // add negated formulas by AND
        formula = f.and(f.not(formula3));
		solver.addHardFormula(formula);
		
//		System.out.println();
		return solver;
	}



	/** This function prepares the ILP program based on the cm and the sat formula
	 * Then solves the model using Groupi solver. 
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
	public Pair<Set<Literal>, Set<Literal>>  solveMaxSat(CausalModel cm, Formula phi, MaxSATSolver maxsatSolver, Set<Literal> cause,  Set<Literal> evaluation ) throws ParserException,  InvalidCausalModelException {
		//		printProblem(satFormula, cause, evaluation);
	     

		
		Pair<Set<Literal>, Boolean> result;
	
		Set<Literal> minimalCause = new HashSet<>();
		Set<Literal> w = new HashSet<>();

		
		// solve the  model
		MaxSATResult   maxSatResult = maxsatSolver.solve();
		// Feasibility means that there is x or a subset of it satisfy AC2 and AC3. We don't know anything about W yet
		// to interpret the result; 1- it should be solved (status should be not 3, not sure if there are other statuses than 2 optimal http://www.gurobi.com/documentation/8.0/refman/optimization_status_codes.html)
		// 2- conclude the satisfying part of x
		// 3- conclude the required W
		// 4- would W really be correct
		/**
		 * G Formula contains: not Phi, context, equivalence or fixed value, no restrictions on x vector values but one should be different
		 * Then we find a solution that minimizes the delta d
		 * An optimal solution will allow us to conclude:
		 *  - Feasibility means x or a subset of x (size d) fulfills ac2 {not phi, some W} and fulfills ac3 (if optimal)
		 *  - to determine minimal x, we choose the parts in the solution that are different than actual values  
		 *  - to determine W in that solution we take all vars (not in x) which values stayed the same, this is not a minimal w
		 *  - for a minimal w, add another objective variable 'fixed_size' fs, we need to minimize it, and it can be zero
		 *  - TODO: minimal w, we need to use the actual world values and the flipped world values to influence W. However this raises the issue of weather minimal x plays a role.
		 **/

		if (maxSatResult == MaxSATResult.UNSATISFIABLE) {
			System.out.println ("Formula unsatisfiable");
			minimalCause = null;
			w=null;
		}else if (maxSatResult == MaxSATResult.OPTIMUM) {
			System.out.println( "Solution found, cost value = "+ maxsatSolver.result());
				
			Assignment maxSatAssignments = maxsatSolver.model();
			//by only the distance value "res" we can judge ac3 and conclude that this is a cause or not. so we can have impls that return from here.
			// interpret the solution
			Iterator<Literal> literals= maxSatAssignments.literals().iterator();
			while (literals.hasNext()) {
				Literal literal = literals.next();
				String varName = literal.name();
				if (cause.stream().anyMatch(element -> element.name().equals(varName))) { // a solution value for a cause
					System.out.println(varName+" is part of the cause set");
					// get the variable literal in the original evaluation 
					Literal partialCause = evaluation.stream().filter(l -> l.name().equals(varName)).findAny().get();
					if (partialCause.phase() != literal.phase()) {// then this part of the cause is  flipped TODO maybe exclude the ST SH here
						System.out.println("Partial cause found "+ varName +" actual value is "+partialCause.phase() +" solved value "+ literal.phase() );
						minimalCause.add(partialCause);
					} 	else{
						//TODO should we add those as W as an over-approximation 
						w.add(partialCause.variable());
//						System.out.println("Model was solved without flipping "+ varName +" actual value is "+partialCause.phase() +" solved value "+ x[j]+". violation of ac3." );
					}
					continue;
				}
				else if (phi.containsVariable(varName)){// is from the effect variables
					continue;
				}
				else if (cm.getExogenousVariables().stream().filter(l->l.name().equals(varName)).findAny().isPresent()){// exo variable
					continue;
				}
				else {// last group of vars, unkown and possible W
					// get the variable literal in the original evaluation 
					Literal potentialWmember = evaluation.stream().filter(l -> l.name().equals(varName)).findAny()
							.orElse(null);
					if (potentialWmember==null){// this one of the vars added to the ILP e.g.res
						continue;
					}
					if ( potentialWmember.phase() == literal.phase()) {// value stayed the same
//						System.out.println("W memeber found "+ varName +" actual value is "+potentialWmember.phase() +" solved value "+ x[j] );
						w.add(potentialWmember.variable());
					} 	
					continue;
				}
			
		}}
			else{
			System.out.println("Unhandled Maxsat status "+ maxsatSolver.result()  );
			minimalCause = null;
			w=null;
		}

		

		return new Pair<>(minimalCause, w); 
	}


	private void printProblem(Formula satFormula, Set<Literal> cause, Set<Literal> evaluation) {
		System.out.println("SATFormula*****"+satFormula);	
		cause.forEach(c -> System.out.println(c.name()+""+c.phase()));
		System.out.println("evaluation");
		evaluation.forEach(c -> System.out.println(c.name()+""+c.phase()));
	}
	   
}
