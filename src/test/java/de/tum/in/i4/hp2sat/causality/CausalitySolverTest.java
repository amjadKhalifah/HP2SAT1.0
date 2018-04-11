package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.testutil.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Constant;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;

import static org.junit.Assert.*;

public class CausalitySolverTest {
    FormulaFactory f;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
    }

    @Test
    public void Should_ReturnUndef_When_AsLongAsMethodIncomplete() throws Exception {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.verum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));
        Set<Variable> w = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Tristate result = CausalitySolver.solve(billySuzy, context, phi, cause, w);
        assertEquals(Tristate.UNDEF, result);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInBillySuzy_When_BillyAndSuzyThrow() throws Exception{
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.verum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));
        Set<Variable> w = new HashSet<>(Collections.singletonList(f.variable("SH")));

        Map<Variable, Boolean> evaluationExpected = new HashMap<>();
        evaluationExpected.put(f.variable("BT_exo"), true);
        evaluationExpected.put(f.variable("ST_exo"), true);
        evaluationExpected.put(f.variable("BT"), true);
        evaluationExpected.put(f.variable("ST"), true);
        evaluationExpected.put(f.variable("BH"), false);
        evaluationExpected.put(f.variable("SH"), true);
        evaluationExpected.put(f.variable("BS"), true);

        Map<Variable, Boolean> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInBillySuzy_When_SuzyThrowsOnly() throws Exception{
        CausalModel billySuzy = ExampleProvider.billySuzy();
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("BT_exo"), f.falsum());
        context.put(f.variable("ST_exo"), f.verum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("BS")));
        Set<Variable> w = new HashSet<>(Collections.singletonList(f.variable("SH")));

        Map<Variable, Boolean> evaluationExpected = new HashMap<>();
        evaluationExpected.put(f.variable("BT_exo"), false);
        evaluationExpected.put(f.variable("ST_exo"), true);
        evaluationExpected.put(f.variable("BT"), false);
        evaluationExpected.put(f.variable("ST"), true);
        evaluationExpected.put(f.variable("BH"), false);
        evaluationExpected.put(f.variable("SH"), true);
        evaluationExpected.put(f.variable("BS"), true);

        Map<Variable, Boolean> evaluationActual = CausalitySolver.evaluateEquations(billySuzy, context);

        assertEquals(evaluationExpected, evaluationActual);
    }

    @Test
    public void Should_ReturnEvaluationForEquationsInArsonistsDisjunctive_When_LightningOnly() throws Exception{
        CausalModel arsonists = ExampleProvider.arsonists(true);
        Map<Variable, Constant> context = new HashMap<>();
        context.put(f.variable("L_exo"), f.verum());
        context.put(f.variable("MD_exo"), f.falsum());
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("MD")));
        Set<Literal> phi = new HashSet<>(Collections.singletonList(f.variable("FF")));
        Set<Variable> w = new HashSet<>();

        Map<Variable, Boolean> evaluationExpected = new HashMap<>();
        evaluationExpected.put(f.variable("L_exo"), true);
        evaluationExpected.put(f.variable("MD_exo"), false);
        evaluationExpected.put(f.variable("L"), true);
        evaluationExpected.put(f.variable("MD"), false);
        evaluationExpected.put(f.variable("FF"), true);

        Map<Variable, Boolean> evaluationActual = CausalitySolver.evaluateEquations(arsonists, context);

        assertEquals(evaluationExpected, evaluationActual);
    }
}