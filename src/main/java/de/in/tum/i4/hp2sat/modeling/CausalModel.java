package de.in.tum.i4.hp2sat.modeling;

import org.logicng.formulas.Variable;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CausalModel {
    private String name;
    private Set<Equation> equations;

    private Set<EndogenousEquation> endogenousEquations;
    private Set<ExogenousEquation> exogenousEquations;

    private Set<Variable> variables;

    public CausalModel(String name, Set<Equation> equations) {
        this.name = name;
        this.equations = equations;

        this.endogenousEquations = this.equations.stream()
                .filter(e -> e instanceof EndogenousEquation)
                .map(e -> (EndogenousEquation) e).collect(Collectors.toSet());
        this.exogenousEquations = this.equations.stream()
                .filter(e -> e instanceof ExogenousEquation)
                .map(e -> (ExogenousEquation) e).collect(Collectors.toSet());

        this.variables = this.equations.stream().map(Equation::getLeft).collect(Collectors.toSet());
        this.endogenousEquations.forEach(e -> this.variables.addAll(e.getRight().variables()));
    }

    public boolean isValid() {
        boolean existsOneEquationForEachVariable = equations.size() == this.variables.size();
        boolean existsNoDuplicateEquationForEachVariable =
                equations.size() == equations.stream().map(Equation::getLeft).collect(Collectors.toSet()).size();
        // TODO: no circularity
        return existsOneEquationForEachVariable && existsNoDuplicateEquationForEachVariable;
    }

    public String getName() {
        return name;
    }

    public Set<Equation> getEquations() {
        return equations;
    }
}
