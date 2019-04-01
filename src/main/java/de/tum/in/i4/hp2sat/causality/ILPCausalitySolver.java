package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.ILPSolverType.GUROBI;
import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.*;

class ILPCausalitySolver extends CausalitySolver {
	private static final String DISTANCE_VAR_NAME = "res";

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
	 */
	CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
			Set<Literal> cause, SolvingStrategy solvingStrategy, ILPSolverType solverType)
					throws InvalidCausalModelException {
		FormulaFactory f = new FormulaFactory();
		Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context);
		Pair<Boolean, Boolean> ac1Tuple = fulfillsAC1(evaluation, phi, cause);
		boolean ac1 = ac1Tuple.first() && ac1Tuple.second();
		boolean ac2 = false,ac3=false;
		Set<Literal> w ;
		Set<Literal> minimalCause ;
		// in ILP we want to check the two conditions in one-go
		Pair<Set<Literal>, Set<Literal>> ac2ac3 = fulfillsAC2AC3(causalModel, phi, cause, context, evaluation,
				solvingStrategy, solverType, f);

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
			Set<Literal> context, Set<Literal> evaluation, SolvingStrategy solvingStrategy, ILPSolverType iLPSolverType,
			FormulaFactory f) throws InvalidCausalModelException {
		// in ILP we don't handle the but-for explicitly, because we are considering the 
		Pair<Set<Literal>, Set<Literal>> result;
		Formula phiNegated = f.not(phi);
		Formula formula = generateSATQuery(causalModel, phiNegated, cause, context, evaluation, solvingStrategy, true,f);
		try {
			result = solveILP(causalModel,phi, formula, cause, evaluation);
		} catch (ParserException | GRBException e) {
			result= null;
			e.printStackTrace();
		}


		return result;
	}

	/**
	 * Compute a not necessarily minimal W.
	 *
	 * @param causalModelModified causal model where the equations of the cause are replaced respectively
	 * @param evaluation          the evaluation in the original causal model
	 * @param assignment          a satisfying assignment
	 * @return a set W if AC2 is fulfilled; null otherwise
	 */
	private Set<Literal> getWStandard(CausalModel causalModelModified, Set<Literal> evaluation, Assignment assignment) {
		// generate (maximum) W
		Set<Literal> w = assignment.literals().stream()
				.filter(l -> evaluation.contains(l)
						&& !causalModelModified.getExogenousVariables().contains(l.variable()))
				.collect(Collectors.toSet());
		return w;
	}

	/**
	 * Computes a minimal W.
	 *
	 * @param causalModelModified causal model where the equations of the cause are replaced respectively
	 * @param evaluation          the evaluation in the original causal model
	 * @param assignments         list of satisfying assignments
	 * @return a set W if AC2 is fulfilled; null otherwise
	 */
	private Set<Literal> getWMinimal(CausalModel causalModelModified, Set<Literal> evaluation,
			List<Assignment> assignments) {
		Set<Literal> w = null;
		Map<Variable, Equation> variableEquationMap = causalModelModified.getVariableEquationMap();
		// loop through all satisfying assignments; the first one found might not expose a minimal W
		for (Assignment assignment : assignments) {
			/*
			 * we construct a set of literals that are possibly in W. This set is equal to the one constructed in
			 * the standard approach
			 * */
			Set<Literal> wCandidates = assignment.literals().stream()
					.filter(l -> evaluation.contains(l)
							&& !causalModelModified.getExogenousVariables().contains(l.variable()))
					.collect(Collectors.toSet());

			Set<Literal> newW = new HashSet<>();
			for (Literal wCandidate : wCandidates) {
				// create an assignment instance where the current wCandidate is removed
				Assignment assignmentNew = new Assignment(assignment.literals().stream()
						.filter(l -> !l.variable().equals(wCandidate.variable())).collect(Collectors.toSet()));
				// compute the value of the current wCandidate using its equation
				boolean value = variableEquationMap.get(wCandidate.variable()).getFormula().evaluate(assignmentNew);
				/*
				 * if the value of the satisfying assignment and the value computed from the equation are
				 * different, than we know that the current variable needs to be in W, since we need to keep it to
				 * its original value such that the formula can be satisfied. */
				if (value != wCandidate.phase()) {
					newW.add(wCandidate);
				}
			}

			if (newW.size() == 1) {
				// if we have found a W of size 1, it cannot get smaller and we can directly return it
				return newW;
			} else if (w == null || newW.size() < w.size()) {
				// update W only if it has not been set so far or if we have found a smaller W
				w = newW;
			}
		}

		return w;
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
	private Formula generateSATQuery(CausalModel causalModel, Formula notPhi, Set<Literal> cause,
			Set<Literal> context, Set<Literal> evaluation, SolvingStrategy solvingStrategy,
			boolean ac3, FormulaFactory f) {
		// get all variables in cause
		Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
		// create map of variables and corresponding evaluation
		Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
				.collect(Collectors.toMap(Literal::variable, Function.identity()));
		// create formula: !phi AND context
		Formula formula = f.and(notPhi, f.and(context));

		Set<Variable> wVariablesOptimized = null;
		if (solvingStrategy == SAT_OPTIMIZED_W || solvingStrategy == SAT_OPTIMIZED_W_MINIMAL) {
			wVariablesOptimized = CausalitySolver.getMinimalWVariables(causalModel, notPhi, cause, f);
		}
		Set<Variable> variablesAffectingPhi = null;
		if (solvingStrategy == SAT_OPTIMIZED_FORMULAS || solvingStrategy == SAT_OPTIMIZED_FORMULAS_MINIMAL) {
			variablesAffectingPhi = CausalitySolver.getReachableVariables(causalModel.getGraphReversed(),
					notPhi.literals(), f);
		}

		for (Equation equation : causalModel.getVariableEquationMap().values()) {
			// get value of variable in original iteration
			Literal originalValue = variableEvaluationMap.get(equation.getVariable());
			Formula equationFormula;
			/*
			 * When generating a SAT query for AC3, then for each variable not in the cause, we stick to the same
			 * scheme as for AC2, i.e. (V_originalValue OR (V <=> Formula_V)).
			 *
			 * OPTIMZED_W Strategy: if the variable of the current equation is in the cause or not in the set of
			 * optimized variables, then we do not allow for its original value and just add (V <=> Formula_V).
			 *
			 * OPTIMIZED_CLAUSES Strategy: if the variable of the current equation is in the cause or not in the
			 * set of variables that affect phi, we just add TRUE to the formula*/
			if (!causeVariables.contains(equation.getVariable()) &&
					((solvingStrategy == SAT_OPTIMIZED_W || solvingStrategy == SAT_OPTIMIZED_W_MINIMAL) &&
							!wVariablesOptimized.contains(equation.getVariable()))) {
				equationFormula = f.equivalence(equation.getVariable(), equation.getFormula());
			} else if (!causeVariables.contains(equation.getVariable()) &&
					(solvingStrategy == SAT_OPTIMIZED_FORMULAS || solvingStrategy == SAT_OPTIMIZED_FORMULAS_MINIMAL)
					&& !variablesAffectingPhi.contains(equation.getVariable())) {
				equationFormula = f.verum();
			} else if (!causeVariables.contains(equation.getVariable())) {
				equationFormula = f.or(originalValue, f.equivalence(equation.getVariable(), equation.getFormula()));
			}
			/*
			 * If however the variable of the current equation in in the cause, we additionally add an OR with its
			 * negation. That is, we allow its original value, the negation of this original value and the
			 * equivalence with its equation. The resulting formula would be
			 * (V_originalValue OR (V <=> Formula_V) OR not(V_originalValue)). Obviously, we could replace that
			 * with TRUE or at least simplify it to (V_originalValue OR not(V_originalValue)). However, when
			 * replacing it by TRUE, we might run into the problem that some variables are removed completely
			 * from the formula which causes problem with the evaluation of some equations later on.
			 * Therefore, we want to keep at least (V_originalValue OR not(V_originalValue)). Unfortunately,
			 * LogicNG automatically replaces this formula by TRUE. To avoid this, we introduce a dummy variable
			 * as follows: (V_originalValue OR (not(V_originalValue) AND dummy))
			 * The dummy variable has no effect on the final result. */
			else {
				equationFormula = f.or(originalValue, originalValue.negate());
			}
			// add created formula to global formula by AND
			formula = f.and(formula, equationFormula);
		}
		return formula;
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
	public Pair<Set<Literal>, Set<Literal>>  solveILP(CausalModel cm, Formula phi, Formula satFormula, Set<Literal> cause,  Set<Literal> evaluation ) throws ParserException, GRBException, InvalidCausalModelException {
		//		printProblem(satFormula, cause, evaluation);
		Pair<Set<Literal>, Boolean> result;
		// create and configure the ilp vars
		//copy the variables of the causal model to ILP model
		GRBModel model = createGRBModel(cm, cause.size());
		Set<Literal> minimalCause = new HashSet<>();
		Set<Literal> w = new HashSet<>();
		//add the constraints based on the formula
		addLPConstraints(satFormula, model, evaluation, cause);
		// write the model to file for debugging (should be stopped in benchmarks)
		model.write("./ILP-models/ptest"+cm.getName()+".lp");
		// solve the  model
		model.optimize();
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
		// status codes: http://www.gurobi.com/documentation/8.0/refman/optimization_status_codes.html
		// 	OPTIMAL	2	Model was solved to optimality (subject to tolerances), and an optimal solution is available.
		//INFEASIBLE	3	Model was proven to be infeasible.
		if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
			System.out.println ("Model is infeasible.");
			minimalCause = null;
			w=null;
		}else if ( model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
			System.out.println("Solution found, distance value = "+ model .get(GRB.DoubleAttr.ObjVal)+" status= "+ model.get(GRB.IntAttr.Status));
			GRBVar[] fvars = model.getVars();
			double[] x = model.get(GRB.DoubleAttr.X, fvars);
			String[] vnames = model.get(GRB.StringAttr.VarName, fvars);
			//by only the distance value "res" we can judge ac3 and conclude that this is a cause or not. so we can have impls that return from here.
			// interpret the solution
			for (int j = 0; j < fvars.length; j++) {
				String varName = vnames[j];
				if (cause.stream().anyMatch(element -> element.name().equals(varName))) { // a solution value for a cause
					System.out.println(varName+" is part of the cause set");
					// get the variable literal in the original evaluation 
					Literal partialCause = evaluation.stream().filter(l -> l.name().equals(varName)).findAny().get();
					if (partialCause.phase() != (x[j] == 1.0)) {// then this part of the cause is  flipped TODO maybe exclude the ST SH here
						System.out.println("Partial cause found "+ varName +" actual value is "+partialCause.phase() +" solved value "+ x[j] );
						minimalCause.add(partialCause);
					} 	else{
						//TODO should we add those as W as an over-approximation 
						w.add(partialCause.variable());
//						System.out.println("Model was solved without flipping "+ varName +" actual value is "+partialCause.phase() +" solved value "+ x[j]+". violation of ac3." );
					}
					continue;
				}
				else if (phi.containsVariable(vnames[j])){// is from the effect variables
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
					if ( potentialWmember.phase() == (x[j] == 1.0)) {// value stayed the same
//						System.out.println("W memeber found "+ varName +" actual value is "+potentialWmember.phase() +" solved value "+ x[j] );
						w.add(potentialWmember.variable());
					} 	
					continue;
				}
			}
		}else{
			System.out.println("Unhandled ILP status "+ model.get(GRB.IntAttr.Status) +" check ILP log" );
			minimalCause = null;
			w=null;
		}

		// Dispose of model and environment
		model.dispose();
		model.getEnv().dispose();

		return new Pair<>(minimalCause, w); 
	}



	private void printProblem(Formula satFormula, Set<Literal> cause, Set<Literal> evaluation) {
		System.out.println("SATFormula*****"+satFormula);	
		cause.forEach(c -> System.out.println(c.name()+""+c.phase()));
		System.out.println("evaluation");
		evaluation.forEach(c -> System.out.println(c.name()+""+c.phase()));
	}

	/**
	 * a function that turns a custom causal model to Gurobi causal model the
	 * location should be changed to inside the causal model for example 
	 * 
	 * @param cm
	 * @param model
	 */
	public GRBModel createGRBModel(CausalModel cm, int causeSize) throws GRBException {
		// basic GRB model, parameters of the model should be set here
		GRBEnv env = new GRBEnv("./ILP-LOG/"+cm.getName()+".log");
		GRBModel model = new GRBModel(env);

		// start with exo vars
		cm.getExogenousVariables().forEach(item -> {
			try {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, item.name());

			} catch (GRBException e) {
				e.printStackTrace();
			}
		});

		cm.getEquationsSorted().forEach(e -> {
			try {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, e.getVariable().name());
			} catch (GRBException e1) {
				e1.printStackTrace();
			}
		});


		model.addVar(1.0, causeSize, 0.0, GRB.INTEGER, DISTANCE_VAR_NAME);

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
	public void addLPConstraints(Formula satFormula, GRBModel model, Set<Literal> evaluation, Set<Literal> cause) throws GRBException {

		// Set objective: min res
		GRBLinExpr expr = new GRBLinExpr();
		expr.addTerm(1.0, model.getVarByName(DISTANCE_VAR_NAME));
		model.setObjective(expr, GRB.MINIMIZE);


		// add constraints to the ILP model based on the CNF clauses
		// this formula doesn't contain any constraint about the cause 
		Iterator<Formula> iter = satFormula.cnf().iterator();
		while (iter.hasNext()) {
			// one clause in the cnf
			Formula clause = iter.next();
			if (clause.isAtomicFormula()) { // Assignment mainly for the effect and the context
				// R118: BS = 0
				Literal literal = (Literal) clause;
				expr = new GRBLinExpr();
				expr.addTerm(1.0, model.getVarByName(literal.name()));
				model.addConstr(expr, GRB.EQUAL, literal.phase() ? 1.0 : 0.0, "R_" + literal.name());
			} else {
				Iterator<Formula> literals = clause.iterator();
				expr = new GRBLinExpr();
				while (literals.hasNext()) {
					Literal literal = (Literal) literals.next();
					if (literal.phase()) {
						expr.addTerm(1.0, model.getVarByName(literal.name()));
					} else {
						expr.addTerm(-1.0, model.getVarByName(literal.name()));
						expr.addConstant(1.0);
					}
				}
				model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "R_" + clause.toString());
			}

		}

		// Add the constraint of the distance
		// res = segma Di Di = {1-x xact = 1 x xact=0}
		// Rres: res = 1- BT + 1- XT {BTact,XTact=1}
		expr = new GRBLinExpr();
		// and their actual values
		Iterator<Literal> causesItr = evaluation.stream().filter(l->cause.contains(l)).iterator();
		while (causesItr.hasNext()) {
			// not sure weather this has the value or should we reuse the evaluation
			Literal literal = causesItr.next();
			if (literal.phase()) {
				expr.addConstant(1.0);
				expr.addTerm(-1.0, model.getVarByName(literal.name()));
			} else {
				expr.addTerm(1.0, model.getVarByName(literal.name()));
			}
		}
		model.addConstr(model.getVarByName(DISTANCE_VAR_NAME), GRB.EQUAL, expr, "R_result");

	}
}
