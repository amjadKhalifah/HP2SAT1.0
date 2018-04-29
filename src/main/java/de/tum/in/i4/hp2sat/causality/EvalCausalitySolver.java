package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.*;

import java.util.*;
import java.util.stream.Collectors;

class EvalCausalitySolver extends CausalitySolver {
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
     */
    Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                             Set<Literal> evaluation, SolvingStrategy solvingStrategy)
            throws InvalidCausalModelException {
        if (solvingStrategy == SolvingStrategy.EVAL) {
            // remove exogenous variables from evaluation as they are not needed for computing the Ws
            Set<Literal> evaluationWithoutExogenousVariables = evaluation.stream()
                    .filter(l -> !causalModel.getExogenousVariables().contains(l.variable()))
                    .collect(Collectors.toSet());
            // get all possible Ws, i.e create power set of the evaluation
            List<Set<Literal>> allW = new UnifiedSet<>(evaluationWithoutExogenousVariables).powerSet().stream()
                    .map(s -> s.toImmutable().castToSet())
                    .sorted(Comparator.comparingInt(Set::size))
                    .collect(Collectors.toList());
            return fulfillsAC2(causalModel, phi, cause, context, allW);
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
     * @return W if AC2 fulfilled, else null
     */
    private Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                     List<Set<Literal>> allW)
            throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Formula phiFormula = f.not(phi); // negate phi

        // create copy of original causal model
        CausalModel causalModelModified = new CausalModel(causalModel);
        // replace equation of each part of the cause with its negation, i.e. setting x'
        for (Literal l : cause) {
            causalModelModified.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                    .forEach(e -> e.setFormula(l.negate().phase() ? f.verum() : f.falsum()));
        }

        // evaluate causal model with setting x' for cause
        Set<Literal> evaluationModified = CausalitySolver.evaluateEquations(causalModelModified, context,
                phiFormula.variables().toArray(new Variable[0]));
        // check if not(phi) evaluates to true for empty W -> if yes, no further investigation necessary
        if (phiFormula.evaluate(new Assignment(evaluationModified))) {
            return new HashSet<>();
        }

        for (Set<Literal> w : allW) {
            // create copy of modified causal model
            CausalModel causalModelModifiedW = new CausalModel(causalModelModified);
            // replace equations of variables in W with true/false
            for (Literal l : w) {
                causalModelModifiedW.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                        .forEach(e -> e.setFormula(l.phase() ? f.verum() : f.falsum()));
            }
            // evaluate all variables in the negated phi
            evaluationModified = CausalitySolver.evaluateEquations(causalModelModifiedW, context,
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
     * Simplifies a given formula. If a variable in the formula is defined by exogenous variables only, it is not
     * further simplified. If a variable is in W or cause or is exogenous we replace it with true/false, depending on
     * its original value as defined by W, cause or evaluation. This method is called recursively until no further
     * simplification is possible.
     *
     * @param formula     the to be simplified formula
     * @param causalModel the corresponding causal model
     * @param cause       the hypothesized cause
     * @param w           the set of literals that are kept at their original value.
     * @param evaluation  the evaluation of all variables
     * @return a simplified version of the formula
     */
    // TODO maybe we do not need this function anymore
    private static Formula simplify(Formula formula, CausalModel causalModel, Set<Literal> cause, Set<Literal> w,
                                    Set<Literal> evaluation) {
        FormulaFactory f = new FormulaFactory();
        if (!(formula instanceof Constant)) {
            Formula simplifiedFormula = formula;
            // simplify each variable
            for (Variable variable : formula.variables()) {
                if (simplifiedFormula instanceof Constant)
                    // if we do not stop simplification here, then true or false might be replaced with an equation
                    break;
                // replace variables in W and exogenous variables with true/false
                if (w.stream().map(Literal::variable).collect(Collectors.toSet()).contains(variable) ||
                        causalModel.getExogenousVariables().contains(variable)) {
                    // no need to check if the literal exists as done before!
                    Literal literal = evaluation.stream().filter(l -> l.variable().equals(variable)).findFirst().get();
                    simplifiedFormula = formula.substitute(variable,
                            (literal.phase() ? f.verum() : f.falsum()));
                }
                // replace variable in cause with true/false; NOTE: we negate the cause!
                else if (cause.stream().map(Literal::variable).collect(Collectors.toSet()).contains(variable)) {
                    // no need to check if the literal exists as done before!
                    Literal literal = cause.stream().filter(l -> l.variable().equals(variable)).findFirst().get()
                            .negate();
                    simplifiedFormula = formula.substitute(variable,
                            (literal.phase() ? f.verum() : f.falsum()));
                }
                // replace all other literals with their equation
                else {
                    Equation correspondingEquation = causalModel.getEquations().stream()
                            .filter(e -> e.getVariable().equals(variable)).findFirst().get();
                    simplifiedFormula = formula.substitute(variable, correspondingEquation.getFormula());
                }
            }
            return simplify(simplifiedFormula, causalModel, cause, w, evaluation);
        } else {
            return formula;
        }
    }
}
