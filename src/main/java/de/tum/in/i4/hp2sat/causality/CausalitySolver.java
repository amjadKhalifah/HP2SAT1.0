package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.graphstream.algorithm.TopologicalSortDFS;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.*;

import java.util.*;
import java.util.stream.Collectors;

abstract class CausalitySolver {
    /**
     * Checks AC1, AC2 and AC3 given a causal model, a cause, a context and phi and a solving strategy.
     *
     * @param causalModel     the underlying causel model
     * @param context         the context
     * @param phi             the phi
     * @param cause           the cause
     * @param solvingStrategy the applied solving strategy
     * @return for each AC, true if fulfilled, false else
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
                                Set<Literal> cause, SolvingStrategy solvingStrategy)
            throws InvalidCausalModelException {
        Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context);
        boolean ac1 = fulfillsAC1(evaluation, phi, cause);
        Set<Literal> w = fulfillsAC2(causalModel, phi, cause, context, evaluation, solvingStrategy);
        boolean ac2 = w != null;
        boolean ac3 = fulfillsAC3(causalModel, phi, cause, context, evaluation, solvingStrategy);
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
        return causalitySolverResult;
    }

    /**
     * Checks if AC1 fulfilled.
     *
     * @param evaluation the original evaluation of variables
     * @param phi        the phi
     * @param cause      the cause for which we check AC1
     * @return true if AC1 fulfilled, else false
     */
    boolean fulfillsAC1(Set<Literal> evaluation, Formula phi, Set<Literal> cause) {
        Set<Literal> litersOfPhi = phi.literals();
        return evaluation.containsAll(litersOfPhi) && evaluation.containsAll(cause);
    }

    /**
     * Checks if AC2 is fulfilled.
     *
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param context         the context
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @return returns W if AC2 fulfilled, else null
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    abstract Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                      Set<Literal> evaluation, SolvingStrategy solvingStrategy)
            throws InvalidCausalModelException;

    /**
     * Checks if AC3 is fulfilled.
     *
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @return true if A3 fulfilled, else false
     */
    boolean fulfillsAC3(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                        Set<Literal> evaluation, SolvingStrategy solvingStrategy) {
        // get all subsets of cause
        Set<Set<Literal>> allSubsetsOfCause = new UnifiedSet<>(cause).powerSet().stream()
                .map(s -> s.toImmutable().castToSet())
                .filter(s -> s.size() > 0 && s.size() < cause.size()) // remove empty set and full cause
                .collect(Collectors.toSet());
        // no sub-cause must fulfill AC1 and AC2
        boolean ac3 = allSubsetsOfCause.stream().noneMatch(c -> {
            try {
                return fulfillsAC1(evaluation, phi, cause) &&
                        fulfillsAC2(causalModel, phi, c, context, evaluation, solvingStrategy) != null;
            } catch (InvalidCausalModelException e) {
                e.printStackTrace();
                return false;
            }
        });
        return ac3;
    }

    /**
     * Returns all causes for a given causal model, a context and phi.
     *
     * @param causalModel the underlying causel model
     * @param context     the context
     * @param phi         the phi
     * @return set of all causes, i.e. AC1-AC3 fulfilled, as set of results
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    Set<CausalitySolverResult> getAllCauses(CausalModel causalModel, Set<Literal> context, Formula phi,
                                            SolvingStrategy solvingStrategy) throws InvalidCausalModelException {
        // compute all possible combination of primitive events
        Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context);
        Set<Literal> evaluationWithoutExogenousVariables = evaluation.stream()
                .filter(l -> !causalModel.getExogenousVariables().contains(l.variable())).collect(Collectors.toSet());
        List<Set<Literal>> allPotentialCauses = new UnifiedSet<>(evaluationWithoutExogenousVariables).powerSet()
                .stream().map(s -> s.toImmutable().castToSet())
                .sorted(Comparator.comparingInt(Set::size))
                .collect(Collectors.toList());
        // remove empty set (index 0 as list is ordered!)
        allPotentialCauses.remove(0);
        Set<CausalitySolverResult> allCauses = new HashSet<>();
        for (Set<Literal> cause : allPotentialCauses) {
            /*
             * if a subset of the currently analyzed potential cause is already a cause, we don't need to check the
             * current one since it will not fulfill AC3 (minimality!) */
            if (allCauses.stream().noneMatch(c -> cause.containsAll(c.getCause()))) {
                CausalitySolverResult causalitySolverResult = solve(causalModel, context, phi, cause, solvingStrategy);
                if (causalitySolverResult.isAc1() && causalitySolverResult.isAc2() && causalitySolverResult.isAc3()) {
                    // if all ACs fulfilled, it is a cause
                    allCauses.add(causalitySolverResult);
                }
            }

        }
        return allCauses;
    }

    /**
     * Evaluates the equations of the given causal model under a given context.
     *
     * @param causalModel the causal model
     * @param context     the context, i.e. the evaluation of the exogenous variables; positive literal means true,
     *                    negative means false
     * @param variables   if some variables are given, then only their evaluation is returned
     * @return evaluation for all variables within the causal model (endo and exo); positive literal means true,
     * negative means false
     */
    static Set<Literal> evaluateEquations(CausalModel causalModel, Set<Literal> context, Variable... variables) {
        // create graph from causal model
        Graph graph = causalModel.toGraph();
        /*
         * Following to HP, we can sort variables in an acyclic causal model according to their dependence on other
         * variables. The following applies: "If X < Y, then the value of X may affect the value of Y , but the value
         * of Y cannot affect the value of X"
         * The problem is that this sorting is NOT transitive. Therefore, we convert the causal model into a graph
         * and to a topological sort.
         * */
        TopologicalSortDFS topologicalSortDFS = new TopologicalSortDFS();
        topologicalSortDFS.init(graph);
        topologicalSortDFS.compute();
        // get sorted nodes
        List<Node> sortedNodes = topologicalSortDFS.getSortedNodes();
        // get set of exogenous variable names
        Set<String> exogenousVariablesNames = causalModel.getExogenousVariables().stream().map(Literal::name)
                .collect(Collectors.toSet());
        // get sorted list of equations
        List<Equation> equationsSorted = sortedNodes.stream()
                .filter(n -> !exogenousVariablesNames.contains(n.getId()))
                .map(n -> causalModel.getEquations().stream()
                        .filter(e -> e.getVariable().name().equals(n.getId())).findFirst().get())
                .collect(Collectors.toList());

        // initially, we can only assign the exogenous variables as defined by the context
        Assignment assignment = new Assignment(context);
        for (Equation equation : equationsSorted) {
            /*
             * For each equation, we "evaluate" the corresponding formula based on the assignment. Since the equations
             * have been sorted according to their dependence on each other, we know that there will ALWAYS be a
             * solution that is true or false given that the provided causal model is valid. Once we obtained the
             * evaluation, we extend the assignment accordingly */
            Formula evaluation = equation.getFormula().restrict(assignment);
            // if the causal model is valid than one of the ifs MUST apply!
            if (evaluation instanceof CTrue) {
                assignment.addLiteral(equation.getVariable());
            } else {
                assignment.addLiteral(equation.getVariable().negate());
            }

            /*
             * If variables are specified, we stop the evaluation once all the specified variables are actually
             * evaluated.*/
            if (variables.length > 0 &&
                    assignment.literals().stream().map(Literal::variable).collect(Collectors.toSet())
                            .containsAll(Arrays.asList(variables))) {
                // return the evaluation for the specified variables only
                return assignment.literals().stream()
                        .filter(l -> Arrays.asList(variables).contains(l.variable())).collect(Collectors.toSet());
            }
        }
        /*
         * Finally, we return the literals of the assignment. A positive/negative literal indicates that the
         * corresponding variable evaluates to true/false  */
        return assignment.literals();
    }

    /**
     * Creates a modified causal model by replacing all equations referring to parts of the cause with the negation
     * of the phase of the respective part of the cause, i.e. with setting x'
     *
     * @param causalModel the causal model
     * @param cause       the cause
     * @param f           a formula factory
     * @return the modified causal model
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    CausalModel createModifiedCausalModelForCause(CausalModel causalModel, Set<Literal> cause, FormulaFactory f)
            throws InvalidCausalModelException {
        return createModifiedCausalModel(causalModel, cause.stream().map(Literal::negate)
                .collect(Collectors.toSet()), f);
    }

    /**
     * Creates a modified causal model by replacing all equations referring to set W with the phase of the literal in
     * W, i.e. its value in the original context.
     *
     * @param causalModel the causal model
     * @param w           the set W
     * @param f           a formula factory
     * @return the modified causal model
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    CausalModel createModifiedCausalModelForW(CausalModel causalModel, Set<Literal> w, FormulaFactory f)
            throws InvalidCausalModelException {
        return createModifiedCausalModel(causalModel, w, f);
    }

    /**
     * Helper method for {@link CausalitySolver#createModifiedCausalModelForCause(CausalModel, Set, FormulaFactory)}
     * and {@link CausalitySolver#createModifiedCausalModelForW(CausalModel, Set, FormulaFactory)}.
     *
     * @param causalModel the causal model
     * @param literals    the set of literals used to replace equations
     * @param f           a formula factory
     * @return the modified causal model
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    private CausalModel createModifiedCausalModel(CausalModel causalModel, Set<Literal> literals, FormulaFactory f)
            throws InvalidCausalModelException {
        CausalModel causalModelModified = new CausalModel(causalModel,
                literals.stream().map(Literal::variable).collect(Collectors.toSet()));
        Map<Variable, Equation> variableEquationMap = causalModelModified.getVariableEquationMap();
        // replace each equation with the phase of the literal
        for (Literal l : literals) {
            Equation equation = variableEquationMap.get(l.variable());
            equation.setFormula(l.phase() ? f.verum() : f.falsum());
        }
        return causalModelModified;
    }
}
