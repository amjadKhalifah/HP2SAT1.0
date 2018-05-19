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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.SATSolverType.MINISAT;

class SATCausalitySolver extends CausalitySolver {
    /**
     * Checks if AC2 is fulfilled. Uses MiniSAT.
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
    protected Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                       Set<Literal> evaluation, SolvingStrategy solvingStrategy, FormulaFactory f)
            throws InvalidCausalModelException {
        return fulfillsAC2(causalModel, phi, cause, context, evaluation, solvingStrategy, MINISAT, f);
    }

    /**
     * Checks if AC2 is fulfilled.
     *
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param context         the context
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @param satSolverType   the to be used SAT solver
     * @return returns W if AC2 fulfilled, else null
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    private Set<Literal> fulfillsAC2(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                             Set<Literal> evaluation, SolvingStrategy solvingStrategy, SATSolverType satSolverType,
                             FormulaFactory f)
            throws InvalidCausalModelException {
        SATSolver satSolver;
        if (satSolverType == MINISAT) {
            satSolver = MiniSat.miniSat(f);
        } else {
            satSolver = MiniSat.glucose(f);
        }
        Formula phiFormula = f.not(phi); // negate phi

        // create copy of original causal model
        CausalModel causalModelModified = createModifiedCausalModelForCause(causalModel, cause, f);

        // evaluate causal model with setting x' for cause
        Set<Literal> evaluationModified = evaluateEquations(causalModelModified, context, f);
        // check if not(phi) evaluates to true for empty W -> if yes, no further investigation necessary
        if (phiFormula.evaluate(new Assignment(evaluationModified))) {
            return new HashSet<>();
        }

        if (solvingStrategy == SolvingStrategy.SAT) {
            return getWStandard(causalModelModified, phiFormula, cause, context, evaluation, satSolver, f);
        } else {
            return getWMinimal(causalModelModified, phiFormula, cause, context, evaluation, satSolver, f);
        }
    }

    /**
     * Checks AC1, AC2 and AC3 given a causal model, a cause, a context and phi and a solving strategy.
     *
     * @param causalModel     the underlying causel model
     * @param context         the context
     * @param phi             the phi
     * @param cause           the cause
     * @param solvingStrategy the applied solving strategy
     * @param satSolverType   the to be used SAT solver
     * @return for each AC, true if fulfilled, false else
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi,
                                Set<Literal> cause, SolvingStrategy solvingStrategy, SATSolverType satSolverType)
            throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Set<Literal> evaluation = CausalitySolver.evaluateEquations(causalModel, context, f);
        boolean ac1 = fulfillsAC1(evaluation, phi, cause);
        Set<Literal> w = fulfillsAC2(causalModel, phi, cause, context, evaluation, solvingStrategy, satSolverType, f);
        boolean ac2 = w != null;
        boolean ac3 = fulfillsAC3(causalModel, phi, cause, context, evaluation, solvingStrategy, f);
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
        return causalitySolverResult;
    }

    /**
     * Compute a not necessarily minimal W.
     *
     * @param causalModelModified causal model where the equations of the cause are replaced respectively
     * @param negatedPhi          negated phi
     * @param cause               the cause
     * @param context             the context
     * @param evaluation          the evaluation in the original causal model
     * @param satSolver           a SAT solver instance
     * @param f                   a formula factory instance
     * @return a set W if AC2 is fulfilled; null otherwise
     */
    private Set<Literal> getWStandard(CausalModel causalModelModified, Formula negatedPhi, Set<Literal> cause,
                                      Set<Literal> context, Set<Literal> evaluation, SATSolver satSolver,
                                      FormulaFactory f) {
        // generate SAT query
        Formula formula = generateSATQuery(causalModelModified, negatedPhi, cause, context, evaluation, f);
        // add query to solver
        satSolver.add(formula);
        if (satSolver.sat() == Tristate.TRUE) {
            // if satisfiable, get the assignment for which the formula is satisfiable
            Assignment assignment = satSolver.model();
            // generate (maximum) W
            Set<Literal> w = assignment.literals().stream()
                    .filter(l -> evaluation.contains(l)
                            && !causalModelModified.getExogenousVariables().contains(l.variable()))
                    .collect(Collectors.toSet());
            return w;
        } else {
            // if not satisfiable
            return null;
        }
    }

    /**
     * Computes a minimal W.
     *
     * @param causalModelModified causal model where the equations of the cause are replaced respectively
     * @param negatedPhi          negated phi
     * @param cause               the cause
     * @param context             the context
     * @param evaluation          the evaluation in the original causal model
     * @param satSolver           a SAT solver instance
     * @param f                   a formula factory instance
     * @return a set W if AC2 is fulfilled; null otherwise
     */
    private Set<Literal> getWMinimal(CausalModel causalModelModified, Formula negatedPhi, Set<Literal> cause,
                                     Set<Literal> context, Set<Literal> evaluation, SATSolver satSolver,
                                     FormulaFactory f) {
        // generate SAT query
        Formula formula = generateSATQuery(causalModelModified, negatedPhi, cause, context, evaluation, f);
        // add query to solver
        satSolver.add(formula);
        Set<Literal> w = null;
        if (satSolver.sat() == Tristate.TRUE) {
            Map<Variable, Equation> variableEquationMap = causalModelModified.getVariableEquationMap();
            // if satisfiable, get the assignments for which the formula is satisfiable
            List<Assignment> assignments = satSolver.enumerateAllModels();
            // loop through all satisfying assignments; the first one found might not expose a minimal W
            for (Assignment assignment : assignments) {
                /*
                 * we construct a set of literals that are possibly in W. This set is equal to the one constructed in
                 * the standard approach
                 * */
                Set<Literal> wCandidates = assignment.literals().stream()
                        .filter(l -> evaluation.contains(l)
                                && !causalModelModified.getExogenousVariables().contains(l.variable()))
                        .collect(Collectors.toSet());

                Set<Literal> newW = new HashSet<>();
                for (Literal wCandidate : wCandidates) {
                    // create an assignment instance where the current wCandidate is removed
                    Assignment assignmentNew = new Assignment(assignment.literals().stream()
                            .filter(l -> !l.variable().equals(wCandidate.variable())).collect(Collectors.toSet()));
                    // compute the value of the current wCandidate using its equation
                    boolean value = variableEquationMap.get(wCandidate.variable()).getFormula().evaluate(assignmentNew);
                    /*
                     * if the value of the satisfying assignment and the value computed from the equation are
                     * different, than we know that the current variable needs to be in W, since we need to keep it to
                     * its original value such that the formula can be satisfied. */
                    if (value != wCandidate.phase()) {
                        newW.add(wCandidate);
                    }
                }

                // update W only if it has not been set so far or if we have found a smaller W
                if (w == null || newW.size() < w.size()) {
                    w = newW;
                }
            }
        }

        return w;
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
    private Formula generateSATQuery(CausalModel causalModelModified, Formula notPhi, Set<Literal> cause,
                                     Set<Literal> context, Set<Literal> evaluation, FormulaFactory f) {
        // get all variables in cause
        Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
        // create map of variables and corresponding evaluation
        Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
                .collect(Collectors.toMap(Literal::variable, Function.identity()));
        // create formula: !phi AND context
        Formula formula = f.and(notPhi, f.and(context));
        for (Equation equation : causalModelModified.getEquations()) {
            // get value of variable in original iteration
            Literal originalValue = variableEvaluationMap.get(equation.getVariable());
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
