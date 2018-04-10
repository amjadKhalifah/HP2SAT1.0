package de.in.tum.i4.hp2sat.modeling;

import de.in.tum.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.logicng.formulas.Variable;

import java.util.Set;
import java.util.stream.Collectors;

public class CausalModel {
    private String name;
    private Set<Equation> equations;
    private Set<Variable> exogenousVariables;

    private Set<Variable> variables;

    /**
     * Creates a new causal model
     *
     * @param name               name of the causal model
     * @param equations          equations for the endogenous variables of the causal model
     * @param exogenousVariables the exogenous variables of the causal model
     * @throws InvalidCausalModelException throws an exception if model is not valid: (1) each variable needs to be
     *                                     either defined by an equation or be exogenous; (2) no duplicate definition of variables; (3) no circular
     *                                     dependencies
     */
    public CausalModel(String name, Set<Equation> equations, Set<Variable> exogenousVariables) throws InvalidCausalModelException {
        this.name = name;
        this.equations = equations;
        this.exogenousVariables = exogenousVariables;

        this.variables = this.equations.stream().map(Equation::getVariable).collect(Collectors.toSet());
        this.equations.forEach(e -> this.variables.addAll(e.getFormula().variables()));
        this.variables.addAll(exogenousVariables);

        isValid(); // possibly throws exception
    }

    /**
     * Checks whether the current causal model is valid.
     *
     * @throws InvalidCausalModelException thrown if invalid
     */
    private void isValid() throws InvalidCausalModelException {
        boolean existsDefinitionForEachVariable = equations.size() + exogenousVariables.size() == this.variables.size();
        boolean existsNoDuplicateEquationForEachVariable =
                equations.size() == equations.stream().map(Equation::getVariable).collect(Collectors.toSet()).size();
        boolean existsCircularDependency = equations.parallelStream()
                .anyMatch(e -> isVariableInEquation(e.getVariable(), e));

        if (!(existsDefinitionForEachVariable && existsNoDuplicateEquationForEachVariable && !existsCircularDependency))
            throw new InvalidCausalModelException();
    }

    /**
     * Checks whether a given variable is within a given equation or within the equations of the variables used
     * within the given equation (recursive!)
     *
     * @param variable the variable for which we want to know whether it is in the given equation
     * @param equation the equation whithin which we search for the variable
     * @return true, if variable was found; otherwise false
     */
    private boolean isVariableInEquation(Variable variable, Equation equation) {
        Set<Variable> variables = equation.getFormula().variables();
        // check if formula of equation contains variable
        if (variables.contains(variable)) {
            return true;
        }

        // check for all other non-exogenous variables whether their corresponding equation contains the searched var
        for (Variable v : variables.stream()
                .filter(v -> !this.exogenousVariables.contains(v) && !v.equals(variable))
                .collect(Collectors.toSet())) {
            Equation eq = equations.stream().filter(e -> e.getVariable().equals(v)).findFirst().orElse(null);
            /*
            if eq is null, this would mean that a variable has no definition; this will be captured by another check
            in the isValid() method. Since the current method is private, we can ignore the case here.
             */
            if (eq != null) {
                if (isVariableInEquation(variable, eq))
                    return true;
            }
        }

        return false;
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
