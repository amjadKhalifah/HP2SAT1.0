package de.tum.in.i4.hp2sat.causality;

import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import de.tum.in.i4.hp2sat.causality.CausalModel;
import de.tum.in.i4.hp2sat.causality.CausalitySolver;
import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.util.ExampleProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CausalitySolverAbstractTest {
    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSTIsCause_BS() throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("BS");
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"), f.variable("SH"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenNotSTIsCause_BS() throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("BS");
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("ST", false)));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"), f.variable("SH"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenBTIsCause_BS() throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("BS");
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("BT")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSTAndBTIsCause_BS()
            throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("BS");
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("BT")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"), f.variable("SH"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSHIsCause_BS() throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("BS");
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSHAndBHIsCause_BS()
            throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("BS");
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("SH"), f.variable("BH")));
        Set<Variable> wVariablesExpected = new HashSet<>(Collections.singletonList(f.variable("BS")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSTAndSHIsCause_BS()
            throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("BS");
        Set<Literal> cause = new HashSet<>(Arrays.asList(f.variable("ST"), f.variable("SH")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("BS"),
                f.variable("BH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSTIsCause_SH()
            throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.variable("SH");
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Set<Variable> wVariablesExpected = new HashSet<>(Collections.singletonList(f.variable("SH")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForBillySuzy_GivenSTIsCause_SHAndBS()
            throws InvalidCausalModelException {
        CausalModel billySuzy = ExampleProvider.billySuzy();
        FormulaFactory f = billySuzy.getFormulaFactory();
        Formula phi = f.and(f.variable("SH"), f.variable("BS"));
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("SH"), f.variable("BH"),
                f.variable("BS")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(billySuzy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }

    @Test
    public void Should_ReturnCorrectWVariables_ForDummy_GivenCIsCause_F() throws InvalidCausalModelException {
        CausalModel dummy = ExampleProvider.dummy();
        FormulaFactory f = dummy.getFormulaFactory();
        Formula phi = f.variable("F");
        Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("C")));
        Set<Variable> wVariablesExpected = new HashSet<>(Arrays.asList(f.variable("G"), f.variable("F"),
                f.variable("H")));
        Set<Variable> wVariablesActual = CausalitySolver.getMinimalWVariables(dummy, phi, cause, f);
        assertEquals(wVariablesExpected, wVariablesActual);
    }
}