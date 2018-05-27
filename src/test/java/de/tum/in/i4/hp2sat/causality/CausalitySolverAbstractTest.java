package de.tum.in.i4.hp2sat.causality;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.util.ExampleProvider;
import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class CausalitySolverAbstractTest {
    FormulaFactory f;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSTIsCause() throws InvalidCausalModelException {
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"), f.variable("SH"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(ExampleProvider.billySuzy(), cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenNotSTIsCause() throws InvalidCausalModelException {
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("ST", false)));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"), f.variable("SH"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(ExampleProvider.billySuzy(), cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenBTIsCause() throws InvalidCausalModelException {
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(ExampleProvider.billySuzy(), cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSTAndBTIsCause() throws InvalidCausalModelException {
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"), f.variable("SH"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(ExampleProvider.billySuzy(), cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSHIsCause() throws InvalidCausalModelException {
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(ExampleProvider.billySuzy(), cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSHAndBHIsCause() throws InvalidCausalModelException {
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("SH"), f.variable("BH")));
        Set<Variable> wVariablesExpected = new HashSet<>(Collections.singletonList(f.variable("BS")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(ExampleProvider.billySuzy(), cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForDummy_GivenCIsCause() throws InvalidCausalModelException {
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("G"), f.variable("F"),
                f.variable("H")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(ExampleProvider.dummy(), cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }
}