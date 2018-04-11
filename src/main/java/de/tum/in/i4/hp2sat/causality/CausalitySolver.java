package de.tum.in.i4.hp2sat.causality;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.*;

import java.util.*;
import java.util.stream.Collectors;

class CausalitySolver {
    static Tristate solve(CausalModel causalModel, Map<Variable, Constant> context, Set<Literal> phi,
                          Set<Literal> cause, Set<Variable> w) {
        // TODO
        return Tristate.UNDEF;
    }

    static Map<Variable, Boolean> evaluateEquations(CausalModel causalModel, Map<Variable, Constant> context) {
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
                        // TODO double check with more examples; can endo and exo vars be mixed?
                        /*
                        * Here comes the tricky part. If neither the previous checks did not match, we end up here.
                        * However, just returning 0 produces wrong results in some cases, which is due to
                        * transitivity. To ensure proper ordering, we need to make sure that equations containing
                        * exogenous variables are considered "smaller" than ones that don't. Only if both equations
                        * contain exogenous variables, we return 0. On that way we ensure that during the evaluation
                        * the exogenous variables are evaluated first. */
                        if (equation1.getFormula().variables().stream().anyMatch(v -> causalModel
                                .getExogenousVariables().contains(v)) && equation2.getFormula().variables().stream().noneMatch(v -> causalModel.getExogenousVariables().contains(v))) {
                            return -1;
                        } else if (equation1.getFormula().variables().stream().anyMatch(v -> causalModel
                                .getExogenousVariables().contains(v)) && equation2.getFormula().variables().stream()
                                .noneMatch(v -> causalModel.getExogenousVariables().contains(v))) {
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
         * Finally, we return a map of variables and their corresponding evaluation. The phase of a
         * Literal (true/false) in the assignment indicates the evaluation. */
        Map<Variable, Boolean> evaluation = assignment.literals().stream()
                .collect(Collectors.toMap(Literal::variable, Literal::phase));
        return evaluation;
    }
}
