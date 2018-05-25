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
import org.logicng.util.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.SATSolverType.MINISAT;

class SATCausalitySolver extends CausalitySolver {
    /**
     * Overrides {@link CausalitySolver#solve(CausalModel, Set, Formula, Set, SolvingStrategy)}
     *
     * @param causalModel     the underlying causel model
     * @param context         the context
     * @param phi             the phi
     * @param cause           the cause
     * @param solvingStrategy the applied solving strategy
     * @return for each AC, true if fulfilled, false else
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    // TODO maybe we should make super.solve abstract as well
    @Override
    CausalitySolverResult solve(CausalModel causalModel, Set<Literal> context, Formula phi, Set<Literal> cause,
                                SolvingStrategy solvingStrategy) throws InvalidCausalModelException {
        return solve(causalModel, context, phi, cause, solvingStrategy, MINISAT);
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
        Set<Literal> w;
        boolean ac3;
        if (solvingStrategy == SolvingStrategy.SAT || solvingStrategy == SolvingStrategy.SAT_MINIMAL) {
            w = fulfillsAC2(causalModel, phi, cause, context, evaluation, solvingStrategy, satSolverType, f);
            ac3 = fulfillsAC3(causalModel, phi, cause, context, evaluation, solvingStrategy, satSolverType, f);
        } else {
            Pair<Set<Literal>, Boolean> ac2ac3 = fulfillsAC2AC3(causalModel, phi, cause, context, evaluation,
                    solvingStrategy, satSolverType, f);
            w = ac2ac3.first();
            ac3 = ac2ac3.second();
        }
        boolean ac2 = w != null;

        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(ac1, ac2, ac3, cause, w);
        return causalitySolverResult;
    }

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
                                     Set<Literal> evaluation, SolvingStrategy solvingStrategy,
                                     SATSolverType satSolverType, FormulaFactory f)
            throws InvalidCausalModelException {
        SATSolver satSolver = selectSATSolver(satSolverType, f);
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
     * Checks if AC3 is fulfilled and overrides
     * {@link CausalitySolver#fulfillsAC3(CausalModel, Formula, Set, Set, Set, SolvingStrategy, FormulaFactory)}.
     * Uses MINISAT as default.
     *
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param context         the context
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @param f               a formula factory
     * @return true if AC3 fulfilled, else false
     */
    @Override
    protected boolean fulfillsAC3(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                  Set<Literal> evaluation, SolvingStrategy solvingStrategy, FormulaFactory f) {
        return fulfillsAC3(causalModel, phi, cause, context, evaluation, solvingStrategy, MINISAT, f);
    }

    /**
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param context         the context
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @param satSolverType   the to be used SAT solver
     * @param f               a formula factory
     * @return true if AC3 fulfilled, else false
     */
    private boolean fulfillsAC3(CausalModel causalModel, Formula phi, Set<Literal> cause, Set<Literal> context,
                                Set<Literal> evaluation, SolvingStrategy solvingStrategy,
                                SATSolverType satSolverType, FormulaFactory f) {
        // if the cause has a size of one, i.e. a singleton-cause, then AC3 is fulfilled automatically
        if (cause.size() > 1) {
            // get specified SAT solver
            SATSolver satSolver = selectSATSolver(satSolverType, f);
            // negate phi
            Formula phiNegated = f.not(phi);
            // generate SAT query for AC3
            Formula formula = generateSATQuery(causalModel, phiNegated, cause, context, evaluation, true, f);
            // add query to solver
            satSolver.add(formula);
            // should be satisfiable, if cause fulfills AC2
            if (satSolver.sat() == Tristate.TRUE) {
                // create a set of Variables in the cause, i.e. map a set of Literals to Variables
                Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
                // create a map of variables in the cause and their actual value represented as literal
                Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
                        .filter(l -> causeVariables.contains(l.variable()))
                        .collect(Collectors.toMap(Literal::variable, Function.identity()));
                // get the assignments for which the formula is satisfiable
                List<Assignment> assignments = satSolver.enumerateAllModels();
                // loop through all satisfying assignments
                for (Assignment assignment : assignments) {
                    /*
                     * get the variables in the cause as literals such that we have their evaluation in the current
                     * satisfying assignment. We call them cause candidates as it is not sure if they are a necessary
                     * part of the cause. */
                    Set<Literal> causeCandidates = assignment.literals().stream()
                            .filter(l -> causeVariables.contains(l.variable())).collect(Collectors.toSet());
                    Set<Variable> notRequiredForCause = new HashSet<>();
                    // loop through all the cause candidates
                    for (Literal causeCandidate : causeCandidates) {
                        // create an assignment instance where the current cause candidate is removed
                        Assignment assignmentNew = new Assignment(assignment.literals().stream()
                                .filter(l -> !l.variable().equals(causeCandidate.variable()))
                                .collect(Collectors.toSet()));
                        // compute the value of the current cause candidate using its equation
                        boolean value = causalModel.getVariableEquationMap().get(causeCandidate.variable()).getFormula()
                                .evaluate(assignmentNew);
                        // TODO maybe we need to take W into account; is the current approach correct? -> test case?
                        /*
                         * For each cause candidate we now check whether it evaluates according to its equation or is
                         * in W. In this case, we found a part of the cause that is not necessarily required, because
                         * not(phi) is satisfied by a subset of the
                         * cause, as we do not necessarily need to negate the current cause candidate such that not
                         * (phi) is fulfilled. We collect all those variables to construct a new potential cause
                         * later on for which we check AC1. */
                        if (causeCandidate.phase() == value || causeCandidate.phase() == variableEvaluationMap
                                .get(causeCandidate.variable()).phase()) {
                            notRequiredForCause.add(causeCandidate.variable());
                        }
                    }

                    // construct a new potential cause by removing all the irrelevant variables
                    Set<Literal> causeNew = cause.stream().filter(l -> !notRequiredForCause.contains(l.variable()))
                            .collect(Collectors.toSet());
                    // if the new cause is smaller than the passed one and fulfills AC1, AC3 is not fulfilled
                    if (causeNew.size() < cause.size() && fulfillsAC1(evaluation, phi, causeNew)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Pair<Set<Literal>, Boolean> fulfillsAC2AC3(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                                  Set<Literal> context, Set<Literal> evaluation,
                                                  SolvingStrategy solvingStrategy, SATSolverType satSolverType,
                                                  FormulaFactory f) {
        // TODO
        return null;
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
        Formula formula = generateSATQuery(causalModelModified, negatedPhi, cause, context, evaluation, false, f);
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
        Formula formula = generateSATQuery(causalModelModified, negatedPhi, cause, context, evaluation, false, f);
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

                if (newW.size() == 1) {
                    // if we have found a W of size 1, it cannot get smaller and we can directly return it
                    return newW;
                } else if (w == null || newW.size() < w.size()) {
                    // update W only if it has not been set so far or if we have found a smaller W
                    w = newW;
                }
            }
        }

        return w;
    }

    /**
     * Generates a formula whose satisfiability indicates whether AC2 is fulfilled or not.
     *
     * @param causalModel the causal model
     * @param notPhi      the negated phi
     * @param cause       the cause
     * @param context     the context
     * @param evaluation  the original evaluation under the given context
     * @param ac3         set to true if used within AC3 check
     * @param f           a formula factory
     * @return a formula
     */
    private Formula generateSATQuery(CausalModel causalModel, Formula notPhi, Set<Literal> cause,
                                     Set<Literal> context, Set<Literal> evaluation, boolean ac3, FormulaFactory f) {
        // get all variables in cause
        Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
        // create map of variables and corresponding evaluation
        Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
                .collect(Collectors.toMap(Literal::variable, Function.identity()));
        // create formula: !phi AND context
        Formula formula = f.and(notPhi, f.and(context));

        if (!ac3) {
            for (Equation equation : causalModel.getEquations()) {
                // get value of variable in original iteration
                Literal originalValue = variableEvaluationMap.get(equation.getVariable());
                /*
                 * create formula: V_originalValue OR (V <=> Formula_V)
                 * if the variable of the current equation is in the cause, then we do not allow for its original value
                 * and just add (V <=> Formula_V).*/
                Formula equationFormula = causeVariables.contains(equation.getVariable()) ?
                        f.equivalence(equation.getVariable(), equation.getFormula()) :
                        f.or(originalValue, f.equivalence(equation.getVariable(), equation.getFormula()));
                // add created formula to global formula by AND
                formula = f.and(formula, equationFormula);
            }
        } else {
            // create dummy variable
            Variable dummy = f.variable("_dummy");
            for (Equation equation : causalModel.getEquations()) {
                // get value of variable in original iteration
                Literal originalValue = variableEvaluationMap.get(equation.getVariable());
                Formula equationFormula;
                /*
                 * When generating a SAT query for AC3, then for each variable not in the cause, we stick to the same
                 * scheme as for AC2, i.e. (V_originalValue OR (V <=> Formula_V)). */
                if (!causeVariables.contains(equation.getVariable())) {
                    equationFormula = f.or(originalValue, f.equivalence(equation.getVariable(), equation.getFormula()));
                }
                /*
                 * If however the variable of the current equation in in the cause, we additionally add an OR with its
                 * negation. That is, we allow its original value, the negation of this original value and the
                 * equivalence with its equation. The resulting formula would be
                 * (V_originalValue OR (V <=> Formula_V) OR not(V_originalValue)). Obviously, we could replace that
                 * with TRUE or at least simplify it to (V_originalValue OR not(V_originalValue)). However, when
                 * replacing it by TRUE, we might run into the problem that some variables are removed completely
                 * from the formula which causes problem with the evaluation of some equations later on.
                 * Therefore, we want to keep at least (V_originalValue OR not(V_originalValue)). Unfortunately,
                 * LogicNG automatically replaces this formula by TRUE. To avoid this, we introduce a dummy variable
                 * as follows: (V_originalValue OR (not(V_originalValue) AND dummy))
                 * The dummy variable has no effect on the final result. */
                else {
                    equationFormula = f.or(originalValue, f.and(originalValue.negate(), dummy));
                }
                // add created formula to global formula by AND
                formula = f.and(formula, equationFormula);
            }

        }
        return formula;
    }

    /**
     * Return a SAT solver instance depending on the given type.
     *
     * @param satSolverType the SAT solver type
     * @param f             a formula factory
     * @return a SAT solver instance
     */
    private SATSolver selectSATSolver(SATSolverType satSolverType, FormulaFactory f) {
        if (satSolverType == MINISAT) {
            return MiniSat.miniSat(f);
        } else {
            return MiniSat.glucose(f);
        }
    }
}
