package de.tum.in.i4.hp2sat.causality;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

public class Equation {
    private Variable variable;
    private Formula formula;

    public Equation(Variable variable, Formula formula) {
        this.variable = variable;
        this.formula = formula;
    }

    @Override
    public String toString() {
        return variable + " = " + formula;
    }
    public Variable getVariable() {
        return variable;
    }

    public Formula getFormula() {
        return formula;
    }
}
