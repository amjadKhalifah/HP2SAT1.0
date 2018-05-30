package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.exceptions.InvalidCauseException;
import de.tum.in.i4.hp2sat.exceptions.InvalidContextException;
import de.tum.in.i4.hp2sat.exceptions.InvalidPhiException;
import de.tum.in.i4.hp2sat.util.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static de.tum.in.i4.hp2sat.causality.SATSolverType.GLUCOSE;
import static de.tum.in.i4.hp2sat.causality.SATSolverType.MINISAT;
import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.EVAL;
import static de.tum.in.i4.hp2sat.causality.SolvingStrategy.SAT;
import static org.junit.Assert.assertEquals;

public class CausalModelTest {
    FormulaFactory f;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
    }

    @Test
    public void Should_CreateCausalModel_When_EverythingFine() throws InvalidCausalModelException {
        ExampleProvider.billySuzy();
    }

    @Test
    public void Should_NotThrowException_When_EverythingFine() throws InvalidCausalModelException {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable cExo = f.variable("c");

        Equation equationA = new Equation(a, b);
        Equation equationB = new Equation(b, cExo);

        CausalModel causalModel = new CausalModel(null, new HashSet<>(Arrays.asList(equationA, equationB)),
                new HashSet<>(Collections.singletonList(cExo)));
    }

    @Test(expected = InvalidCausalModelException.class)
    public void Should_ThrowException_When_TwoEquationsDefineSameVariable() throws InvalidCausalModelException {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable cExo = f.variable("c");

        Equation equationA1 = new Equation(a, b);
        Equation equationA2 = new Equation(a, cExo);

        CausalModel causalModel = new CausalModel(null, new HashSet<>(Arrays.asList(equationA1, equationA2)),
                new HashSet<>(Collections.singletonList(cExo)));
    }

    @Test(expected = InvalidCausalModelException.class)
    public void Should_ThrowException_When_VariableIsDefinedByEquationAndExogenous()
            throws InvalidCausalModelException {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable cExo = f.variable("c");

        Equation equationA = new Equation(a, b);
        Equation equationB = new Equation(b, cExo);

        CausalModel causalModel = new CausalModel(null, new HashSet<>(Arrays.asList(equationA, equationB)),
                new HashSet<>(Arrays.asList(b, cExo)));
    }

    @Test(expected = InvalidCausalModelException.class)
    public void Should_ThrowException_When_ExogenousVariableCalledLikeDummy()
            throws InvalidCausalModelException {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable cExo = f.variable(SATCausalitySolver.DUMMY_VAR_NAME);

        Equation equationA = new Equation(a, b);
        Equation equationB = new Equation(b, cExo);

        CausalModel causalModel = new CausalModel(null, new HashSet<>(Arrays.asList(equationA, equationB)),
                new HashSet<>(Collections.singletonList(cExo)));
    }

    @Test(expected = InvalidCausalModelException.class)
    public void Should_BeInvalid_When_NotEachVariableDefinedByEquation() throws InvalidCausalModelException {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable c = f.variable("c");

        Equation equationA = new Equation(a, b);
        Equation equationB = new Equation(b, c);

        CausalModel causalModel = new CausalModel(null, new HashSet<>(Arrays.asList(equationA, equationB)),
                new HashSet<>());
    }

    @Test(expected = InvalidCausalModelException.class)
    public void Should_BeValid_When_CircularDependency1() throws InvalidCausalModelException {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable cExo = f.variable("c");
        Variable d = f.variable("d");

        Equation equationA = new Equation(a, b);
        Equation equationB = new Equation(b, d);
        Equation equationD = new Equation(d, a); // circular: a -> b -> d -> a

        CausalModel causalModel = new CausalModel(null,
                new HashSet<>(Arrays.asList(equationA, equationB, equationD)),
                new HashSet<>(Collections.singletonList(cExo)));
    }

    @Test(expected = InvalidCausalModelException.class)
    public void Should_BeValid_When_CircularDependency2() throws InvalidCausalModelException {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable cExo = f.variable("c");
        Variable d = f.variable("d");

        Equation equationA = new Equation(a, f.and(b, a)); // circular: a -> b,a
        Equation equationB = new Equation(b, d);
        Equation equationD = new Equation(d, cExo);

        CausalModel causalModel = new CausalModel(null,
                new HashSet<>(Arrays.asList(equationA, equationB, equationD)),
                new HashSet<>(Collections.singletonList(cExo)));
    }

    @Test
    public void Should_NotThrowException_When_ContextCausePhiFine() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");
        billySuzy.isCause(context, phi, cause, SolvingStrategy.EVAL);
    }

    @Test
    public void Should_FulfillAllACs_When_STIsCauseBS() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Formula phi = f.variable("BS");

        CausalitySolverResult causalitySolverResultExpectedEval =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Collections.singletonList(f.literal("BH", false))));
        CausalitySolverResult causalitySolverResultExpectedSAT =
                new CausalitySolverResult(true, true, true, cause,
                        new HashSet<>(Arrays.asList(f.variable("BT"), f.literal("BH", false))));
        assertEquals(causalitySolverResultExpectedEval, billySuzy.isCause(context, phi, cause, EVAL));
        assertEquals(causalitySolverResultExpectedSAT, billySuzy.isCause(context, phi, cause, SAT, MINISAT));
        assertEquals(causalitySolverResultExpectedSAT, billySuzy.isCause(context, phi, cause, SAT, GLUCOSE));
    }

    @Test(expected = InvalidContextException.class)
    public void Should_ThrowException_When_ContextIncomplete() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Collections.singletonList(f.literal("BT_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");
        billySuzy.isCause(context, phi, cause, SolvingStrategy.EVAL);
    }

    @Test(expected = InvalidPhiException.class)
    public void Should_NotThrowException_When_PhiContainsInvalidVariable() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("ST_exo");
        billySuzy.isCause(context, phi, cause, SolvingStrategy.EVAL);
    }

    @Test(expected = InvalidContextException.class)
    public void Should_ThrowException_When_ContextContainsNonExogenousVariable() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("BT", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Formula phi = f.variable("BS");
        billySuzy.isCause(context, phi, cause, SolvingStrategy.EVAL);
    }

    @Test(expected = InvalidCauseException.class)
    public void Should_ThrowException_When_CauseContainsInvalidVariable() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("AnInvalidVar")));
        Formula phi = f.variable("BS");
        billySuzy.isCause(context, phi, cause, SolvingStrategy.EVAL);
    }

    @Test(expected = InvalidCauseException.class)
    public void Should_ThrowException_When_CauseIsEmpty() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Set<Literal> context = new HashSet<>(Arrays.asList(
                f.literal("BT_exo", true), f.literal("ST_exo", true)));
        Set<Literal> cause = new HashSet<>();
        Formula phi = f.variable("BS");
        billySuzy.isCause(context, phi, cause, SolvingStrategy.EVAL);
    }
}