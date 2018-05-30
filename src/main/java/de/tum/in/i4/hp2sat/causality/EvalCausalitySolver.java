package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.util.Util;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class EvalCausalitySolver extends CausalitySolver {
    /**
     * Overrides {@link CausalitySolver#solve(CausalModel, Set, Formula, Set, SolvingStrategy)}.
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
        FormulaFactory f = new FormulaFactory();
        Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context, f);
        boolean ac1 = fulfillsAC1(evaluation, phi, cause);
        Set<Literal> w = fulfillsAC2(causalModel, phi, cause, context, evaluation, solvingStrategy, f);
        boolean ac2 = w != null;
        boolean ac3 = fulfillsAC3(causalModel, phi, cause, context, evaluation, solvingStrategy, f);
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
        return causalitySolverResult;
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
     * @param f               a formula factory
     * @return returns W if AC2 fulfilled, else null
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    private Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                       Set<Literal> evaluation, SolvingStrategy solvingStrategy, FormulaFactory f)
            throws InvalidCausalModelException {
        if (solvingStrategy == SolvingStrategy.EVAL || solvingStrategy == SolvingStrategy.EVAL_OPTIMIZED_W) {
            Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
            /*
             * remove exogenous variables from evaluation as they are not needed for computing the Ws. Furthermore,
             * all variables in the cause also must not be in W. */
            Set<Literal> wVariables = evaluation.stream()
                    .filter(l -> !causalModel.getExogenousVariables().contains(l.variable()) &&
                            !(causeVariables.contains(l.variable())))
                    .collect(Collectors.toSet());
            if (solvingStrategy == SolvingStrategy.EVAL_OPTIMIZED_W) {
                Set<Variable> wVariablesOptimized = CausalitySolver.getMinimalWVariables(causalModel, phi, cause, f);
                // remove variables that are not in the optimized W vars set
                wVariables = wVariables.stream()
                        .filter(l -> wVariablesOptimized.contains(l.variable())).collect(Collectors.toSet());
            }
            // get all possible Ws, i.e create power set of the evaluation
            List<Set<Literal>> allW = (new Util<Literal>()).generatePowerSet(wVariables);
            return fulfillsAC2(causalModel, phi, cause, context, allW, f);
        } else {
            return null;
        }
    }

    /**
     * Internal method for checking if AC2 is fulfilled.
     *
     * @param causalModel the underlying causal model
     * @param phi         the phi
     * @param cause       the cause for which we check AC2
     * @param context     the context
     * @param allW        set of all relevant W
     * @param f           a formula factory
     * @return W if AC2 fulfilled, else null
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    private Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                     List<Set<Literal>> allW, FormulaFactory f)
            throws InvalidCausalModelException {
        Formula phiFormula = f.not(phi); // negate phi

        // create copy of original causal model
        CausalModel causalModelModified = createModifiedCausalModelForCause(causalModel, cause, f);

        // evaluate causal model with setting x' for cause
        Set<Literal> evaluationModified = CausalitySolver.evaluateEquations(causalModelModified, context, f,
                phiFormula.variables().toArray(new Variable[0]));
        // check if not(phi) evaluates to true for empty W -> if yes, no further investigation necessary
        if (phiFormula.evaluate(new Assignment(evaluationModified))) {
            return new HashSet<>();
        }

        for (Set<Literal> w : allW) {
            // create copy of modified causal model
            CausalModel causalModelModifiedW = createModifiedCausalModelForW(causalModelModified, w, f);
            // evaluate all variables in the negated phi
            evaluationModified = CausalitySolver.evaluateEquations(causalModelModifiedW, context, f,
                    phiFormula.variables().toArray(new Variable[0]));
            /*
             * if the negated phi evaluates to true given the values of the variables in the modified causal model,
             * AC2 is fulfilled an we return the W for which it is fulfilled. */
            if (phiFormula.evaluate(new Assignment(evaluationModified)))
                return w;
        }

        return null;
    }

    /**
     * Checks if AC3 is fulfilled
     *
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param context         the context
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @param f               a formula factory
     * @return true if A3 fulfilled, else false
     */
    private boolean fulfillsAC3(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                  Set<Literal> evaluation, SolvingStrategy solvingStrategy, FormulaFactory f)
            throws InvalidCausalModelException {
        // get all subsets of cause
        Set<Set<Literal>> allSubsetsOfCause = new UnifiedSet<>(cause).powerSet().stream()
                .map(s -> s.toImmutable().castToSet())
                .filter(s -> s.size() > 0 && s.size() < cause.size()) // remove empty set and full cause
                .collect(Collectors.toSet());
        // no sub-cause must fulfill AC1 and AC2
        for (Set<Literal> c : allSubsetsOfCause) {
            if (fulfillsAC1(evaluation, phi, c) &&
                    fulfillsAC2(causalModel, phi, c, context, evaluation, solvingStrategy, f) != null) {
                return false;
            }
        }
        return true;
    }
}
