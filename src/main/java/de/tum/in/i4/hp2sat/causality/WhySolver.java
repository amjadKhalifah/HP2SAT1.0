package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.solvers.CleaneLing;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.transformations.cnf.CNFFactorization;
import org.logicng.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.ILPSolverType.GUROBI;
import static de.tum.in.i4.hp2sat.causality.SATSolverType.GLUCOSE;
import static de.tum.in.i4.hp2sat.causality.SATSolverType.MINICARD;
import static de.tum.in.i4.hp2sat.causality.SATSolverType.MINISAT;
import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.*;

class WhySolver extends CausalitySolver {
	static final String C1_VAR_PREFIX = "C1_";
	static final String C2_VAR_PREFIX = "C2_";
	static final String C3_VAR_PREFIX = "C3_";
	static final String C1_SUM_VAR = C1_VAR_PREFIX + "sum";
	static final String C1COM_SUM_VAR = C1_VAR_PREFIX + "com_sum";
	static final String C3_SUM_VAR = C3_VAR_PREFIX + "sum";
	
	
	

	@Override
	CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
			SolvingStrategy solvingStrategy) throws InvalidCausalModelException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@link CausalitySolver#solve(CausalModel, Set, Formula, Set, SolvingStrategy)}.
	 * Default SATSolver: MINISAT
	 *
	 * @param causalModel     the underlying causal model
	 * @param context         the context
	 * @param phi             the phi
	 * @param solvingStrategy the applied solving strategy
	 * @return for each AC, true if fulfilled, false else
	 * @throws InvalidCausalModelException thrown if internally generated causal
	 *                                     models are invalid
	 */
	
	List<CausalitySolverResult> solveWhy(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
			SolvingStrategy solvingStrategy) throws InvalidCausalModelException {
		return solve(causalModel, context, phi);
	}

	/**
	 * Checks AC1, AC2 and AC3 given a causal model, a cause, a context and phi and
	 * a solving strategy.
	 *
	 * @param causalModel the underlying causal model
	 * @param context     the context
	 * @param phi         the phi
	 * @return for each AC, true if fulfilled, false else
	 * @throws InvalidCausalModelException thrown if internally generated causal
	 *                                     models are invalid
	 */
	List<CausalitySolverResult> solve(CausalModel causalModel, Set<Literal> context, Formula phi)
			throws InvalidCausalModelException {
		FormulaFactory f = new FormulaFactory();
		Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context);

		List<Pair<Set<Literal>, Set<Literal>>> result = findCauses(causalModel, phi, context, evaluation, f);
		List<CausalitySolverResult> causes = new ArrayList<CausalitySolverResult>();
		if (result == null) {
			return causes;
		}
//		convert to results objects
		for (Pair<Set<Literal>, Set<Literal>> actualCause : result) {
			// TODO should we check ac1 after the results are obtained
			boolean ac2 = false, ac3 = false, ac1 = false;
			Set<Literal> w;
			Set<Literal> cause;
			// we want to find a possible cause with some criteria

			cause = actualCause.first();
			w = actualCause.second();

			if (cause != null) {
				if (cause.isEmpty()) {// means we did n't have to flip anything, couldn't happen because of minimum
										// distance constraint
					ac2 = ac3 = false;
					System.out.println("The effect is not at all affected by the cause");
				} else {
					ac1 = true;
					ac2 = true; // maybe this is a harsh conclusion
					ac3 = true;
				}

			}
			CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
			causes.add(causalitySolverResult);
		}
		return causes;
	}

	/**
	 * Checks if AC2 and AC3 are fulfilled. Combined approach that takes advantage
	 * of synergies between the separate approaches.
	 *
	 * @param causalModel     the underlying causal model
	 * @param phi             the phi
	 * @param cause           the cause for which we check AC2
	 * @param context         the context
	 * @param evaluation      the original evaluation of variables
	 * @param solvingStrategy the solving strategy
	 * @param satSolverType   the to be used SAT solver
	 * @param f               a formula factory
	 * @return a tuple of set W and a boolean value indicating whether AC3 is
	 *         fulfilled or not
	 * @throws InvalidCausalModelException thrown if internally generated causal
	 *                                     models are invalid
	 */
	private List<Pair<Set<Literal>, Set<Literal>>> findCauses(CausalModel causalModel, Formula phi,
			Set<Literal> context, Set<Literal> evaluation, FormulaFactory f) throws InvalidCausalModelException {
		List<Pair<Set<Literal>, Set<Literal>>> result;
		Formula phiNegated = f.not(phi);
		Formula formula = generateSATQuery(causalModel, phiNegated, context, evaluation, f);
//		System.out.println("G:=" + formula.toString());

		try {
			result = solveILP(causalModel, phi, formula, evaluation, f);
		} catch (ParserException | GRBException e) {
			result = null;
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Generates a formula whose satisfiability indicates whether AC2 is fulfilled
	 * or not.
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
	private Formula generateSATQuery(CausalModel causalModel, Formula notPhi, Set<Literal> context,
			Set<Literal> evaluation, FormulaFactory f) {
		Set<Variable> phiVariables = notPhi.variables();
		// create map of variables and corresponding evaluation
		Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
				.collect(Collectors.toMap(Literal::variable, Function.identity()));
		// create formula: !phi AND context
		Formula formula = f.and(notPhi, f.and(context));

		for (Equation equation : causalModel.getVariableEquationMap().values()) {

			// get value of variable in original iteration
			Literal originalValue = variableEvaluationMap.get(equation.getVariable());
			Formula equationFormula;
			if (phiVariables.contains(equation.getVariable())) {
				equationFormula = f.equivalence(equation.getVariable(), equation.getFormula());
			} else {
				Variable c1 = f.variable(C1_VAR_PREFIX + equation.getVariable().name());
				Variable c2 = f.variable(C2_VAR_PREFIX + equation.getVariable().name());
				equationFormula = f.and(
						f.or(f.and(f.equivalence(equation.getVariable(), equation.getFormula()), c1),
								f.and(f.not(f.equivalence(equation.getVariable(), equation.getFormula())), f.not(c1))),
						f.or(f.and(originalValue, c2), f.and(f.not(originalValue), f.not(c2))));
			}
			// add created formula to global formula by AND
			formula = f.and(formula, equationFormula);
		}

		return formula;
	}

	/**
	 * This function prepares the ILP program based on the cm and the sat formula
	 * Then solves the model using Groupi solver. Lastly, interpret the reults to
	 * return two sets one for the minimal cause and one for the non minimal w AC2
	 * and AC3 results are not directly returned, but they can be infered from
	 * minimal cause set (size >1 AC2 is satisfied, == cause.size ac3 is satisfied)
	 * 
	 * @param cm
	 * @param phi
	 * @param satFormula
	 * @param cause
	 * @param evaluation
	 * @param f
	 * @return: a list of pairs the first element is the minimal cause, the second
	 *          is the non minimal w; returns null if the model is infeasible and
	 *          there were no causes infered
	 * @throws ParserException
	 * @throws GRBException
	 * @throws InvalidCausalModelException
	 */
	public List<Pair<Set<Literal>, Set<Literal>>> solveILP(CausalModel cm, Formula phi, Formula satFormula,
			Set<Literal> evaluation, FormulaFactory f)
			throws ParserException, GRBException, InvalidCausalModelException {
		List<Pair<Set<Literal>, Set<Literal>>> causes = new ArrayList<Pair<Set<Literal>, Set<Literal>>>();
		// create and configure the ilp vars
		// copy the variables of the causal model to ILP model
		GRBModel model = createGRBModel(cm, phi.variables());

		// add the constraints based on the formula
		addLPConstraints(satFormula, model, f);
		// write the model to file for debugging (should be stopped in benchmarks)
		model.write("./ILP-models/ptest" + cm.getName() + "why.lp");
		model.set(GRB.IntParam.OutputFlag, 0);
		// solve the model
		model.optimize();
//		Feasibility of the program means that there is a cause of size at least 1 (exact size is c3sum), that makes phi not hold, 
//		the context set. To process the results:
//		1- c1sum is the number of normal variables (evaluate according to their equations), for the exact variables check the C1_Vi=1
//		2- c3sum is the size of the cause vector, the exact variables can be deducted by collecting C3_Vx=1 variables  
//		3- W size can be calculated using the above(complement-c3sum) , for the exact variables check C1_Vi and C2_Vi should be 01
		// OPTIMAL 2 Model was solved to optimality INFEASIBLE 3 Model was proven to be
		// infeasible.
		if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
			System.out.println("Model is infeasible.");
			causes = null;

		} else if (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
			causes = new ArrayList<Pair<Set<Literal>, Set<Literal>>>();
			// Print number of solutions stored
			int nSolutions = model.get(GRB.IntAttr.SolCount);
			System.out.println("Number of solutions found: " + nSolutions);

			// keep more then cause? yes until we have a better way to compare them
			for (int e = 0; e < nSolutions; e++) {
				Set<Literal> cause = new HashSet<>();
				Set<Literal> w = new HashSet<>();

				model.set(GRB.IntParam.SolutionNumber, e);
				// first objective value: c1_sum
				model.set(GRB.IntParam.ObjNumber, 0);
				double obj1 = model.get(GRB.DoubleAttr.ObjNVal);
				System.out.print("Normal Vars" + obj1);
				// scnd objective value: size of W
				model.set(GRB.IntParam.ObjNumber, 1);
				double obj2 = model.get(GRB.DoubleAttr.ObjNVal);
				System.out.print("	Size of W " + obj2 + "\n");

				// loop the original variables and fetch their control vars
				for (Literal variable : evaluation) {
					String name = variable.name();
					if (phi.containsVariable(name) || cm.getExogenousVariables().stream()
							.filter(l -> l.name().equals(name)).findAny().isPresent()) {// is from the effect variables
						continue;
					} else {
						GRBVar c3 = model.getVarByName(C3_VAR_PREFIX + name);
						if (c3.get(GRB.DoubleAttr.Xn) == 1.0)// this is a cause
						{
							cause.add(variable);
							continue;
						} else {
							GRBVar c1 = model.getVarByName(C1_VAR_PREFIX + name);
							GRBVar c2 = model.getVarByName(C2_VAR_PREFIX + name);
							if (c1.get(GRB.DoubleAttr.Xn) == 0 && (c2.get(GRB.DoubleAttr.X) == 1.0))// this is a W
							{
								w.add(variable);
							}

						}
					}

					// TODO this loop can be optimized by checking the sizes of current causes and w
					// and breaking once done

				}

				System.out.println("cause " + cause);
				System.out.println("W " + w);
				causes.add(new Pair(cause, w));
			}
		} else {
			System.out.println("Unhandled ILP status " + model.get(GRB.IntAttr.Status) + " check ILP log");
			causes = null;
		}
		// Dispose of model and environment
		model.dispose();
		model.getEnv().dispose();

		return causes;
	}

	private void printProblem(Formula satFormula, Set<Literal> cause, Set<Literal> evaluation) {
		System.out.println("SATFormula*****" + satFormula);
		cause.forEach(c -> System.out.println(c.name() + "" + c.phase()));
		System.out.println("evaluation");
		evaluation.forEach(c -> System.out.println(c.name() + "" + c.phase()));
	}

	/**
	 * a function that turns a custom causal model to Gurobi causal model the
	 * location should be changed to inside the causal model for example
	 * 
	 * @param cm
	 * @param phiVariables
	 * @param model
	 */
	public GRBModel createGRBModel(CausalModel cm, SortedSet<Variable> phiVariables) throws GRBException {
		// basic GRB model, parameters of the model should be set here
		GRBEnv env = new GRBEnv("./ILP-LOG/" + cm.getName() + "_why.log");
		GRBModel model = new GRBModel(env);

		GRBLinExpr c1SumExp = new GRBLinExpr();
		GRBLinExpr c3SumExp = new GRBLinExpr();
		// start with exo vars
		cm.getExogenousVariables().forEach(item -> {
			try {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, item.name());
			} catch (GRBException e) {
				e.printStackTrace();
			}
		});
		// for each endogenous variable not in phi add 3 vars
		cm.getEquationsSorted().forEach(e -> {
			try {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, e.getVariable().name());
				if (!phiVariables.contains(e.getVariable())) {
					GRBVar c1 = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, C1_VAR_PREFIX + e.getVariable().name());
					GRBVar c2 = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, C2_VAR_PREFIX + e.getVariable().name());
					GRBVar c3 = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, C3_VAR_PREFIX + e.getVariable().name());
					// doing an exception of adding the c1,c2,c3 constraints here before the CNF
					// constraints
					// because i don't want to loop again
					// c3 =~c1 /\ ~c2 e.g., - cbt1 - cbt2 - 2 cbt3 <= -1 - cbt1 - cbt2 - 2 cbt3 >=-2
					GRBLinExpr expr = new GRBLinExpr();
					expr.addTerm(-1.0, c1);
					expr.addTerm(-1.0, c2);
					expr.addTerm(-2.0, c3);
					model.addConstr(expr, GRB.LESS_EQUAL, -1.0, "c3_" + e.getVariable().name() + "_1");
					model.addConstr(expr, GRB.GREATER_EQUAL, -2.0, "c3_" + e.getVariable().name() + "_2");

					// prepare the c1 and c3 sums
					c1SumExp.addTerm(1.0, c1);
					c3SumExp.addTerm(1.0, c3);
				}
			} catch (GRBException e1) {
				e1.printStackTrace();
			}
		});

		// add the overall variables c1_sum, c1_complement, c3_ sum
		GRBVar c1sum = model.addVar(0.0, cm.getEquationsSorted().size() - phiVariables.size(), 0.0, GRB.INTEGER,
				C1_SUM_VAR);
		GRBVar c1ComSum = model.addVar(0.0, cm.getEquationsSorted().size() - phiVariables.size(), 0.0, GRB.INTEGER,
				C1COM_SUM_VAR);
		GRBVar c3sum = model.addVar(1.0, cm.getEquationsSorted().size() - phiVariables.size(), 0.0, GRB.INTEGER,
				C3_SUM_VAR);


		// finalize the equations for the sum constraints
		c1SumExp.addTerm(-1, c1sum);
		model.addConstr(c1SumExp, GRB.EQUAL, 0, "c1Sum");

		c3SumExp.addTerm(-1, c3sum);
		model.addConstr(c3SumExp, GRB.EQUAL, 0, "c3Sum");

		GRBLinExpr expr2 = new GRBLinExpr();
		expr2.addTerm(1, c1sum);
		expr2.addTerm(1, c1ComSum);
		model.addConstr(expr2, GRB.EQUAL, cm.getEquationsSorted().size() - phiVariables.size(), "c1ComSum");

		// Apparently should call this method so that the model is in sync
		model.update();
		return model;

	}

	/**
	 * a function that converts CNF clauses to GRB constraints
	 * 
	 * @param fm
	 * @param model
	 * @param f
	 * @throws GRBException
	 */
	public void addLPConstraints(Formula satFormula, GRBModel model, FormulaFactory f) throws GRBException {

		GRBLinExpr expr = new GRBLinExpr();
		// OBJ0: Priority=2 Weight=1 AbsTol=0 RelTol=0
		// c1sum
		// OBJ1: Priority=1 Weight=1 AbsTol=0 RelTol=0
		// c1sumcomplement - c3sum

		// Set global sense for ALL objectives

		model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

		GRBLinExpr objn0 = new GRBLinExpr();
		String vname = "OBJ0";
		objn0.addTerm(1.0, model.getVarByName(C1_SUM_VAR));

		model.setObjectiveN(objn0, 0, 2, 1, 0, 0, vname);

		GRBLinExpr objn1 = new GRBLinExpr();
		String vname2 = "OBJ1";
		objn1.addTerm(1.0, model.getVarByName(C1COM_SUM_VAR));
		objn1.addTerm(-1.0, model.getVarByName(C3_SUM_VAR));

		model.setObjectiveN(objn1, 1, 1, 1, 0, 0, vname2);

		// add constraints to the ILP model based on the CNF clauses
		// this formula doesn't contain any constraint about the cause
//		PropositionalParser p = new PropositionalParser(f);
//		Formula cnf = satFormula.transform(new CNFFactorization());
		Iterator<Formula> iter = satFormula.cnf().iterator();
//TODO check the performance of this against satFormula.cnf();
		// https://github.com/logic-ng/LogicNG/issues/15

		while (iter.hasNext()) {
			// one clause in the cnf
			Formula clause = iter.next();
			if (clause.isAtomicFormula()) { // Assignment mainly for the effect and the context
				// R118: BS = 0
				Literal literal = (Literal) clause;
				expr = new GRBLinExpr();
				expr.addTerm(1.0, model.getVarByName(literal.name()));
				model.addConstr(expr, GRB.EQUAL, literal.phase() ? 1.0 : 0.0, "");
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
				try {
				model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "");

			} catch (Exception e) {
				e.printStackTrace();
			}
			}

		}

	}


}
