package de.in.tum.i4.hp2sat.modeling;

import java.util.Set;

public class CausalModel {
    private String name;
    private Set<Equation> equations;

    public CausalModel(String name, Set<Equation> equations) {
        this.name = name;
        this.equations = equations;
    }

    public String getName() {
        return name;
    }

    public Set<Equation> getEquations() {
        return equations;
    }
}
