package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// TODO rename
class SATCausalitySolver extends CausalitySolver {
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
    @Override
    Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                             Set<Literal> evaluation, SolvingStrategy solvingStrategy)
            throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        SATSolver satSolver = MiniSat.miniSat(f); // TODO make dynamic?
        Formula phiFormula = f.not(phi); // negate phi

        // create copy of original causal model
        CausalModel causalModelModified = createModifiedCausalModelForCause(causalModel, cause, f);

        // evaluate causal model with setting x' for cause
        Set<Literal> evaluationModified = evaluateEquations(causalModelModified, context);
        // check if not(phi) evaluates to true for empty W -> if yes, no further investigation necessary
        if (phiFormula.evaluate(new Assignment(evaluationModified))) {
            return new HashSet<>();
        }
        // generate SAT query
        Formula formula = generateSATQuery(causalModelModified, phiFormula, cause, context, evaluation, f);
        // add query to solver
        satSolver.add(formula);
        if (satSolver.sat() == Tristate.TRUE) {
            // if satisfiable, get the assignment for which the formula is satisfiable
            Assignment assignment = satSolver.model();
            // TODO minimal w? currently, we take the maximum W -> need to check, if it works for a smaller one as well
            // generate (maximum) W
            Set<Literal> w = assignment.literals().stream()
                    .filter(l -> evaluation.contains(l) && !causalModel.getExogenousVariables().contains(l.variable()))
                    .collect(Collectors.toSet());
            return w;
        } else {
            // if not satisfiable
            return null;
        }
    }

    /**
     * Generates a formula whose satisfiability indicates whether AC2 is fulfilled or not.
     *
     * @param causalModelModified the causal model
     * @param notPhi              the negated phi
     * @param cause               the cause
     * @param context             the context
     * @param evaluation          the original evaluation under the given context
     * @param f                   a formula factory
     * @return a formula
     */
    private Formula generateSATQuery(CausalModel causalModelModified, Formula notPhi, Set<Literal> cause, Set<Literal> context,
                                     Set<Literal> evaluation, FormulaFactory f) {
        // get all variables in cause
        Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
        // create formula: !phi AND context
        Formula formula = f.and(notPhi, f.and(context));
        for (Equation equation : causalModelModified.getEquations()) {
            // get value of variable in original iteration
            Literal originalValue = evaluation.stream().filter(l -> l.variable().equals(equation.getVariable()))
                    .findFirst().get(); // we know that it exists! -> no need to check isPresent()
            /*
             * create formula: V_originalValue OR (V <=> Formula_V)
             * if the variable of the current equation is in the cause, then we do not allow for its original value
             * and just add (V <=> Formula_V). Notice that Formula_V will be a constant if V is in the cause since we
             * are considering the modified causal model. */
            Formula equationFormula = causeVariables.contains(equation.getVariable()) ?
                    f.equivalence(equation.getVariable(), equation.getFormula()) :
                    f.or(originalValue, f.equivalence(equation.getVariable(), equation.getFormula()));
            // add created formula to global formula by AND
            formula = f.and(formula, equationFormula);
        }
        return formula;
    }
}
