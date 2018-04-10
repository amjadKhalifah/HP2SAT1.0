package de.in.tum.i4.hp2sat.modeling;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

public class Equation {
    private Variable variable;
    private Formula formula;

    public Equation(Variable variable, Formula formula) {
        this.variable = variable;
        this.formula = formula;
    }

    public Variable getVariable() {
        return variable;
    }

    public Formula getFormula() {
        return formula;
    }
}
