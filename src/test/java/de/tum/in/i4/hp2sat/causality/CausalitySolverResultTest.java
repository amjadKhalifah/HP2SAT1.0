package de.tum.in.i4.hp2sat.causality;

import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import de.tum.in.i4.hp2sat.causality.CausalitySolverResult;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class CausalitySolverResultTest {
    FormulaFactory f;

    Literal a;
    Literal b;
    Literal c;
    Literal d;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
        a = f.literal("A", true);
        b = f.literal("B", false);
        c = f.literal("C", false);
        d = f.literal("D", false);
    }

    @Test
    public void Should_ReturnResponsibilityZero_When_NotAllACsFulfilled() {
        CausalitySolverResult causalitySolverResult1 = new CausalitySolverResult(false, false, false,
                new HashSet<>(Collections.singletonList(a)), new HashSet<>());
        Map<Literal, Double> responsibility1 = new HashMap<>();
        responsibility1.put(a, 0D);
        assertEquals(responsibility1, causalitySolverResult1.getResponsibility());

        CausalitySolverResult causalitySolverResult2 = new CausalitySolverResult(true, false, false,
                new HashSet<>(Arrays.asList(a, b)), new HashSet<>());
        Map<Literal, Double> responsibility2 = new HashMap<>();
        responsibility2.put(a, 0D);
        responsibility2.put(b, 0D);
        assertEquals(responsibility2, causalitySolverResult2.getResponsibility());
    }

    @Test
    public void Should_ReturnEmptyResponsibility_IfEmptyCause() {
        CausalitySolverResult causalitySolverResult = new CausalitySolverResult(false, false, false,
                new HashSet<>(), new HashSet<>());
        Map<Literal, Double> responsibility = new HashMap<>();
        assertEquals(responsibility, causalitySolverResult.getResponsibility());
    }

    @Test
    public void Should_ReturnCorrectResponsibility_IfACsFulfilled() {
        CausalitySolverResult causalitySolverResult1 = new CausalitySolverResult(true, true, true,
                new HashSet<>(Collections.singletonList(a)), new HashSet<>());
        Map<Literal, Double> responsibility1 = new HashMap<>();
        responsibility1.put(a, 1D);
        assertEquals(responsibility1, causalitySolverResult1.getResponsibility());

        CausalitySolverResult causalitySolverResult2 = new CausalitySolverResult(true, true, true,
                new HashSet<>(Arrays.asList(a, b)), new HashSet<>());
        Map<Literal, Double> responsibility2 = new HashMap<>();
        responsibility2.put(a, 0.5);
        responsibility2.put(b, 0.5);
        assertEquals(responsibility2, causalitySolverResult2.getResponsibility());

        CausalitySolverResult causalitySolverResult3 = new CausalitySolverResult(true, true, true,
                new HashSet<>(Arrays.asList(a, b)), new HashSet<>(Collections.singletonList(c)));
        Map<Literal, Double> responsibility3 = new HashMap<>();
        responsibility3.put(a, 1D / 3D);
        responsibility3.put(b, 1D / 3D);
        assertEquals(responsibility3, causalitySolverResult3.getResponsibility());

        CausalitySolverResult causalitySolverResult4 = new CausalitySolverResult(true, true, true,
                new HashSet<>(Arrays.asList(a, b)), new HashSet<>(Arrays.asList(c, d)));
        Map<Literal, Double> responsibility4 = new HashMap<>();
        responsibility4.put(a, 0.25);
        responsibility4.put(b, 0.25);
        assertEquals(responsibility4, causalitySolverResult4.getResponsibility());

        CausalitySolverResult causalitySolverResult5 = new CausalitySolverResult(true, true, true,
                new HashSet<>(Arrays.asList(a, b)), null);
        Map<Literal, Double> responsibility5 = new HashMap<>();
        responsibility5.put(a, 0.5);
        responsibility5.put(b, 0.5);
        assertEquals(responsibility5, causalitySolverResult5.getResponsibility());

        CausalitySolverResult causalitySolverResult6 = new CausalitySolverResult(true, true, true,
                new HashSet<>(Collections.singletonList(a)), new HashSet<>(Collections.singletonList(c)));
        Map<Literal, Double> responsibility6 = new HashMap<>();
        responsibility6.put(a, 0.5);
        assertEquals(responsibility6, causalitySolverResult6.getResponsibility());
    }
}