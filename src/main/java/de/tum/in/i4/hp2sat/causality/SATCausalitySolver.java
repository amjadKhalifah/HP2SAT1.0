package de.tum.in.i4.hp2sat.causality;

import com.google.errorprone.annotations.Var;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tum.in.i4.hp2sat.causality.SATSolverType.MINISAT;
import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.*;

class SATCausalitySolver extends CausalitySolver {
    static final String DUMMY_VAR_NAME = "_dummy";

    /**
     * Overrides {@link CausalitySolver#solve(CausalModel, Set, Formula, Set, SolvingStrategy)}.
     * Default SATSolver: MINISAT
     *
     * @param causalModel     the underlying causel model
     * @param context         the context
     * @param phi             the phi
     * @param cause           the cause
     * @param solvingStrategy the applied solving strategy
     * @return for each AC, true if fulfilled, false else
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
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
        if (Arrays.asList(SAT, SAT_MINIMAL, SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_CLAUSES,
                SAT_OPTIMIZED_CLAUSES_MINIMAL).contains(solvingStrategy)) {
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
        Formula negatedPhi = f.not(phi); // negate phi

        // create copy of original causal model
        CausalModel causalModelModified = createModifiedCausalModelForCause(causalModel, cause, f);

        // evaluate causal model with setting x' for cause
        Set<Literal> evaluationModified = evaluateEquations(causalModelModified, context, f);
        // check if not(phi) evaluates to true for empty W -> if yes, no further investigation necessary
        if (negatedPhi.evaluate(new Assignment(evaluationModified))) {
            return new HashSet<>();
        }

        // generate SAT query
        Formula formula = generateSATQuery(causalModelModified, negatedPhi, cause, context, evaluation,
                solvingStrategy, false, f);
        satSolver.add(formula);
        if (satSolver.sat() == Tristate.TRUE) {
            if (Arrays.asList(SAT, SAT_OPTIMIZED_W, SAT_OPTIMIZED_CLAUSES).contains(solvingStrategy)) {
                // if satisfiable, get the assignment for which the formula is satisfiable
                Assignment assignment = satSolver.model();
                return getWStandard(causalModelModified, evaluation, assignment);
            } else {
                // if satisfiable, get the assignments for which the formula is satisfiable
                List<Assignment> assignments = satSolver.enumerateAllModels();
                return getWMinimal(causalModelModified, evaluation, assignments);
            }
        } else {
            return null;
        }
    }

    /**
     * Checks if AC3 is fulfilled.
     *
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
            Formula formula = generateSATQuery(causalModel, phiNegated, cause, context, evaluation, solvingStrategy,
                    true, f);
            // add query to solver
            satSolver.add(formula);
            // should be satisfiable, if cause fulfills AC2
            if (satSolver.sat() == Tristate.TRUE) {
                // get the assignments for which the formula is satisfiable
                List<Assignment> assignments = satSolver.enumerateAllModels().stream()
                        .filter(a -> a.literals().contains(f.variable(DUMMY_VAR_NAME))).collect(Collectors.toList());
                return fulfillsAC3Helper(causalModel, phi, cause, evaluation, assignments);
            }
        }
        return true;
    }

    /**
     * Helper method used in the AC3 check as well as the combined approach. Checks if AC3 holds.
     *
     * @param causalModel the underlying causal model
     * @param phi         the phi
     * @param cause       the cause for which we check AC2
     * @param evaluation  the original evaluation of variables
     * @param assignments a list of satisfying assignments
     * @return true if AC3 holds, else false
     */
    private boolean fulfillsAC3Helper(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                      Set<Literal> evaluation, List<Assignment> assignments) {
        // create a set of Variables in the cause, i.e. map a set of Literals to Variables
        Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
        // create a map of variables in the cause and their actual value represented as literal
        Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
                .filter(l -> causeVariables.contains(l.variable()))
                .collect(Collectors.toMap(Literal::variable, Function.identity()));
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
        return true;
    }

    /**
     * Checks if AC2 and AC3 are fulfilled. Combined approach that takes advantage of synergies between the separate
     * approaches.
     *
     * @param causalModel     the underlying causal model
     * @param phi             the phi
     * @param cause           the cause for which we check AC2
     * @param context         the context
     * @param evaluation      the original evaluation of variables
     * @param solvingStrategy the solving strategy
     * @param satSolverType   the to be used SAT solver
     * @param f               a formula factory
     * @return a tuple of set W and a boolean value indicating whether AC3 is fulfilled or not
     * @throws InvalidCausalModelException thrown if internally generated causal models are invalid
     */
    private Pair<Set<Literal>, Boolean> fulfillsAC2AC3(CausalModel causalModel, Formula phi, Set<Literal> cause,
                                                       Set<Literal> context, Set<Literal> evaluation,
                                                       SolvingStrategy solvingStrategy, SATSolverType satSolverType,
                                                       FormulaFactory f) throws InvalidCausalModelException {
        Set<Literal> w;
        boolean ac3;
        // if the cause is of size 1, then AC3 is fulfilled automatically. Hence, we just need to check for AC2
        if (cause.size() == 1) {
            // set new solving strategy
            SolvingStrategy solvingStrategyNew;
            if (solvingStrategy == SolvingStrategy.SAT_COMBINED) {
                solvingStrategyNew = SolvingStrategy.SAT;
            } else {
                solvingStrategyNew = SolvingStrategy.SAT_MINIMAL;
            }
            w = fulfillsAC2(causalModel, phi, cause, context, evaluation, solvingStrategyNew, satSolverType, f);
            // ac3 is true if cause has size 1
            ac3 = true;
        } else {
            // negate phi
            Formula phiNegated = f.not(phi);
            // create copy of original causal model
            CausalModel causalModelModified = createModifiedCausalModelForCause(causalModel, cause, f);
            // evaluate causal model with setting x' for cause
            Set<Literal> evaluationModified = evaluateEquations(causalModelModified, context, f);
            // check if not(phi) evaluates to true for empty W
            if (phiNegated.evaluate(new Assignment(evaluationModified))) {
                w = new HashSet<>();
                // perform a normal AC3 check
                ac3 = fulfillsAC3(causalModel, phi, cause, context, evaluation, solvingStrategy, satSolverType, f);
            } else {
                // get specified SAT solver
                SATSolver satSolver = selectSATSolver(satSolverType, f);
                // generate SAT query for AC3 as this SAT query contains also the satisfying assignments for AC2
                Formula formula = generateSATQuery(causalModel, phiNegated, cause, context, evaluation,
                        solvingStrategy, true, f);
                // add query to solver
                satSolver.add(formula);
                if (satSolver.sat() == Tristate.TRUE) {
                    // flip/negate the cause
                    Set<Literal> causeNegated = cause.stream().map(Literal::negate).collect(Collectors.toSet());
                    // get all satisfying assignments
                    List<Assignment> assignments = satSolver.enumerateAllModels().stream()
                            .filter(a -> a.literals().contains(f.variable(DUMMY_VAR_NAME)))
                            .collect(Collectors.toList());

                    if (solvingStrategy == SAT_COMBINED) {
                        /*
                         * the SAT query might contain satisfying assignment that are not relevant for AC2. We therefore
                         * filter the assignment in which the cause variables are flipped as specified. */
                        Assignment assignmentForAC2 = assignments.stream()
                                .filter(a -> a.literals().containsAll(causeNegated)).findFirst().orElse(null);
                        if (assignmentForAC2 != null) {
                            w = getWStandard(causalModel, evaluation, assignmentForAC2);
                        } else {
                            w = null;
                        }
                    } else {
                        // SAT_COMBINED_MINIMAL
                        /*
                         * the SAT query might contain satisfying assignment that are not relevant for AC2. We therefore
                         * filter all those assignments in which the cause variables are flipped as specified. */
                        List<Assignment> assignmentsForAC2 = assignments.stream()
                                .filter(a -> a.literals().containsAll(causeNegated)).collect(Collectors.toList());
                        if (assignmentsForAC2.size() > 0) {
                            w = getWMinimal(causalModel, evaluation, assignmentsForAC2);
                        } else {
                            w = null;
                        }
                    }
                    // check if AC3 holds
                    ac3 = fulfillsAC3Helper(causalModel, phi, cause, evaluation, assignments);
                } else {
                    // if the SAT query is not satisfiable at all, then both AC2 and AC3 do not hold
                    w = null;
                    ac3 = true;
                }
            }
        }
        return new Pair<>(w, ac3);
    }

    /**
     * Compute a not necessarily minimal W.
     *
     * @param causalModelModified causal model where the equations of the cause are replaced respectively
     * @param evaluation          the evaluation in the original causal model
     * @param assignment          a satisfying assignment
     * @return a set W if AC2 is fulfilled; null otherwise
     */
    private Set<Literal> getWStandard(CausalModel causalModelModified, Set<Literal> evaluation, Assignment assignment) {
        // generate (maximum) W
        Set<Literal> w = assignment.literals().stream()
                .filter(l -> evaluation.contains(l)
                        && !causalModelModified.getExogenousVariables().contains(l.variable()))
                .collect(Collectors.toSet());
        return w;
    }

    /**
     * Computes a minimal W.
     *
     * @param causalModelModified causal model where the equations of the cause are replaced respectively
     * @param evaluation          the evaluation in the original causal model
     * @param assignments         list of satisfying assignments
     * @return a set W if AC2 is fulfilled; null otherwise
     */
    private Set<Literal> getWMinimal(CausalModel causalModelModified, Set<Literal> evaluation,
                                     List<Assignment> assignments) {
        Set<Literal> w = null;
        Map<Variable, Equation> variableEquationMap = causalModelModified.getVariableEquationMap();
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
                                     Set<Literal> context, Set<Literal> evaluation, SolvingStrategy solvingStrategy,
                                     boolean ac3, FormulaFactory f) {
        // get all variables in cause
        Set<Variable> causeVariables = cause.stream().map(Literal::variable).collect(Collectors.toSet());
        // create map of variables and corresponding evaluation
        Map<Variable, Literal> variableEvaluationMap = evaluation.stream()
                .collect(Collectors.toMap(Literal::variable, Function.identity()));
        // create formula: !phi AND context
        Formula formula = f.and(notPhi, f.and(context));

        Set<Variable> wVariablesOptimized = null;
        if (solvingStrategy == SAT_OPTIMIZED_W || solvingStrategy == SAT_OPTIMIZED_W_MINIMAL) {
            wVariablesOptimized = CausalitySolver.getMinimalWVariables(causalModel, notPhi, cause, f);
        }
        Set<Variable> variablesAffectingPhi = null;
        if (solvingStrategy == SAT_OPTIMIZED_CLAUSES || solvingStrategy == SAT_OPTIMIZED_CLAUSES_MINIMAL) {
            variablesAffectingPhi = CausalitySolver.getReachableVariables(causalModel.getGraphReversed(),
                    notPhi.literals(), f);
        }

        if (!ac3) {
            for (Equation equation : causalModel.getVariableEquationMap().values()) {
                /*
                 * create formula: V_originalValue OR (V <=> Formula_V)
                 *
                 * OPTIMZED_W Strategy: if the variable of the current equation is in the cause or not in the set of
                 * optimized variables, then we do not allow for its original value and just add (V <=> Formula_V).
                 *
                 * OPTIMIZED_CLAUSES Strategy: if the variable of the current equation is in the cause or not in the
                 * set of variables that affect phi, we just add TRUE to the formula
                 * */
                Formula equationFormula;
                if (causeVariables.contains(equation.getVariable()) ||
                        ((solvingStrategy == SAT_OPTIMIZED_W || solvingStrategy == SAT_OPTIMIZED_W_MINIMAL) &&
                                !wVariablesOptimized.contains(equation.getVariable()))) {
                    equationFormula = f.equivalence(equation.getVariable(), equation.getFormula());
                } else if ((solvingStrategy == SAT_OPTIMIZED_CLAUSES
                        || solvingStrategy == SAT_OPTIMIZED_CLAUSES_MINIMAL)
                        && !variablesAffectingPhi.contains(equation.getVariable())) {
                    equationFormula = f.verum();
                } else {
                    // get value of variable in original iteration
                    Literal originalValue = variableEvaluationMap.get(equation.getVariable());
                    equationFormula = f.or(originalValue, f.equivalence(equation.getVariable(), equation.getFormula()));
                }

                // add created formula to global formula by AND
                formula = f.and(formula, equationFormula);
            }
        } else {
            // create dummy variable
            Variable dummy = f.variable(DUMMY_VAR_NAME);
            for (Equation equation : causalModel.getVariableEquationMap().values()) {
                // get value of variable in original iteration
                Literal originalValue = variableEvaluationMap.get(equation.getVariable());
                Formula equationFormula;
                /*
                 * When generating a SAT query for AC3, then for each variable not in the cause, we stick to the same
                 * scheme as for AC2, i.e. (V_originalValue OR (V <=> Formula_V)).
                 *
                 * OPTIMZED_W Strategy: if the variable of the current equation is in the cause or not in the set of
                 * optimized variables, then we do not allow for its original value and just add (V <=> Formula_V).
                 *
                 * OPTIMIZED_CLAUSES Strategy: if the variable of the current equation is in the cause or not in the
                 * set of variables that affect phi, we just add TRUE to the formula*/
                if (!causeVariables.contains(equation.getVariable()) &&
                        ((solvingStrategy == SAT_OPTIMIZED_W || solvingStrategy == SAT_OPTIMIZED_W_MINIMAL) &&
                                !wVariablesOptimized.contains(equation.getVariable()))) {
                    equationFormula = f.equivalence(equation.getVariable(), equation.getFormula());
                } else if (!causeVariables.contains(equation.getVariable()) &&
                        (solvingStrategy == SAT_OPTIMIZED_CLAUSES || solvingStrategy == SAT_OPTIMIZED_CLAUSES_MINIMAL)
                        && !variablesAffectingPhi.contains(equation.getVariable())) {
                    equationFormula = f.verum();
                } else if (!causeVariables.contains(equation.getVariable())) {
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
