package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.*;

import java.util.*;
import java.util.stream.Collectors;

class CausalitySolver {
    /**
     * Checks AC1, AC2 and AC3 given a causal model, a cause, a context and phi. Solving strategy is STANDARD.
     *
     * @param causalModel the underlying causel model
     * @param context     the context
     * @param phi         the phi
     * @param cause       the cause
     * @return for each AC, true if fulfilled, false else
     */
    static CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
                                       Set<Literal> cause) throws InvalidCausalModelException {
        return solve(causalModel, context, phi, cause, SolvingStrategy.STANDARD);
    }

    /**
     * Checks AC1, AC2 and AC3 given a causal model, a cause, a context and phi and a solving strategy.
     *
     * @param causalModel     the underlying causel model
     * @param context         the context
     * @param phi             the phi
     * @param cause           the cause
     * @param solvingStrategy the applied solving strategy
     * @return for each AC, true if fulfilled, false else
     */
    static CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
                                       Set<Literal> cause, SolvingStrategy solvingStrategy)
            throws InvalidCausalModelException {
        Set<Literal> evaluation = evaluateEquations(causalModel, context);
        boolean ac1 = fulfillsAC1(evaluation, phi, cause);
        Set<Literal> w = fulfillsAC2(causalModel, phi, cause, evaluation, solvingStrategy);
        boolean ac2 = w != null;
        boolean ac3 = fulfillsAC3(causalModel, phi, cause, evaluation, solvingStrategy);
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
        return causalitySolverResult;
    }

    /**
     * Returns all causes for a given causal model, a context and phi.
     *
     * @param causalModel the underlying causel model
     * @param context     the context
     * @param phi         the phi
     * @return set of all causes, i.e. AC1-AC3 fulfilled, as set of results
     */
    static Set<CausalitySolverResult> getAllCauses(CausalModel causalModel, Set<Literal> context, Formula phi)
            throws InvalidCausalModelException {
        // compute all possible combination of primitive events
        Set<Literal> evaluation = evaluateEquations(causalModel, context);
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
                CausalitySolverResult causalitySolverResult = CausalitySolver.solve(causalModel, context, phi, cause);
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

    /**
     * Checks if AC2 is fulfilled given a solving strategy. Wrapper for the actual fulfillsAC2 method.
     *
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @return internally calls another method the checks for AC2; returns true if AC2 fulfilled, else false
     */
    private static Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                            Set<Literal> evaluation, SolvingStrategy solvingStrategy)
            throws InvalidCausalModelException {
        if (solvingStrategy == SolvingStrategy.STANDARD) {
            // remove exogenous variables from evaluation as they are not needed for computing the Ws
            Set<Literal> evaluationWithoutExogenousVariables = evaluation.stream()
                    .filter(l -> !causalModel.getExogenousVariables().contains(l.variable()))
                    .collect(Collectors.toSet());
            // get all possible Ws, i.e create power set of the evaluation
            List<Set<Literal>> allW = new UnifiedSet<>(evaluationWithoutExogenousVariables).powerSet().stream()
                    .map(s -> s.toImmutable().castToSet())
                    .sorted(Comparator.comparingInt(Set::size))
                    .collect(Collectors.toList());
            return fulfillsAC2(causalModel, phi, cause, evaluation, allW);
        } else {
            return null;
        }
    }

    /**
     * Checks if AC2 is fulfilled.
     *
     * @param causalModel the underlying causal model
     * @param phi         the phi
     * @param cause       the cause for which we check AC2
     * @param evaluation  the original evaluation of variables
     * @param allW        set of all relevant W
     * @return true if AC2 fulfilled, else false
     */
    private static Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                            Set<Literal> evaluation, List<Set<Literal>> allW)
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

        for (Set<Literal> w : allW) {
            // create copy of modified causal model
            CausalModel causalModelModifiedW = new CausalModel(causalModelModified);
            // replace equations of variables in W with true/false
            for (Literal l : w) {
                causalModelModifiedW.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                        .forEach(e -> e.setFormula(l.phase() ? f.verum() : f.falsum()));
            }
            // evaluate all variables in the negated phi
            Set<Literal> evaluationModified = evaluateEquations(causalModelModifiedW, evaluation.stream()
                    .filter(l -> causalModelModifiedW.getExogenousVariables().contains(l.variable())) // get context
                    .collect(Collectors.toSet()), phiFormula.variables().toArray(new Variable[0]));
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
    private static boolean fulfillsAC3(CausalModel causalModel, Formula phi, Set<Literal> cause,
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
                        fulfillsAC2(causalModel, phi, c, evaluation, solvingStrategy) != null;
            } catch (InvalidCausalModelException e) {
                e.printStackTrace();
                return false;
            }
        });
        return ac3;
    }
}
