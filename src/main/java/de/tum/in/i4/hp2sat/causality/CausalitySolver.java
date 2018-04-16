package de.tum.in.i4.hp2sat.causality;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.*;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.*;
import java.util.stream.Collectors;

class CausalitySolver {
    static CausalitySolverResult solve(CausalModel causalModel, Map<Variable, Constant> context, Set<Literal> phi,
                                       Set<Literal> cause) {
        Set<Literal> evaluation = evaluateEquations(causalModel, context);
        boolean ac1 = fulfillsAC1(evaluation, phi, cause);
        boolean ac2 = false;
        boolean ac3 = false;
        if (ac1) {
            ac2 = fulfillsAC2(causalModel, phi, cause, evaluation);
            if (ac2) {
                // get all possible Ws, i.e create power set of the evaluation
                Set<Set<Literal>> allSubsetsOfCause = new UnifiedSet<>(cause).powerSet().stream()
                        .map(s -> s.toImmutable().castToSet())
                        .filter(s -> s.size() > 0 && s.size() < cause.size()) // remove empty set and full cause
                        .collect(Collectors.toSet());
                // no sub-cause must fulfill AC1 and AC2
                ac3 = allSubsetsOfCause.stream().noneMatch(c -> fulfillsAC1(evaluation, phi, cause) &&
                        fulfillsAC2(causalModel, phi, c, evaluation));
            }
        }
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3);
        return causalitySolverResult;
    }

    static Set<Literal> evaluateEquations(CausalModel causalModel, Map<Variable, Constant> context) {
        // assume that causal model is valid!
        /*
         * Following to HP, we can sort variables in an acyclic causal model according to their dependence on other
         * variables. The following applies: "If X < Y, then the value of X may affect the value of Y , but the value
         * of Y cannot affect the value of X"
         * */
        List<Equation> equationsSorted = new ArrayList<>(causalModel.getEquations()).stream()
                .sorted((equation1, equation2) -> {
                    // the following comments assume: X is defined by equation1 and Y is defined by equation2
                    if (causalModel.isVariableInEquation(equation2.getVariable(), equation1)) {
                        // if Y is used in the formula of X, then Y < X -> return 1
                        return 1;
                    } else if (causalModel.isVariableInEquation(equation1.getVariable(), equation2)) {
                        // if X is used in the formula of Y, then X < Y -> return -1
                        return -1;
                    } else {
                        /*
                         * We need to ensure that variables defined by exogenous variables only always come before
                         * variables defined by endo- AND exogenous variables (or possibly endogenous variables only).
                         * On that way, we ensure that we can properly evaluate all variabls given a context */
                        if (causalModel.getExogenousVariables().containsAll(equation1.getFormula().variables()) &&
                                !causalModel.getExogenousVariables().containsAll(equation2.getFormula().variables())) {
                            return -1;
                        } else if (causalModel.getExogenousVariables()
                                .containsAll(equation2.getFormula().variables()) &&
                                !causalModel.getExogenousVariables().containsAll(equation1.getFormula().variables())) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }).collect(Collectors.toList());

        // initially, we can only assign true/false to the respective exogenous variables as defined by the context
        Assignment assignment = new Assignment();
        context.forEach((v, c) -> {
            if (c instanceof CTrue) {
                assignment.addLiteral(v);
            } else if (c instanceof CFalse) {
                assignment.addLiteral(v.negate());
            }
        });
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
            } else if (evaluation instanceof CFalse) {
                assignment.addLiteral(equation.getVariable().negate());
            }
        }
        /*
         * Finally, we return the literals of the assignment. A positive/negative literal indicates that the
         * corresponding variable evaluates to true/false  */
        return assignment.literals();
    }

    /**
     * Checks if AC1 fulfilled.
     *
     * @param evaluation the original evaluation of variables
     * @param phi        the phi
     * @param cause      the cause for which we check AC1
     * @return true if AC1 fulfilled, else false
     */
    private static boolean fulfillsAC1(Set<Literal> evaluation, Set<Literal> phi, Set<Literal> cause) {
        return evaluation.containsAll(phi) && evaluation.containsAll(cause);
    }

    /**
     * Checks if AC2 is fulfilled.
     *
     * @param causalModel the underlying causal model
     * @param phi         the phi
     * @param cause       the cause for which we check AC2
     * @param evaluation  the original evaluation of variables
     * @return true if AC2 fulfilled, else false
     */
    private static boolean fulfillsAC2(CausalModel causalModel, Set<Literal> phi, Set<Literal> cause,
                                       Set<Literal> evaluation) {
        // remove exogenous variables from evaluation as they are not needed for computing the Ws
        Set<Literal> evaluationWithoutExogenousVariables = evaluation.stream()
                .filter(l -> !causalModel.getExogenousVariables().contains(l.variable())).collect(Collectors.toSet());
        // get all possible Ws, i.e create power set of the evaluation
        List<Set<Literal>> allW = new UnifiedSet<>(evaluationWithoutExogenousVariables).powerSet().stream()
                .map(s -> s.toImmutable().castToSet())
                .sorted(Comparator.comparingInt(Set::size))
                .collect(Collectors.toList());

        FormulaFactory f = new FormulaFactory();
        Formula phiFormula = f.not(f.and(phi));
        Set<Formula> simplifiedFormulas = new HashSet<>();
        for (Set<Literal> w : allW) {
            // for each W, simplify formula
            Formula simplifiedFormula = simplify(phiFormula, causalModel, cause, w, evaluation);
            simplifiedFormulas.add(simplifiedFormula);
        }
        // combine all simplified formulas together by OR
        Formula combinedWsFormula = f.or(simplifiedFormulas.stream()
                .filter(formula -> formula.variables().size() > 0).collect(Collectors.toSet()));
        // some variables need to be kept at their original value and the cause needs to be negated
        Formula requiredValuesFormula = f.and(evaluation.stream()
                .filter(l -> combinedWsFormula.variables().contains(l.variable()))
                .map(l -> {
                    if (cause.stream().map(Literal::variable).collect(Collectors.toSet()).contains(l.variable()))
                        /*
                         * need to negate the cause to check whether phi still occurs in the counterfactual scenario,
                         * i.e. where the cause does not occur anymore */
                        return l.negate();
                    else
                        return l;
                }).collect(Collectors.toSet()));
        // construct final formula using AND
        Formula finalFormula = f.and(requiredValuesFormula, combinedWsFormula);
        // instantiate SAT solver
        SATSolver miniSAT = MiniSat.miniSat(f);
        miniSAT.add(finalFormula);
        // obtain SAT result
        Tristate result = miniSAT.sat();
        return result == Tristate.TRUE;
    }

    /**
     * Simplifies a given formula. If a variable in the formula is part of the Cause, this variable is not further
     * simplified. Same, if the variable consists of exogenous variables only. If a variable is in W, we replace it
     * with true/false, depending on its original value as defined by W. Analogously for the evaluation. This method is
     * called recursively until no further simplification is possible.
     *
     * @param formula     the to be simplified formula
     * @param causalModel the corresponding causal model
     * @param cause       the hypothesized cause
     * @param w           the set of literals that are kept at their original value.
     * @param evaluation  the evaluation of exogenous variables
     * @return a simplified version of the formula
     */
    private static Formula simplify(Formula formula, CausalModel causalModel, Set<Literal> cause, Set<Literal> w,
                                    Set<Literal> evaluation) {
        FormulaFactory formulaFactory = new FormulaFactory();
        /*
         * get all simplifiable variables; the following must apply:
         * (1) the variable must not be part of the cause
         * (2) the variable must not consist of exogenous variables only*/
        Set<Variable> simplifiableVariables = formula.variables().stream()
                .filter(v -> !cause.contains(v)).collect(Collectors.toSet());
        Set<Variable> simplifiableVariablesTemp = new HashSet<>();
        for (Variable variable : simplifiableVariables) {
            if (causalModel.getExogenousVariables().contains(variable)) {
                /*
                 * this case can only apply if the exogenous variable is in a formula together with some endogenous
                 * variables. We then need to "simplify" this exogenous variable as well by replacing it with
                 * true/false depending on its evaluation in the underlying scenario */
                simplifiableVariablesTemp.add(variable);
            } else {
                // no need to check if equation exists, as we ensure this by validating the causal model
                Equation correspondingEquation = causalModel.getEquations().stream().filter(e -> e.getVariable().equals
                        (variable)).findFirst().get();
                Formula f = correspondingEquation.getFormula();
                // only if the variable is not defined by exogenous variables only, it is simplifiable
                if (!causalModel.getExogenousVariables().containsAll(f.variables()))
                    simplifiableVariablesTemp.add(variable);
            }
        }
        simplifiableVariables = simplifiableVariablesTemp;

        if (simplifiableVariables.size() > 0) {
            Formula simplifiedFormula = formula;
            // simplify each variable
            for (Variable variable : simplifiableVariables) {
                if (w.stream().map(Literal::variable).collect(Collectors.toSet()).contains(variable)) {
                    // no need to check if the literal exists as done before!
                    Literal literal = w.stream().filter(l -> l.variable().equals(variable)).findFirst().get();
                    simplifiedFormula = formula.substitute(variable,
                            (literal.phase() ? formulaFactory.verum() : formulaFactory.falsum()));
                } else if (causalModel.getExogenousVariables().contains(variable)) {
                    Literal literal = evaluation.stream().filter(l -> l.variable().equals(variable)).findFirst().get();
                    simplifiedFormula = formula.substitute(variable,
                            (literal.phase() ? formulaFactory.verum() : formulaFactory.falsum()));
                } else {
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
