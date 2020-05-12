package org.mariuszgromada.math.mxparser;

import de.tum.in.i4.hp2sat.causality.CausalitySolver;
import de.tum.in.i4.hp2sat.causality.CausalitySolverResult;
import de.tum.in.i4.hp2sat.causality.Equation;
import de.tum.in.i4.hp2sat.causality.ILPCausalitySolver;
import de.tum.in.i4.hp2sat.causality.NumericCausalitySolver;
import de.tum.in.i4.hp2sat.causality.SATCausalitySolver;
import de.tum.in.i4.hp2sat.causality.SATSolverType;
import de.tum.in.i4.hp2sat.causality.SolvingStrategy;
import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.exceptions.InvalidCauseException;
import de.tum.in.i4.hp2sat.exceptions.InvalidContextException;
import de.tum.in.i4.hp2sat.exceptions.InvalidPhiException;
import de.tum.in.i4.hp2sat.util.Util;
import gurobi.GRBException;

import org.graphstream.algorithm.TopologicalSortDFS;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.logicng.formulas.Variable;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NumericCausalModel {
	private String name;
	private Set<Argument> exogenousVariables;

	private Map<String, Argument> variableEquationMap;
	private Graph graph;
	private Graph graphReversed;
	private List<Argument> equationsSorted; // according to topological sort

	/**
	 * Creates a new causal model
	 *
	 * @param name               name of the causal model
	 * @param equations          equations for the endogenous variables of the
	 *                           causal model
	 * @param exogenousVariables the exogenous variables of the causal model
	 * @param formulaFactory     a formula factory
	 * @throws InvalidCausalModelException throws an exception if model is not
	 *                                     valid: (1) each variable needs to be
	 *                                     either defined by an equation or be
	 *                                     exogenous; (2) no duplicate definition of
	 *                                     variables; (3) no circular dependencies;
	 *                                     (4) an exogenous variable must not be
	 *                                     called like
	 *                                     {@link SATCausalitySolver#DUMMY_VAR_NAME}
	 */
	public NumericCausalModel(String name, Set<Argument> equations, Set<Argument> exogenousVariables)
			throws InvalidCausalModelException {
		this(name, equations, exogenousVariables, true);

	}

	/**
	 * Same as
	 * {@link NumericCausalModel#CausalModel(String, Set, Set, FormulaFactory)}, but
	 * allows to specify if validity is checked. For internal use only.
	 *
	 * @param name
	 * @param equations
	 * @param exogenousVariables
	 * @param checkValidity
	 * @throws InvalidCausalModelException
	 */
	private NumericCausalModel(String name, Set<Argument> equations, Set<Argument> exogenousVariables,
			boolean checkValidity) throws InvalidCausalModelException {
		this.name = name;
		this.exogenousVariables = exogenousVariables;

		if (checkValidity) {
			// throws an exception if invalid
			isValid(equations, exogenousVariables);
		}
		this.variableEquationMap = equations.stream()
				.collect(Collectors.toMap(Argument::getArgumentName, Function.identity()));
		this.graph = this.toGraph();
		equationsSorted = this.sortEquations();
	}

//	/**
//	 * Creates a copy of the passed causal model in which only the equation that
//	 * define the passed variables are copied. IMPORTANT: We skip the validity check
//	 * when calling this constructor!
//	 *
//	 * @param causalModel the causal model that is copied
//	 * @param variables   the variables whose equations are copied
//	 */
//	NumericCausalModel(NumericCausalModel causalModel, Set<Argument> variables) throws InvalidCausalModelException {
//		/*
//		 * we assume that this constructor is called only, if we know that the original
//		 * causal model is valid. Thereforce, we skip the validity check.
//		 */
//		this(causalModel.name, causalModel.variableEquationMap.values().stream()
//				.map(e -> variables.contains(e.getVariable()) ? new Equation(e) : e).collect(Collectors.toSet()),
//				causalModel.exogenousVariables,  false);
//	}

	/**
	 * Determines whether the passed set of Literals is a cause for the given phi.
	 * For both phi and cause, a positive/negative literal means that the variable
	 * is meant to be true/false. The context defines the exogenous variables. Each
	 * variable is assigned a Constant (CTrue/CTFalse).
	 *
	 * @param context the context of the causal scenario; defines the values of the
	 *                exogenous variables
	 * @param phi     the literals (i.e. events) we want to check for whether the
	 *                given cause is indeed a cause
	 * @param cause   the set of literals (i.e. primitive events) we want to check
	 *                for being a cause for phi
	 * @return the result of the SAT Solver, i.e. true, false or undefined
	 * @throws InvalidContextException thrown if context is invalid: (1) each
	 *                                 exogenous variable needs to be defined; (2)
	 *                                 no other variable than the exogenous variable
	 *                                 are in the Map
	 * @throws InvalidCauseException   thrown if the cause is invalid: each literal
	 *                                 of the cause needs to be defined in the
	 *                                 equations
	 * @throws InvalidPhiException     thrown if phi is invalid: each literal of phi
	 *                                 needs to be defined in the equations
	 * @throws GRBException 
	 */
	public CausalitySolverResult isCause( Map<String, Double> context, Expression phi, Set<Argument> cause, double BIGM,  double upperBound, double lowerBound)
			throws InvalidContextException, InvalidCauseException, InvalidPhiException, InvalidCausalModelException, GRBException {
		
		NumericCausalitySolver cm = new NumericCausalitySolver(BIGM,upperBound,lowerBound);
		
		return cm.solve(this, context, phi, cause, SolvingStrategy.ILP_NUM);
		
	}

	/**
	 * see {@link #isCause(Set, Formula, Set, SolvingStrategy, SATSolverType)} for a
	 * full documentation. The only difference is that in the current method the to
	 * be used SAT solver can be specified. This only works if the solving strategy
	 * refers to {@link SATCausalitySolver}. Otherwise,
	 * {@link #isCause(Set, Formula, Set, SolvingStrategy)} is called, i.e. the SAT
	 * solver type is ignored.
	 *
	 * @param context
	 * @param phi
	 * @param cause
	 * @param solvingStrategy
	 * @param satSolverType
	 * @return
	 * @throws InvalidContextException
	 * @throws InvalidCauseException
	 * @throws InvalidPhiException
	 * @throws InvalidCausalModelException
	 */
	public CausalitySolverResult isCause(Set<Argument> context, Expression phi, Set<Argument> cause,
			SolvingStrategy solvingStrategy, SATSolverType satSolverType)
			throws InvalidContextException, InvalidCauseException, InvalidPhiException, InvalidCausalModelException {
			validateCausalityCheck(context, phi, cause);
			SATCausalitySolver satCausalitySolver = new SATCausalitySolver();
			return null;
			//return satCausalitySolver.solve(this, context, phi, cause, solvingStrategy, satSolverType);
	}

	/**
	 * Checks whether the given equations and exogenous variables are valid.
	 *
	 * @param equations          the equations
	 * @param exogenousVariables the exogenous variables
	 * @return true if valid
	 * @throws InvalidCausalModelException thrown if invalid
	 */
	private boolean isValid(Set<Argument> endogenousVars, Set<Argument> exogenousVariables)
			throws InvalidCausalModelException {
		// get all variables
		Set<Argument> variables = new HashSet<>();
		for (Iterator<Argument> iterator = endogenousVars.iterator(); iterator.hasNext();) {
			Argument argument = iterator.next();
			variables.add(argument);
			for (int j = 0; j < argument.getArgumentsNumber(); j++) {
				variables.add(argument.getArgument(j));
			}
		}
		
		
		variables.addAll(exogenousVariables);

		boolean existsDefinitionForEachVariable = endogenousVars.size() + exogenousVariables.size() == variables.size();
		boolean existsNoDuplicateEquationForEachVariable = endogenousVars.size() == endogenousVars.stream()
				.map(Argument::getArgumentName).collect(Collectors.toSet()).size();
		// TODO no cyclic check for now
		// boolean existsCircularDependency = endogenousVars.parallelStream()
		// .anyMatch(e -> isVariableInEquation(e.getVariable(), e, equations));

		if (!(existsDefinitionForEachVariable && existsNoDuplicateEquationForEachVariable))
			throw new InvalidCausalModelException();

		return true;
	}

	/**
	 * Checks whether a given variable is within a given equation or within the
	 * equations of the variables used within the given equation (recursive!)
	 *
	 * @param variable  the variable for which we want to know whether it is in the
	 *                  given equation
	 * @param equation  the equation within which we search for the variable
	 * @param equations the equations
	 * @return true, if variable was found; otherwise false
	 */
	private boolean isVariableInEquation(Argument variable, Equation equation, Set<Equation> equations) {
		Set<Variable> variables = equation.getFormula().variables();
		// check if formula of equation contains variable
		if (variables.contains(variable)) {
			return true;
		}

		// check for all other non-exogenous variables whether their corresponding
		// equation contains the searched var
		for (Variable v : variables.stream().filter(v -> !this.exogenousVariables.contains(v) && !v.equals(variable))
				.collect(Collectors.toSet())) {
			Equation eq = equations.stream().filter(e -> e.getVariable().equals(v)).findFirst().orElse(null);
			/*
			 * if eq is null, this would mean that a variable has no definition; this will
			 * be captured by another check in the isValid() method. Since the current
			 * method is private, we can ignore the case here.
			 */
			if (eq != null) {
				if (isVariableInEquation(variable, eq, equations))
					return true;
			}
		}

		return false;
	}

	/**
	 * Converts this causal model into a graph. Needed for evaluation.
	 *
	 * @return the causal model as graph
	 */
	private Graph toGraph() {
		Graph graph = new SingleGraph(this.name != null ? this.name : "");
		// create all nodes
		this.variableEquationMap.values().forEach(e -> graph.addNode(e.getArgumentName()));
		this.getExogenousVariables().forEach(e -> graph.addNode(e.getArgumentName()));
		// create edges
		
		
		this.variableEquationMap.values().forEach(e -> {
			String equationVariableName = e.getArgumentName();
			for (int i = 0; i < e.getArgumentsNumber(); i++) {
				
				String variableName = e.getArgument(i).getArgumentName();
				graph.addEdge(equationVariableName + "-" + variableName, variableName, equationVariableName, true);
			}
			});

		return graph;
	}

	/**
	 * Checks if the given context is valid
	 *
	 * @param context the to be checked context
	 * @return true if valid, else false
	 */
	private boolean isContextValid(Set<Argument> context) {
		// each and only each exogenous variable must be defined by context
		return context.size() == exogenousVariables.size();
	}

	/**
	 * Checks if the given literals (i.e. cause, phi or W) are in the Variable part
	 * of the equations of this causal model.
	 *
	 * @param literals the to be checked literals
	 * @return true if all the literals are in the Variable part of the equations of
	 *         this causal model, else false
	 */
	private boolean isArgumnetsInExpression(Set<? extends Argument> argumnets) {
		return this.variableEquationMap.values().containsAll(argumnets);
	}

	/**
	 * Make sure that the passed context, phi and cause are valid for the current
	 * causal model.
	 *
	 * @param context the context
	 * @param phi     the phi
	 * @param cause   the cause
	 * @throws InvalidCauseException
	 * @throws InvalidPhiException
	 * @throws InvalidContextException
	 */
	private void validateCausalityCheck(Set<Argument> context, Expression phi, Set<Argument> cause)
			throws InvalidCauseException, InvalidPhiException, InvalidContextException {
		if (!isContextValid(context))
			throw new InvalidContextException();
		Set<Argument> argumentsInPhi = new HashSet<>();
		
		for (int i = 0; i <phi.getArgumentsNumber(); i++) {
			argumentsInPhi.add(phi.getArgument(i));
		}
		if (!isArgumnetsInExpression(argumentsInPhi))
			throw new InvalidPhiException();
		if (!isArgumnetsInExpression(cause) || cause.size() < 1)
			throw new InvalidCauseException();
	}

	/**
	 * Sort the equations of the causal model according to the total order proposed
	 * by Halpern.
	 *
	 * @return an ordered list of equations
	 */
	private List<Argument> sortEquations() {
		// get the causal model as graph
		Graph graph = this.getGraph();
		/*
		 * Following to HP, we can sort variables in an acyclic causal model according
		 * to their dependence on other variables. The following applies: "If X < Y,
		 * then the value of X may affect the value of Y , but the value of Y cannot
		 * affect the value of X" The problem is that we only obtain a partial order if
		 * we define < as X is contained in Y (or recursively in the variables in the
		 * equation of Y) if X < Y. Therefore, we use a topological sort.
		 */
		TopologicalSortDFS topologicalSortDFS = new TopologicalSortDFS();
		topologicalSortDFS.init(graph);
		topologicalSortDFS.compute();
		// get sorted nodes
		List<Node> sortedNodes = topologicalSortDFS.getSortedNodes();
		// get sorted list of equations
		List<Argument> equationsSorted = sortedNodes.stream()
				// filter nodes representing endogenous variables
				.filter(n -> !this.getExogenousVariables().stream().map(Argument::getArgumentName).collect(Collectors.toSet())
						.contains(n.getId()))
				// get corresponding equation
				.map(n -> this.getVariableEquationMap().get(n.getId())).collect(Collectors.toList());
		return equationsSorted;
	}

	public String getName() {
		return name;
	}

	public Set<Argument> getExogenousVariables() {
		return exogenousVariables;
	}

	public Map<String, Argument> getVariableEquationMap() {
		return variableEquationMap;
	}

	public Graph getGraph() {
		return graph;
	}

	public Graph getGraphReversed() {
		if (this.graphReversed == null) {
			this.graphReversed = Util.reverseGraph(this.graph);
		}
		return graphReversed;
	}

	public List<Argument> getEquationsSorted() {
		return equationsSorted;
	}
	public Expression getExpression (Argument ar) {
		return ar.argumentExpression;
		
	}

	public Argument getVaribale(String string) {
		return variableEquationMap.get(string);
	}

	
	public void print () {
		System.out.println("Model "+ getName()+" size:"+ exogenousVariables.size()+" exogenous vars, and "+equationsSorted.size()+" endogenous"  );
		exogenousVariables.stream().forEach(e-> System.out.println("EX "+e.getArgumentName()+"= "+e.getArgumentValue() ));
//		variableEquationMap.entrySet().stream().forEach(e-> System.out.println(e.getKey()+"= "+e.getValue().getArgumentExpressionString()+" ="+e.getValue().getArgumentValue()));
		equationsSorted.stream().forEach(e-> System.out.println(e.getArgumentName()+"= "+e.getArgumentValue()));
	}
}
