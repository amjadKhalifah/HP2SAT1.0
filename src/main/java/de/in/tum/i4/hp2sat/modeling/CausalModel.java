package de.in.tum.i4.hp2sat.modeling;

import org.logicng.formulas.Variable;

import java.util.Set;
import java.util.stream.Collectors;

public class CausalModel {
    private String name;
    private Set<Equation> equations;
    private Set<Variable> exogenousVariables;

    private Set<Variable> variables;

    public CausalModel(String name, Set<Equation> equations, Set<Variable> exogenousVariables) {
        this.name = name;
        this.equations = equations;
        this.exogenousVariables = exogenousVariables;

        this.variables = this.equations.stream().map(Equation::getVariable).collect(Collectors.toSet());
        this.equations.forEach(e -> this.variables.addAll(e.getFormula().variables()));
    }

    public boolean isValid() {
        boolean existsDefinitionForEachVariable = equations.size() + exogenousVariables.size() == this.variables.size();
        boolean existsNoDuplicateEquationForEachVariable =
                equations.size() == equations.stream().map(Equation::getVariable).collect(Collectors.toSet()).size();
        // TODO: no circularity
        return existsDefinitionForEachVariable && existsNoDuplicateEquationForEachVariable;
    }

    public String getName() {
        return name;
    }

    public Set<Equation> getEquations() {
        return equations;
    }

    public Set<Variable> getExogenousVariables() {
        return exogenousVariables;
    }
}
