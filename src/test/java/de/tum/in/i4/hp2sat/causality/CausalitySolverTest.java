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
}