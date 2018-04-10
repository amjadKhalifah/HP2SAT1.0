package de.in.tum.i4.hp2sat.modeling;

import org.junit.Before;
import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class CausalModelTest {
    FormulaFactory f;

    @Before
    public void setUp() throws Exception {
        f = new FormulaFactory();
    }

    @Test
    public void Should_CreateCausalModel_When_EverythingFine() {
        Variable BTExo = f.variable("BT_exo");
        Variable STExo = f.variable("ST_exo");

        Variable BT = f.variable("BT");
        Variable ST = f.variable("ST");
        Variable BH = f.variable("BH");
        Variable SH = f.variable("SH");
        Variable BS = f.variable("BS");

        Formula BTFormula = BTExo;
        Formula STFormula = STExo;
        Formula SHFormula = ST;
        Formula BHFormula = f.and(BT, f.not(SH));
        Formula BSFormula = f.and(ST, BT);

        ExogenousEquation BTExoEquation = new ExogenousEquation(BTExo);
        ExogenousEquation STExoEquation = new ExogenousEquation(STExo);

        EndogenousEquation BTEquation = new EndogenousEquation(BT, BTFormula);
        EndogenousEquation STEquation = new EndogenousEquation(ST, STFormula);
        EndogenousEquation SHEquation = new EndogenousEquation(SH, SHFormula);
        EndogenousEquation BHEquation = new EndogenousEquation(BH, BHFormula);
        EndogenousEquation BSEquation = new EndogenousEquation(BS, BSFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(BTEquation, STEquation, SHEquation, BHEquation,
                BSEquation, BTExoEquation, STExoEquation));

        CausalModel causalModel = new CausalModel("BillySuzy", equations);
    }

    @Test
    public void Should_BeValid_When_EverythingFine() {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable c = f.variable("c");

        EndogenousEquation equationA = new EndogenousEquation(a, b);
        EndogenousEquation equationB = new EndogenousEquation(b, c);
        ExogenousEquation equationC = new ExogenousEquation(c);

        CausalModel causalModel =
                new CausalModel(null, new HashSet<>(Arrays.asList(equationA, equationB, equationC)));
        assertTrue(causalModel.isValid());
    }

    @Test
    public void Should_BeInvalid_When_TwoEquationsWithSameVariable() {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable c = f.variable("c");

        EndogenousEquation equationA1 = new EndogenousEquation(a, b);
        EndogenousEquation equationA2 = new EndogenousEquation(a, c);
        ExogenousEquation equationC = new ExogenousEquation(c);

        CausalModel causalModel =
                new CausalModel(null, new HashSet<>(Arrays.asList(equationA1, equationA2, equationC)));
        assertFalse(causalModel.isValid());
    }

    @Test
    public void Should_BeInvalid_When_NotEachVariableDefinedByEquation() {
        Variable a = f.variable("a");
        Variable b = f.variable("b");
        Variable c = f.variable("c");

        EndogenousEquation equationA = new EndogenousEquation(a, b);
        EndogenousEquation equationB = new EndogenousEquation(b, c);

        CausalModel causalModel = new CausalModel(null, new HashSet<>(Arrays.asList(equationA, equationB)));
        assertFalse(causalModel.isValid());
    }
}