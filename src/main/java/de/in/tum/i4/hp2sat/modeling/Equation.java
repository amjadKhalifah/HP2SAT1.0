package de.in.tum.i4.hp2sat.modeling;

import org.logicng.formulas.Variable;

public abstract class Equation {
    private Variable left;

    public Equation(Variable left) {
        this.left = left;
    }

    public Variable getLeft() {
        return left;
    }
}
