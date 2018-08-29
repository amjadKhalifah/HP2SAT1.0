package edu.hp2sat.causality;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

public class Equation {
    private Variable variable;
    private Formula formula;

    public Equation(Variable variable, Formula formula) {
        this.variable = variable;
        this.formula = formula;
    }

    public Equation(Equation equation) {
        this(equation.variable, equation.formula);
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

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public void setFormula(Formula formula) {
        this.formula = formula;
    }
}
