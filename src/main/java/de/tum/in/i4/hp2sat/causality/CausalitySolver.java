package de.tum.in.i4.hp2sat.causality;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.*;

import java.util.*;
import java.util.stream.Collectors;

class CausalitySolver {
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
        Assignment assignment = new Assignment(context);
        return evaluateEquationsHelper(causalModel, causalModel.getEquations(), assignment, variables);
    }

    /**
     * Helper method that can be called recursively to avoid unevaluated equations.
     *
     * @param causalModel the causal model
     * @param equations   the equations that need to be evaluated
     * @param assignment  the currently known assignment
     * @param variables   if some variables are given, then only their evaluation is returned
     * @return evaluation for all variables within the causal model (endo and exo); positive literal means true,
     * negative means false
     */
    private static Set<Literal> evaluateEquationsHelper(CausalModel causalModel, Set<Equation> equations,
                                                        Assignment assignment, Variable... variables) {
        // assume that causal model is valid!
        /*
         * Following to HP, we can sort variables in an acyclic causal model according to their dependence on other
         * variables. The following applies: "If X < Y, then the value of X may affect the value of Y , but the value
         * of Y cannot affect the value of X"
         * */
        List<Equation> equationsSorted = new ArrayList<>(equations).stream()
                .sorted((equation1, equation2) -> {
                    // the following comments assume: X is defined by equation1 and Y is defined by equation2
                    if (causalModel.isVariableInEquation(equation2.getVariable(), equation1)) {
                        // if Y is used in the formula of X, then Y < X -> return 1
                        return 1;
                    } else if (causalModel.isVariableInEquation(equation1.getVariable(), equation2)) {
                        // if X is used in the formula of Y, then X < Y -> return -1
                        return -1;
                    } else {
                        Set<Variable> exoVars = causalModel.getExogenousVariables();
                        /*
                         * We need to ensure that variables defined by exogenous variables only always come before
                         * variables defined by endo- AND exogenous variables (or possibly endogenous variables only)
                         * and that variables defined by exo- and endogenous variables come before variables defined
                         * by endogenous variables only. On that way, we ensure that we can properly evaluate all
                         * variables given a context */
                        if (exoVars.containsAll(equation1.getFormula().variables()) &&
                                !exoVars.containsAll(equation2.getFormula().variables())) {
                            return -1;
                        } else if (exoVars.containsAll(equation2.getFormula().variables()) &&
                                !exoVars.containsAll(equation1.getFormula().variables())) {
                            return 1;
                        } else if (equation1.getFormula().variables().stream().anyMatch(exoVars::contains) &&
                                equation2.getFormula().variables().stream().noneMatch(exoVars::contains)) {
                            return -1;
                        } else if (equation2.getFormula().variables().stream().anyMatch(exoVars::contains) &&
                                equation1.getFormula().variables().stream().noneMatch(exoVars::contains)) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }).collect(Collectors.toList());

        Set<Equation> unevaluatedEquations = new HashSet<>();
        // initially, we can only assign the exogenous variables as defined by the context
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
            } else {
                // add equation to unevaluated equations
                unevaluatedEquations.add(equation);
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

        if (unevaluatedEquations.size() != 0) {
            // if some equations are still unevaluated, recursively call method
            /*
             * This does not happen very often. The problem seems to be the sort function. */
            return evaluateEquationsHelper(causalModel, unevaluatedEquations, assignment, variables);
        } else {
            /*
             * Finally, we return the literals of the assignment. A positive/negative literal indicates that the
             * corresponding variable evaluates to true/false  */
            return assignment.literals();
        }
    }

    /**
     * Checks if AC1 fulfilled.
     *
     * @param evaluation the original evaluation of variables
     * @param phi        the phi
     * @param cause      the cause for which we check AC1
     * @return true if AC1 fulfilled, else false
     */
    static boolean fulfillsAC1(Set<Literal> evaluation, Formula phi, Set<Literal> cause) {
        Set<Literal> litersOfPhi = phi.literals();
        return evaluation.containsAll(litersOfPhi) && evaluation.containsAll(cause);
    }
}
