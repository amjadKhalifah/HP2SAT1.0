package de.tum.in.i4.hp2sat.causality;

import java.util.Objects;

public class CausalityCheckResult {
    private boolean ac1;
    private boolean ac2;
    private boolean ac3;

    public CausalityCheckResult(boolean ac1, boolean ac2, boolean ac3) {
        this.ac1 = ac1;
        this.ac2 = ac2;
        this.ac3 = ac3;
    }

    @Override
    public String toString() {
        return "CausalityCheckResult{" +
                "ac1=" + ac1 +
                ", ac2=" + ac2 +
                ", ac3=" + ac3 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CausalityCheckResult that = (CausalityCheckResult) o;
        return ac1 == that.ac1 &&
                ac2 == that.ac2 &&
                ac3 == that.ac3;
    }

    @Override
    public int hashCode() {

        return Objects.hash(ac1, ac2, ac3);
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
}
