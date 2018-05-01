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
class RealSATCausalitySolver extends CausalitySolver {
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
        Formula formula = generateSATQuery(causalModel, phiFormula, cause, context, evaluation, f);

        satSolver.add(formula);
        if (satSolver.sat() == Tristate.TRUE) {
            Assignment assignment = satSolver.model();
            // TODO minimal w? currently, we take the maximum W -> need to check, if it works for a smaller one as well
            Set<Literal> w = assignment.literals().stream()
                    .filter(l -> evaluation.contains(l) && (evaluationModified.contains(l.negate()) ||
                            (evaluationModified.contains(l) &&
                                    !causalModel.getExogenousVariables().contains(l.variable()))))
                    .collect(Collectors.toSet());
            return w;
        } else {
            return null;
        }
    }

    private Formula generateSATQuery(CausalModel causalModel, Formula notPhi, Set<Literal> cause, Set<Literal> context,
                                     Set<Literal> evaluation, FormulaFactory f) {
        Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
        Formula formula = f.and(notPhi, f.and(context), f.not(f.and(cause)));
        for (Equation equation : causalModel.getEquations()) {
            if (!causeVariables.contains(equation.getVariable())) {
                // we know that it exists!
                Literal originalValue = evaluation.stream().filter(l -> l.variable().equals(equation.getVariable()))
                        .findFirst().get();
                Formula equationFormula = f.or(originalValue, f.equivalence(equation.getVariable(), equation.getFormula()));
                formula = f.and(formula, equationFormula);
            }
        }
        return formula;
    }
}
