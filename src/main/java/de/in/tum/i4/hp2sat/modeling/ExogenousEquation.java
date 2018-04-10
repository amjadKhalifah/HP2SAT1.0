package de.in.tum.i4.hp2sat.modeling;

import org.logicng.formulas.Constant;
import org.logicng.formulas.Variable;

public class ExogenousEquation extends Equation{
    private Constant constant;

    public ExogenousEquation(Variable left) {
        super(left);
    }

    public ExogenousEquation(Variable left, Constant constant) {
        super(left);
        this.constant = constant;
    }

    public Constant getConstant() {
        return constant;
    }

    public void setConstant(Constant constant) {
        this.constant = constant;
    }
}
