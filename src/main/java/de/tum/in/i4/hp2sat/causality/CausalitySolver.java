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
    /**
     * Checks AC1, AC2 and AC3 given a causal model, a cause, a context and phi.
     *
     * @param causalModel the underlying causel model
     * @param context     the context
     * @param phi         the phi
     * @param cause       the cause
     * @return for each AC, true if fulfilled, false else
     */
    static CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
                                       Set<Literal> cause) {
        Set<Literal> evaluation = evaluateEquations(causalModel, context);
        boolean ac1 = fulfillsAC1(evaluation, phi, cause);
        Set<Literal> w = fulfillsAC2(causalModel, phi, cause, evaluation);
        boolean ac2 = w != null;
        boolean ac3 = fulfillsAC3(causalModel, phi, cause, evaluation);
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
        return causalitySolverResult;
    }

    /**
     * Evaluates the equations of the given causal model under a given context.
     *
     * @param causalModel the causal model
     * @param context     the context, i.e. the evaluation of the exogenous variables; positive literal means true,
     *                    negative means false
     * @return evaluation for all variables within the causal model (endo and exo); positive literal means true,
     * negative means false
     */
    static Set<Literal> evaluateEquations(CausalModel causalModel, Set<Literal> context) {
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
    private static boolean fulfillsAC1(Set<Literal> evaluation, Formula phi, Set<Literal> cause) {
        Set<Literal> litersOfPhi = phi.literals();
        return evaluation.containsAll(litersOfPhi) && evaluation.containsAll(cause);
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
    private static Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause,
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
        Formula phiFormula = f.not(phi); // negate phi

        SATSolver miniSAT = MiniSat.miniSat(f);
        for (Set<Literal> w : allW) {
            // for each W, simplify formula
            Formula simplifiedFormula = simplify(phiFormula, causalModel, cause, w, evaluation);
            /*
            * get all literals and the values they are required to have (expressed by their phase) such that the
            * simplified formula is possibly satisfiable while keeping all the not affected variables at their value
            * according to the original evaluation */
            Set<Literal> requiredLiterals = evaluation.stream()
                    .filter(l -> simplifiedFormula.variables().contains(l.variable())).collect(Collectors.toSet());
            // construct final formula
            Formula finalFormula = f.and(f.and(requiredLiterals), simplifiedFormula);
            // reset SAT solver
            miniSAT.reset();
            // add to-be-checked formula to SAT solver
            miniSAT.add(finalFormula);
            Tristate result = miniSAT.sat();
            // return true, i.e. AC2 fulfilled, if formula is satisfiable
            if (result == Tristate.TRUE)
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
    private static Formula simplify(Formula formula, CausalModel causalModel, Set<Literal> cause, Set<Literal> w,
                                    Set<Literal> evaluation) {
        FormulaFactory formulaFactory = new FormulaFactory();
        /*
         * get all simplifiable variables; the following must apply:
         * the variable must not consist of exogenous variables only*/
        Set<Variable> simplifiableVariables = new HashSet<>();
        for (Variable variable : formula.variables()) {
            if (causalModel.getExogenousVariables().contains(variable)) {
                /*
                 * this case can only apply if the exogenous variable is in a formula together with some endogenous
                 * variables. We then need to "simplify" this exogenous variable as well by replacing it with
                 * true/false depending on its evaluation in the underlying scenario */
                simplifiableVariables.add(variable);
            } else if (cause.stream().map(Literal::variable).collect(Collectors.toSet()).contains(variable)) {
                simplifiableVariables.add(variable);
            } else {
                // no need to check if equation exists, as we ensure this by validating the causal model
                Equation correspondingEquation = causalModel.getEquations().stream().filter(e -> e.getVariable().equals
                        (variable)).findFirst().get();
                Formula f = correspondingEquation.getFormula();
                // only if the variable is not defined by exogenous variables only, it is simplifiable
                if (!causalModel.getExogenousVariables().containsAll(f.variables()))
                    simplifiableVariables.add(variable);
            }
        }

        if (simplifiableVariables.size() > 0) {
            Formula simplifiedFormula = formula;
            // simplify each variable
            for (Variable variable : simplifiableVariables) {
                if (simplifiedFormula instanceof Constant)
                    // if we do not stop simplification here, then true or false might be replaced with an equation
                    break;
                // replace variables in W and exogenous variables with true/false
                if (w.stream().map(Literal::variable).collect(Collectors.toSet()).contains(variable) ||
                        causalModel.getExogenousVariables().contains(variable)) {
                    // no need to check if the literal exists as done before!
                    Literal literal = evaluation.stream().filter(l -> l.variable().equals(variable)).findFirst().get();
                    simplifiedFormula = formula.substitute(variable,
                            (literal.phase() ? formulaFactory.verum() : formulaFactory.falsum()));
                }
                // replace variable in cause with true/false; NOTE: we negate the cause!
                else if (cause.stream().map(Literal::variable).collect(Collectors.toSet()).contains(variable)) {
                    // no need to check if the literal exists as done before!
                    Literal literal = cause.stream().filter(l -> l.variable().equals(variable)).findFirst().get().negate();
                    simplifiedFormula = formula.substitute(variable,
                            (literal.phase() ? formulaFactory.verum() : formulaFactory.falsum()));
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

    /**
     * Checks if AC3 is fulfilled.
     *
     * @param causalModel the underlying causal model
     * @param phi         the phi
     * @param cause       the cause for which we check AC2
     * @param evaluation  the original evaluation of variables
     * @return true if A3 fulfilled, else false
     */
    private static boolean fulfillsAC3(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                       Set<Literal> evaluation) {
        // get all subsets of cause
        Set<Set<Literal>> allSubsetsOfCause = new UnifiedSet<>(cause).powerSet().stream()
                .map(s -> s.toImmutable().castToSet())
                .filter(s -> s.size() > 0 && s.size() < cause.size()) // remove empty set and full cause
                .collect(Collectors.toSet());
        // no sub-cause must fulfill AC1 and AC2
        boolean ac3 = allSubsetsOfCause.stream().noneMatch(c -> fulfillsAC1(evaluation, phi, cause) &&
                fulfillsAC2(causalModel, phi, c, evaluation) != null);
        return ac3;
    }
}
