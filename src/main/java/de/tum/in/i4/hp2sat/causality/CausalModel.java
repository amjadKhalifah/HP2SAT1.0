package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.exceptions.InvalidCauseException;
import de.tum.in.i4.hp2sat.exceptions.InvalidContextException;
import de.tum.in.i4.hp2sat.exceptions.InvalidPhiException;
import de.tum.in.i4.hp2sat.util.Util;
import org.graphstream.algorithm.TopologicalSortDFS;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.*;

public class CausalModel {
    private String name;
    private Set<Equation> equations;
    private Set<Variable> exogenousVariables;

    private Set<Variable> variables;
    private Map<Variable, Equation> variableEquationMap;
    private Graph graph;
    private Graph graphReversed;
    private List<Equation> equationsSorted; // according to topological sort

    /**
     * Creates a new causal model
     *
     * @param name               name of the causal model
     * @param equations          equations for the endogenous variables of the causal model
     * @param exogenousVariables the exogenous variables of the causal model
     * @throws InvalidCausalModelException throws an exception if model is not valid: (1) each variable needs to be
     *                                     either defined by an equation or be exogenous; (2) no duplicate definition of
     *                                     variables; (3) no circular dependencies
     */
    public CausalModel(String name, Set<Equation> equations, Set<Variable> exogenousVariables)
            throws InvalidCausalModelException {
        this.name = name;
        this.equations = equations;
        this.exogenousVariables = exogenousVariables;

        this.variables = this.equations.stream().map(Equation::getVariable).collect(Collectors.toSet());
        this.equations.forEach(e -> this.variables.addAll(e.getFormula().variables()));
        this.variables.addAll(exogenousVariables);

        if (isValid()) { // possibly throws exception
            this.variableEquationMap = this.equations.stream()
                    .collect(Collectors.toMap(Equation::getVariable, Function.identity()));
            this.graph = this.toGraph();
            this.graphReversed = Util.reverseGraph(this.graph);
            equationsSorted = this.sortEquations();
        }
    }

    /**
     * Creates a copy of the passed causal model in which only the equation that define the passed variables are
     * copied.
     *
     * @param causalModel the causal model that is copied
     * @param variables   the variables whose equations are copied
     */
    CausalModel(CausalModel causalModel, Set<Variable> variables) throws InvalidCausalModelException {
        this(causalModel.name, causalModel.equations.stream()
                .map(e -> variables.contains(e.getVariable()) ? new Equation(e) : e)
                .collect(Collectors.toSet()), new HashSet<>(causalModel.exogenousVariables));
    }

    /**
     * Determines whether the passed set of Literals is a cause for the given phi. For both phi and cause, a
     * positive/negative literal means that the variable is meant to be true/false. The context defines the exogenous
     * variables. Each variable is assigned a Constant (CTrue/CTFalse).
     *
     * @param context the context of the causal scenario; defines the values of the exogenous variables
     * @param phi     the literals (i.e. events) we want to check for whether the given cause is indeed a cause
     * @param cause   the set of literals (i.e. primitive events) we want to check for being a cause for phi
     * @return the result of the SAT Solver, i.e. true, false or undefined
     * @throws InvalidContextException thrown if context is invalid: (1) each exogenous variable needs to be defined;
     *                                 (2) no other variable than the exogenous variable are in the Map
     * @throws InvalidCauseException   thrown if the cause is invalid: each literal of the cause needs to be defined in
     *                                 the equations
     * @throws InvalidPhiException     thrown if phi is invalid: each literal of phi needs to be defined in the equations
     */
    public CausalitySolverResult isCause(Set<Literal> context, Formula phi, Set<Literal> cause,
                                         SolvingStrategy solvingStrategy)
            throws InvalidContextException, InvalidCauseException, InvalidPhiException, InvalidCausalModelException {
        validateCausalityCheck(context, phi, cause);
        CausalitySolver causalitySolver = null;
        if (solvingStrategy == EVAL || solvingStrategy == EVAL_OPTIMIZED_W) {
            causalitySolver = new EvalCausalitySolver();
        } else if (Arrays.asList(SAT, SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL, SAT_OPTIMIZED_W,
                SAT_OPTIMIZED_W_MINIMAL).contains(solvingStrategy)) {
            causalitySolver = new SATCausalitySolver();
        }

        return causalitySolver.solve(this, context, phi, cause, solvingStrategy);
    }

    /**
     * see {@link #isCause(Set, Formula, Set, SolvingStrategy, SATSolverType)} for a full documentation. The only
     * difference is that in the current method the to be used SAT solver can be specified. This only works if the
     * solving strategy refers to {@link SATCausalitySolver}. Otherwise,
     * {@link #isCause(Set, Formula, Set, SolvingStrategy)} is called, i.e. the SAT solver type is ignored.
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
    public CausalitySolverResult isCause(Set<Literal> context, Formula phi, Set<Literal> cause,
                                         SolvingStrategy solvingStrategy, SATSolverType satSolverType)
            throws InvalidContextException, InvalidCauseException, InvalidPhiException, InvalidCausalModelException {
        if (solvingStrategy == EVAL || solvingStrategy == EVAL_OPTIMIZED_W) {
            // ignore SAT solver type if solving strategy is not SAT related
            return isCause(context, phi, cause, solvingStrategy);
        } else {
            validateCausalityCheck(context, phi, cause);
            SATCausalitySolver satCausalitySolver = new SATCausalitySolver();
            return satCausalitySolver.solve(this, context, phi, cause, solvingStrategy, satSolverType);
        }
    }


    /**
     * Checks whether the current causal model is valid.
     *
     * @throws InvalidCausalModelException thrown if invalid
     */
    private boolean isValid() throws InvalidCausalModelException {
        boolean existsDefinitionForEachVariable = equations.size() + exogenousVariables.size() == this.variables.size();
        boolean existsNoDuplicateEquationForEachVariable =
                equations.size() == equations.stream().map(Equation::getVariable).collect(Collectors.toSet()).size();
        boolean existsCircularDependency = equations.parallelStream()
                .anyMatch(e -> isVariableInEquation(e.getVariable(), e));

        if (!(existsDefinitionForEachVariable && existsNoDuplicateEquationForEachVariable && !existsCircularDependency))
            throw new InvalidCausalModelException();

        return true;
    }

    /**
     * Checks whether a given variable is within a given equation or within the equations of the variables used
     * within the given equation (recursive!)
     *
     * @param variable the variable for which we want to know whether it is in the given equation
     * @param equation the equation whithin which we search for the variable
     * @return true, if variable was found; otherwise false
     */
    boolean isVariableInEquation(Variable variable, Equation equation) {
        Set<Variable> variables = equation.getFormula().variables();
        // check if formula of equation contains variable
        if (variables.contains(variable)) {
            return true;
        }

        // check for all other non-exogenous variables whether their corresponding equation contains the searched var
        for (Variable v : variables.stream()
                .filter(v -> !this.exogenousVariables.contains(v) && !v.equals(variable))
                .collect(Collectors.toSet())) {
            Equation eq = equations.stream().filter(e -> e.getVariable().equals(v)).findFirst().orElse(null);
            /*
            if eq is null, this would mean that a variable has no definition; this will be captured by another check
            in the isValid() method. Since the current method is private, we can ignore the case here.
             */
            if (eq != null) {
                if (isVariableInEquation(variable, eq))
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
        this.getEquations().forEach(e -> graph.addNode(e.getVariable().name()));
        this.getExogenousVariables().forEach(e -> graph.addNode(e.name()));
        // create edges
        for (Equation equation : this.equations) {
            String equationVariableName = equation.getVariable().name();
            Formula formula = equation.getFormula();
            formula.variables().forEach(v -> graph.addEdge(equationVariableName + "-" + v.name(), v.name(),
                    equationVariableName, true));
        }
        return graph;
    }

    /**
     * Checks if the given context is valid
     *
     * @param context the to be checked context
     * @return true if valid, else false
     */
    private boolean isContextValid(Set<Literal> context) {
        // each and only each exogenous variable must be defined by context
        return context.size() == exogenousVariables.size() &&
                exogenousVariables.containsAll(context.stream().map(Literal::variable).collect(Collectors.toSet()));
    }

    /**
     * Checks if the given literals (i.e. cause, phi or W) are in the Variable part of the equations of this causal
     * model.
     *
     * @param literals the to be checked literals
     * @return true if all the literals are in the Variable part of the equations of this causal model, else false
     */
    private boolean isLiteralsInEquations(Set<? extends Literal> literals) {
        return equations.stream().map(Equation::getVariable).collect(Collectors.toSet())
                .containsAll(literals.stream().map(Literal::variable).collect(Collectors.toSet()));
    }

    private void validateCausalityCheck(Set<Literal> context, Formula phi, Set<Literal> cause)
            throws InvalidCauseException, InvalidPhiException, InvalidContextException {
        if (!isContextValid(context))
            throw new InvalidContextException();
        if (!isLiteralsInEquations(phi.literals()))
            throw new InvalidPhiException();
        if (!isLiteralsInEquations(cause) || cause.size() < 1)
            throw new InvalidCauseException();
    }

    private List<Equation> sortEquations() {
        FormulaFactory f = new FormulaFactory();
        // get the causal model as graph
        Graph graph = this.getGraph();
        /*
         * Following to HP, we can sort variables in an acyclic causal model according to their dependence on other
         * variables. The following applies: "If X < Y, then the value of X may affect the value of Y , but the value
         * of Y cannot affect the value of X"
         * The problem is that we only obtain a partial order if we define < as X is contained in Y (or recursively
         * in the variables in the equation of Y) if X < Y. Therefore, we use a topological sort.
         * */
        TopologicalSortDFS topologicalSortDFS = new TopologicalSortDFS();
        topologicalSortDFS.init(graph);
        topologicalSortDFS.compute();
        // get sorted nodes
        List<Node> sortedNodes = topologicalSortDFS.getSortedNodes();
        // get sorted list of equations
        List<Equation> equationsSorted = sortedNodes.stream()
                // filter nodes representing endogenous variables
                .filter(n -> !this.getExogenousVariables().contains(f.variable(n.getId())))
                // get corresponding equation
                .map(n -> this.getVariableEquationMap().get(f.variable(n.getId())))
                .collect(Collectors.toList());
        return equationsSorted;
    }

    public String getName() {
        return name;
    }

    public Set<Equation> getEquations() {
        return equations;
    }

    public Set<Variable> getExogenousVariables() {
        return exogenousVariables;
    }

    public Map<Variable, Equation> getVariableEquationMap() {
        return variableEquationMap;
    }

    public Graph getGraph() {
        return graph;
    }

    public Graph getGraphReversed() {
        return graphReversed;
    }

    public List<Equation> getEquationsSorted() {
        return equationsSorted;
    }
}
