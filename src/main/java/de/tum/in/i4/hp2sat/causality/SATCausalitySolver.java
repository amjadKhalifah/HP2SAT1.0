package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.*;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.CausalitySolver.evaluateEquations;
import static de.tum.in.i4.hp2sat.causality.CausalitySolver.fulfillsAC1;

class SATCausalitySolver {
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
        Set<Literal> w = fulfillsAC2(causalModel, phi, cause, evaluation);
        boolean ac2 = w != null;
        boolean ac3 = fulfillsAC3(causalModel, phi, cause, evaluation, solvingStrategy);
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
        return causalitySolverResult;
    }

    /**
     * Checks if AC2 is fulfilled. Wrapper for the actual fulfillsAC2 method.
     *
     * @param causalModel the underlying causal model
     * @param phi         the phi
     * @param cause       the cause for which we check AC2
     * @param evaluation  the original evaluation of variables
     * @return internally calls another method the checks for AC2; returns W if AC2 fulfilled, else null
     */
    private static Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                            Set<Literal> evaluation)
            throws InvalidCausalModelException {
        // TODO get context as arg
        FormulaFactory f = new FormulaFactory();
        SATSolver satSolver = MiniSat.miniSat(f); // TODO make dynamic?
        Formula phiFormula = f.not(phi); // negate phi

        // TODO create method to avoid code duplication
        // create copy of original causal model
        CausalModel causalModelModified = new CausalModel(causalModel);
        // replace equation of each part of the cause with its negation, i.e. setting x'
        for (Literal l : cause) {
            causalModelModified.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                    .forEach(e -> e.setFormula(l.negate().phase() ? f.verum() : f.falsum()));
        }

        // evaluate causal model with setting x' for cause
        Set<Literal> evaluationModified = evaluateEquations(causalModelModified, evaluation.stream()
                .filter(l -> causalModelModified.getExogenousVariables().contains(l.variable())) // get context
                .collect(Collectors.toSet()));
        // TODO directly return true if !phi is fulfilled with empty W

        // IMPORTANT: we call the helper with the negated phi!
        return fulfillsAC2Helper(causalModelModified, phiFormula, evaluation, evaluationModified, f, satSolver,
                new HashSet<>());
    }

    /**
     * Checks if AC2 is fulfilled.
     *
     * @param causalModel        the underlying causal model
     * @param phi                the phi
     * @param evaluation         the original evaluation of variables
     * @param evaluationModified the evaluation of variables with setting x'
     * @param f                  formula factory instance
     * @param satSolver          SAT solver instance
     * @param checkedFormulas    the formulas that have already been checked; internal detail
     * @return called recursively; returns W if AC2 fulfilled, else null
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    private static Set<Literal> fulfillsAC2Helper(CausalModel causalModel, Formula phi, Set<Literal> evaluation,
                                                  Set<Literal> evaluationModified, FormulaFactory f,
                                                  SATSolver satSolver, Set<Formula> checkedFormulas)
            throws InvalidCausalModelException {
        // reset SAT solver
        satSolver.reset();
        // add phi to the SAT solver
        satSolver.add(phi);

        // TODO maybe not all solutions at a time, but "lazy"
        // get all variable assignments such that phi is satisfied
        Set<Set<Literal>> solutions = satSolver.enumerateAllModels().stream().map(Assignment::literals)
                .collect(Collectors.toSet());
        if (solutions == null) {
            // phi cannot be satisfied
            return null;
        }

        for (Set<Literal> solution : solutions) {
            /*
             * For each solution, we check whether it is plausible. That is, whether we can construct a set W. This is
             * the case, if all the variables in the solution are either in the original evaluation or in the
             * modified evaluation.
             *
             * Example 1:
             * -> phi = B and C -> 1 possible solution: B=1, C=1
             * -> original evaluation: B=0, C=0; modified evaluation: B=0, C=0
             * -> in this case the solution IS NOT plausible, since B=1,C=1 is neither in the original evaluation nor
             * in the modified one.
             *
             * Example 2:
             * -> phi = B and C -> 1 possible solution: B=1, C=1
             * -> original evaluation: B=0, C=1; modified evaluation: B=1, C=0
             * -> in this case the solution IS plausible, since B=1 is in the modified evaluation and C=0 is in the
             * original evaluation. That is, keeping C at its original value C=1 by constructing a respective set W,
             * phi is possibly fulfilled. */
            boolean plausible = solution.stream()
                    .allMatch(l -> evaluation.contains(l) || evaluationModified.contains(l));
            if (plausible) {
                /*
                 * If a solution is plausible, we need to check whether the intervention, i.e. the construction of a
                 * set W does not affect other variables not in W such that phi is not fulfilled. */

                // construct W: the variable in the solution must have flipped its value in the modified evaluation
                Set<Literal> w = solution.stream()
                        .filter(l -> evaluation.contains(l) && evaluationModified.contains(l.negate()))
                        .collect(Collectors.toSet());
                // create modified causal model by applying W, i.e. replace respective equations with true/false
                CausalModel causalModelModifiedW = new CausalModel(causalModel);
                for (Literal l : w) {
                    causalModelModifiedW.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                            .forEach(e -> e.setFormula(l.phase() ? f.verum() : f.falsum()));
                }
                // create set of literals that are not in W
                Set<Literal> notInW = solution.stream().filter(l -> !w.contains(l)).collect(Collectors.toSet());
                // evaluate the variables in phi again given the modified causal model that incorporates W
                Set<Literal> evaluationModifiedW = evaluateEquations(causalModelModifiedW, evaluation.stream()
                        .filter(l -> causalModelModifiedW.getExogenousVariables().contains(l.variable())) // get context
                        .collect(Collectors.toSet()), phi.variables().toArray(new Variable[0]));

                if (evaluationModifiedW.containsAll(notInW)) {
                    /*
                     * if all variables not in W did not change their value, then we know that we have found a valid
                     * solution: We can construct a set W such that the combination of W and notW matches the solution
                     * and the variables not in W are not affected by the intervention. */
                    return w;
                } else {
                    /*
                     * It might happen that the variables in W affect other variables such that the current solution
                     * is not fulfilled anymore. We can than try to add all those variables that have changed their
                     * value to W as well. */

                    // get the literals that changed and negate them such that we obtain their original value
                    Set<Literal> changedLiterals = evaluationModifiedW.stream()
                            .filter(l -> notInW.contains(l.negate()) && !w.contains(l)).map(Literal::negate)
                            .collect(Collectors.toSet());
                    // add the changed literals to W
                    w.addAll(changedLiterals);
                    // apply the new parts of W to the modified causal model
                    for (Literal l : changedLiterals) {
                        causalModelModifiedW.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                                .forEach(e -> e.setFormula(l.phase() ? f.verum() : f.falsum()));
                    }
                    // re-compute the literals not in W
                    Set<Literal> notInWNew = solution.stream().filter(l -> !w.contains(l)).collect(Collectors.toSet());
                    // TODO check if one re-eval is fine or if we need more or recursion
                    // re-evaluate
                    evaluationModifiedW = evaluateEquations(causalModelModifiedW, evaluation.stream()
                            .filter(l -> causalModelModifiedW.getExogenousVariables().contains(l.variable())) // get context
                            .collect(Collectors.toSet()), phi.variables().toArray(new Variable[0]));
                    // check again if the new W affected the variables not in W
                    if (evaluationModifiedW.containsAll(notInWNew)) {
                        return w;
                    }
                }
            }
        }

        /*
         * If for the current phi no solution was found that is plausible and valid, we need to simplify/modify phi in
         * order to reach other variables that we can include into W. The simplification can be done using the
         * equations from the causal model. */

        // TODO powerset method
        // TODO can we optimize that and consider "relevant" variables only?
        // get all possible combinations of current variables in phi
        List<Set<Variable>> allCombinationOfVariables = new UnifiedSet<>(phi.variables()).powerSet()
                .stream().map(s -> s.toImmutable().castToSet())
                .sorted(Comparator.comparingInt(Set::size))
                .collect(Collectors.toList());
        allCombinationOfVariables.remove(0);

        for (Set<Variable> variables : allCombinationOfVariables) {
            Formula phiModified = phi;
            // replace all variables by their corresponding equation as defined by the causal model
            for (Variable v : variables) {
                Equation correspondingEquation = causalModel.getEquations().stream()
                        .filter(e -> e.getVariable().equals(v)).findFirst().get();
                phiModified = phiModified.substitute(v, correspondingEquation.getFormula());
            }
            // replace exogenous variables by true/false
            for (Variable v : causalModel.getExogenousVariables()) {
                Literal literal = evaluation.stream().filter(l -> l.variable().equals(v)).findFirst().get();

                phiModified = phiModified.substitute(v, literal.phase() ? f.verum() : f.falsum());
            }
            // replace variables that are constants by true/false
            for (Variable v : phiModified.variables()) {
                Formula correspondingFormula = causalModel.getEquations().stream()
                        .filter(e -> e.getVariable().equals(v)).findFirst().get().getFormula();
                if (correspondingFormula instanceof Constant) {
                    phiModified = phiModified.substitute(v, correspondingFormula);
                }
            }

            if (checkedFormulas.contains(phiModified)) {
                // if the current simplified phi has already been checked, we do not need to check it again
                continue;
            } else {
                // add the simplified phi to the checked formulas
                checkedFormulas.add(phiModified);
            }

            // check if AC2 can be fulfilled using the modified phi
            Set<Literal> w = fulfillsAC2Helper(causalModel, phiModified, evaluation, evaluationModified, f,
                    satSolver, checkedFormulas);
            if (w != null) {
                return w;
            }
        }

        return null;
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
    // TODO use one method for both
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
                        fulfillsAC2(causalModel, phi, c, evaluation) != null;
            } catch (InvalidCausalModelException e) {
                e.printStackTrace();
                return false;
            }
        });
        return ac3;
    }
}
