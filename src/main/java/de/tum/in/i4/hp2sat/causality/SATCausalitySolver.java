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
     * Checks if AC2 is fulfilled given a solving strategy. Wrapper for the actual fulfillsAC2 method.
     *
     * @param causalModel the underlying causal model
     * @param phi         the phi
     * @param cause       the cause for which we check AC2
     * @param evaluation  the original evaluation of variables
     * @return internally calls another method the checks for AC2; returns true if AC2 fulfilled, else false
     */
    private static Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                            Set<Literal> evaluation)
            throws InvalidCausalModelException {
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

        Set<Literal> evaluationModified = evaluateEquations(causalModelModified, evaluation.stream()
                .filter(l -> causalModelModified.getExogenousVariables().contains(l.variable())) // get context
                .collect(Collectors.toSet()));

        return fulfillsAC2Helper(causalModelModified, phiFormula, evaluation, evaluationModified, f, satSolver,
                new HashSet<>());
    }

    // TODO doc
    private static Set<Literal> fulfillsAC2Helper(CausalModel causalModel, Formula phi, Set<Literal> evaluation,
                                                  Set<Literal> evaluationModified, FormulaFactory f,
                                                  SATSolver satSolver, Set<Formula> checkedFormulas)
            throws InvalidCausalModelException {
        satSolver.reset();
        satSolver.add(phi);
        // TODO maybe not all solutions at a time, but "lazy"
        Set<Set<Literal>> solutions = satSolver.enumerateAllModels().stream().map(Assignment::literals)
                .collect(Collectors.toSet());
        if (solutions == null) {
            return null;
        }

        for (Set<Literal> solution : solutions) {
            boolean plausible = solution.stream()
                    .allMatch(l -> evaluation.contains(l) || evaluationModified.contains(l));
            if (plausible) {
                Set<Literal> w = solution.stream()
                        .filter(l -> evaluation.contains(l) && evaluationModified.contains(l.negate()))
                        .collect(Collectors.toSet());
                CausalModel causalModelModifiedW = new CausalModel(causalModel);
                for (Literal l : w) {
                    causalModelModifiedW.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                            .forEach(e -> e.setFormula(l.phase() ? f.verum() : f.falsum()));
                }
                Set<Literal> notInW = solution.stream().filter(l -> !w.contains(l)).collect(Collectors.toSet());
                Set<Literal> evaluationModifiedW = evaluateEquations(causalModelModifiedW, evaluation.stream()
                        .filter(l -> causalModelModifiedW.getExogenousVariables().contains(l.variable())) // get context
                        .collect(Collectors.toSet()), phi.variables().toArray(new Variable[0]));
                if (evaluationModifiedW.containsAll(notInW)) {
                    return w;
                } else {
                    Set<Literal> changedLiterals = evaluationModifiedW.stream()
                            .filter(l -> notInW.contains(l.negate()) && !w.contains(l)).map(Literal::negate)
                            .collect(Collectors.toSet());
                    for (Literal l : changedLiterals) {
                        causalModelModifiedW.getEquations().stream().filter(e -> e.getVariable().equals(l.variable()))
                                .forEach(e -> e.setFormula(l.phase() ? f.verum() : f.falsum()));
                    }
                    w.addAll(changedLiterals);
                    Set<Literal> notInWNew = solution.stream().filter(l -> !w.contains(l)).collect(Collectors.toSet());
                    // TODO check if one re-eval is fine
                    evaluationModifiedW = evaluateEquations(causalModelModifiedW, evaluation.stream()
                            .filter(l -> causalModelModifiedW.getExogenousVariables().contains(l.variable())) // get context
                            .collect(Collectors.toSet()), phi.variables().toArray(new Variable[0]));
                    if (evaluationModifiedW.containsAll(notInWNew)) {
                        return w;
                    }
                }
            }
        }

        // TODO powerset method
        List<Set<Variable>> allCombinationOfVariables = new UnifiedSet<>(phi.variables()).powerSet()
                .stream().map(s -> s.toImmutable().castToSet())
                .sorted(Comparator.comparingInt(Set::size))
                .collect(Collectors.toList());
        allCombinationOfVariables.remove(0);

        for (Set<Variable> variables : allCombinationOfVariables) {
            Formula phiModified = phi;
            for (Variable v : variables) {
                Equation correspondingEquation = causalModel.getEquations().stream()
                        .filter(e -> e.getVariable().equals(v)).findFirst().get();
                phiModified = phiModified.substitute(v, correspondingEquation.getFormula());
            }
            for (Variable v : causalModel.getExogenousVariables()) {
                Literal literal = evaluation.stream().filter(l -> l.variable().equals(v)).findFirst().get();

                phiModified = phiModified.substitute(v, literal.phase() ? f.verum() : f.falsum());
            }
            for (Variable v : phiModified.variables()) {
                Formula correspondingFormula = causalModel.getEquations().stream()
                        .filter(e -> e.getVariable().equals(v)).findFirst().get().getFormula();
                if (correspondingFormula instanceof Constant) {
                    phiModified = phiModified.substitute(v, correspondingFormula);
                }
            }
            if (checkedFormulas.contains(phiModified)) {
                continue;
            } else {
                checkedFormulas.add(phiModified);
            }

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
