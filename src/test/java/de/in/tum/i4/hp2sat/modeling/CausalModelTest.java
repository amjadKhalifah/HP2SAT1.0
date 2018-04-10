package de.in.tum.i4.hp2sat.modeling;

import org.junit.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class CausalModelTest {
    @Test
    public void Should_CreateCausalModel_When_EverythingFine() {
        FormulaFactory f = new FormulaFactory();

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

        EndogenousEquation BTEquation = new EndogenousEquation(BT, BTFormula);
        EndogenousEquation STEquation = new EndogenousEquation(ST, STFormula);
        EndogenousEquation SHEquation = new EndogenousEquation(SH, SHFormula);
        EndogenousEquation BHEquation = new EndogenousEquation(BH, BHFormula);
        EndogenousEquation BSEquation = new EndogenousEquation(BS, BSFormula);

        Set<Equation> equations =
                new HashSet<>(Arrays.asList(BTEquation, STEquation, SHEquation, BHEquation, BSEquation));
        
        CausalModel causalModel = new CausalModel("BillySuzy", equations);
    }
}