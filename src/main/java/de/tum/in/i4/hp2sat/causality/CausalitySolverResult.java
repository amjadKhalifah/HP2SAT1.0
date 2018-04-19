package de.tum.in.i4.hp2sat.causality;

import org.logicng.formulas.Literal;

import java.util.Objects;
import java.util.Set;

public class CausalitySolverResult {
    private boolean ac1;
    private boolean ac2;
    private boolean ac3;
    private Set<Literal> cause;
    private Set<Literal> w;

    public CausalitySolverResult(boolean ac1, boolean ac2, boolean ac3, Set<Literal> cause, Set<Literal> w) {
        this.ac1 = ac1;
        this.ac2 = ac2;
        this.ac3 = ac3;
        this.cause = cause;
        this.w = w;
    }

    @Override
    public String toString() {
        return "CausalitySolverResult{" +
                "ac1=" + ac1 +
                ", ac2=" + ac2 +
                ", ac3=" + ac3 +
                ", cause=" + cause +
                ", w=" + w +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CausalitySolverResult that = (CausalitySolverResult) o;
        return ac1 == that.ac1 &&
                ac2 == that.ac2 &&
                ac3 == that.ac3 &&
                Objects.equals(cause, that.cause) &&
                Objects.equals(w, that.w);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ac1, ac2, ac3, cause, w);
    }

    public boolean isAc1() {
        return ac1;
    }

    public boolean isAc2() {
        return ac2;
    }

    public boolean isAc3() {
        return ac3;
    }

    public Set<Literal> getCause() {
        return cause;
    }

    public Set<Literal> getW() {
        return w;
    }
}
