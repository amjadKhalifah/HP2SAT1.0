# hp2sat

[![Build Status](https://travis-ci.org/anonymous-submission-dev/HP2SAT.svg?branch=master)](https://travis-ci.org/anonymous-submission-dev/HP2SAT)

## Background
This library allows to determine actual causality according to the modified Halpern-Pearl definition of causality [1]
. The used examples in the unit test cases (specifically in [CausalitySolverInstanceTest](
./src/test/java/edu/hp2sat/causality/CausalitySolverInstanceTest.java)) are described [here](./doc/evaluated-models.pdf).

## Installation

Currently, this library is _not_ published in a Maven repository. Please build it manually from source: 

```bash
$ mvn install
```
Then, you can import it using Maven:
```xml
<dependency>
    <groupId>edu</groupId>
    <artifactId>hp2sat</artifactId>
    <version>1.0</version>
</dependency>
```

Alternatively, a pre-built ```.jar``` is offered in the [release section](https://github.com/anonymous-submission-dev/HP2SAT/releases) of this repository.

## Usage

### General

#### Creation of a causal model
```java
// instantiate a new FormulaFactory
FormulaFactory f = new FormulaFactory();

// create exogenous variables; using _exo is not required, but used to distinguish them
Variable BTExo = f.variable("BT_exo");
Variable STExo = f.variable("ST_exo");

// create endogenous variables; technically, there is no difference to exogenous ones
Variable BT = f.variable("BT");
Variable ST = f.variable("ST");
Variable BH = f.variable("BH");
Variable SH = f.variable("SH");
Variable BS = f.variable("BS");

// create the formula/function for each endogenous variable
Formula BTFormula = BTExo;
Formula STFormula = STExo;
Formula SHFormula = ST;
Formula BHFormula = f.and(BT, f.not(SH));
Formula BSFormula = f.or(SH, BH);

// create the equations of the causal model: each endogenous variable and its formula form an equation
Equation BTEquation = new Equation(BT, BTFormula);
Equation STEquation = new Equation(ST, STFormula);
Equation SHEquation = new Equation(SH, SHFormula);
Equation BHEquation = new Equation(BH, BHFormula);
Equation BSEquation = new Equation(BS, BSFormula);

Set<Equation> equations = new HashSet<>(Arrays.asList(BTEquation, STEquation, SHEquation,
    BHEquation, BSEquation));
Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(BTExo, STExo));

// instantiate the CausalModel
CausalModel causalModel = new CausalModel("RockThrowing", equations, exogenousVariables, f);
```

#### Check whether *ST = 1* is a cause of *BS = 1* in the previously created causal model given *ST_exo, BT_exo = 1* as context
```java
// IMPORTANT: Use the same FormulaFactory instance as in the above!

/*
 * Create positive literals for ST_exo and BT_exo. If ST_exo, BT_exo = 0, we would create negative ones,
 * e.g. f.literal("ST_exo", false). Using f.variable("ST_exo") would be a shortcut for f.literal("ST_exo", true)
 */
Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", true),
    f.literal("ST_exo", true)));

/*
 * Similar as for the context, we specify f.literal("ST", true) as cause and f.variable("BS") as phi, as we 
 * want to express ST = 1 and BS = 1, respectively.
 */
Set<Literal> cause = new HashSet<>(Collections.singletonList(f.literal("ST", true)));
Formula phi = f.variable("BS");

// finally, call isCause on the causal model using the SAT-based algorithm
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, cause, SolvingStrategy.SAT);
```

#### Use other algorithms

The ```SolvingStrategy``` enum contains all currently supported algorithms/strategies:
```java
public enum SolvingStrategy {
    BRUTE_FORCE, SAT, SAT_MINIMAL, SAT_COMBINED
}
```

Just call the ```isCause```-method with the respective ```SolvingStrategy```:
```java
// Brute-Force
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, cause, SolvingStrategy.BRUTE_FORCE);

// SAT-based
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, cause, SolvingStrategy.SAT);

// SAT-based returning a minimal W for AC2
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, cause, SolvingStrategy.SAT_MINIMAL);

// SAT-based where checking AC2 and AC3 is combined
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, cause, SolvingStrategy.SAT_COMBINED);
```

### Important Notes

- When working with a causal model, *always* use the *same* `FormulaFactory` instance. If not, an exception might occur.
- When creating a `CausalModel`, it is checked whether the latter is valid. It needs to fulfill the following 
characteristics; otherwise an exception is thrown:
    - Each variable needs to be either exogenous or defined by *exactly one* equation.
    - The causal model must be *acyclic*. That is, no variables are allowed to mutually depend on each other 
    (directly and indirectly)
    - Variables must not be named with `"_dummy"`.
    
## Literature

[1] J. Y. Halpern. "A Modification of the Halpern-Pearl Definition of Causality." In: Proceedings of the Twenty-Fourth International Joint Conference on Artificial Intelligence, IJCAI 2015, Buenos Aires, Argentina, July 25-31, 2015. 2015, pp. 3022–3033.