package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.*;
import org.logicng.util.Pair;

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
    abstract CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
                                         Set<Literal> cause, SolvingStrategy solvingStrategy)
            throws InvalidCausalModelException;

    /**
     * Checks if AC1 fulfilled.
     *
     * @param evaluation the original evaluation of variables
     * @param phi        the phi
     * @param cause      the cause for which we check AC1
     * @return a tuple where the first item indicates whether phi occurred and the second item whether the cause
     * occurred
     */
    Pair<Boolean, Boolean> fulfillsAC1(Set<Literal> evaluation, Formula phi, Set<Literal> cause) {
        boolean phiEvaluation = phi.evaluate(new Assignment(evaluation));
        return new Pair<>(phiEvaluation, evaluation.containsAll(cause));
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
                                            SolvingStrategy solvingStrategy, FormulaFactory f)
            throws InvalidCausalModelException {
        // compute all possible combination of primitive events
        Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context, f);
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
    static Set<Literal> evaluateEquations(CausalModel causalModel, Set<Literal> context, FormulaFactory f,
                                          Variable... variables) {
        // initially, we can only assign the exogenous variables as defined by the context
        Assignment assignment = new Assignment(context);
        for (Equation equation : causalModel.getEquationsSorted()) {
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
     * Returns only those variables of a causal model that need to be in set W.
     *
     * @param causalModel the causal model
     * @param cause       the cause for which we check the conditions of the HP definition
     * @param f           a formula factory
     * @return a set of variables that need to be in W
     */
    static Set<Variable> getMinimalWVariables(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                              FormulaFactory f) {
        Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
        Graph graph = causalModel.getGraph();
        // get set of reachable variables by X
        Set<Variable> reachableVariablesByCause = getReachableVariables(graph, cause, f);
        reachableVariablesByCause = reachableVariablesByCause.stream()
                .filter(v -> !causeVariables.contains(v)).collect(Collectors.toSet());
        Graph graphReversed = causalModel.getGraphReversed();
        // get set of variables that affect phi -> use reversed graph
        Set<Variable> reachableVariablesByPhi = getReachableVariables(graphReversed, phi.literals(), f);

        // the idea is to only include those variables into W that can be affected by the cause and that affect phi
        reachableVariablesByCause.retainAll(reachableVariablesByPhi);
        return reachableVariablesByCause;
    }

    /**
     * Computes the set of variables reachable from the passed set of literals in a given graph.
     *
     * @param graph    the graph
     * @param literals the literals
     * @param f        a formula factory
     * @return a set of variables reachable from the passed literals
     */
    static Set<Variable> getReachableVariables(Graph graph, Set<Literal> literals, FormulaFactory f) {
        Set<Variable> reachableVariables = new HashSet<>();
        for (Literal literal : literals) {
            // get the corresponding node in the graph
            Node node = graph.getNode(literal.name());
            Iterator<Node> iterator = node.getDepthFirstIterator(true);
            // iterate through all reachable nodes
            while (iterator.hasNext()) {
                Node reachableNode = iterator.next();
                reachableVariables.add(f.variable(reachableNode.getId()));
            }
        }
        return reachableVariables;
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
