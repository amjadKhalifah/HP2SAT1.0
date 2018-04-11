package de.tum.in.i4.hp2sat.causality;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Constant;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.Map;
import java.util.Set;

class CausalitySolver {
    static Tristate solve(CausalModel causalModel, Map<Variable, Constant> context, Set<Literal> phi,
                          Set<Literal> cause, Set<Variable> w) {
        // TODO
        return Tristate.UNDEF;
    }
}
