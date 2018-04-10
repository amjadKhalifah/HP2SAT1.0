package de.in.tum.i4.hp2sat.modeling;

import java.util.Set;
import java.util.stream.Collectors;

public class CausalModel {
    private String name;
    private Set<Equation> equations;

    private Set<EndogenousEquation> endogenousEquations;
    private Set<ExogenousEquation> exogenousEquations;

    public CausalModel(String name, Set<Equation> equations) {
        this.name = name;
        this.equations = equations;

        this.endogenousEquations = this.equations.stream()
                .filter(e -> e instanceof EndogenousEquation)
                .map(e -> (EndogenousEquation) e).collect(Collectors.toSet());
        this.exogenousEquations = this.equations.stream()
                .filter(e -> e instanceof ExogenousEquation)
                .map(e -> (ExogenousEquation) e).collect(Collectors.toSet());
    }

    public String getName() {
        return name;
    }

    public Set<Equation> getEquations() {
        return equations;
    }
}
