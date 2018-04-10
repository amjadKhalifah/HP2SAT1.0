package de.in.tum.i4.hp2sat.modeling;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

public class EndogenousEquation extends Equation {
    private Formula right;

    public EndogenousEquation(Variable left, Formula right) {
        super(left);
        this.right = right;
    }

    public Formula getRight() {
        return right;
    }
}
