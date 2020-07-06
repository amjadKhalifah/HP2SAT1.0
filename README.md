# Academic Citation 
Please cite the following paper when using this library. 

Ibrahim, A., Pretschner, A.: "From Checking to Inference: Actual Causality Computations as Optimization Problems". International Symposium on Automated Technology for Verification and Analysis. Springer (to appear), pre-print https://arxiv.org/abs/2006.03363


@inproceedings{ibrahim2020,
  author    = {Amjad Ibrahim and Alexander Pretschner},
  title     = {From Checking to Inference: Actual Causality Computations as Optimization Problems},
  booktitle = {Automated Technology for Verification and Analysis - 18th International
               Symposium, {ATVA} 2020, Proceedings},
  series    = {Lecture Notes in Computer Science},
  volume    = {},
  pages     = {in print},
  publisher = {Springer},
  year      = {2020},
}


[![Build Status](https://travis-ci.com/srehwald/hp2sat.svg?token=YUmexXqP9AGj9wNMuDhx&branch=develop)](https://travis-ci.com/srehwald/hp2sat)

## Background
This library allows to determine actual causality according to the modified Halpern-Pearl definition of causality. The theoretical background can be found in [1].

## Installation

Currently, this library is _not_ published in a Maven repository. Please build it manually from source: 

```bash
$ mvn install
```
Then, you can import it using Maven:
```xml
<dependency>
    <groupId>de.tum.in.i4</groupId>
    <artifactId>hp2sat</artifactId>
    <version>1.0</version>
</dependency>
```

## Usage
When using ILP, Gurobi solver should be installed and running on the machine. 
All models in the benchmark can be created using the class util/ExampleProvider.java
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
 * Create positive literals for ST_exo and BT_exo. If ST_exo, BT_exo = 0, we would create negative 
 * ones, e.g. f.literal("ST_exo", false). Using f.variable("ST_exo") would be a shortcut for 
 * f.literal("ST_exo", true)
 */
Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", true),
    f.literal("ST_exo", true)));

/*
 * Similar as for the context, we specify f.literal("ST", true) as cause and f.variable("BS") as phi, 
 * as we want to express ST = 1 and BS = 1, respectively.
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
    BRUTE_FORCE, BRUTE_FORCE_OPTIMIZED_W, SAT, SAT_MINIMAL, SAT_COMBINED, SAT_COMBINED_MINIMAL,
    SAT_OPTIMIZED_W, SAT_OPTIMIZED_W_MINIMAL, SAT_OPTIMIZED_FORMULAS, SAT_OPTIMIZED_FORMULAS_MINIMAL,
    SAT_OPTIMIZED_AC3, SAT_OPTIMIZED_AC3_MINIMAL, MAX_SAT, ILP, ILP_WHY
}
```

Just call the ```isCause```-method with the respective ```SolvingStrategy```. Some examples:
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
    
 // ILP based AC2 and AC3 checking
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, cause, SolvingStrategy.ILP);
    
 // MaxSAT based AC2 and AC3 checking
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, cause, SolvingStrategy.MAX_SAT);   
    
 // ILP based  causlity inference
CausalitySolverResult causalitySolverResult =
    CauscausalModel.isCause(context, phi, new HashSet<>(), SolvingStrategy.ILP_WHY); 
```

### Important Notes

- When working with a causal model, *always* use the same `FormulaFactory` instance. If not, an exception might occur.
- When creating a `CausalModel`, it is checked whether the latter is valid. It needs to fulfill the following 
characteristics; otherwise an exception is thrown:
    - Each variable needs to be either exogenous or defined by *exactly one* equation.
    - The causal model must be *acyclic*. That is, no variables are allowed to mutually depend on each other 
    (directly and indirectly)
    - Variables must not be named with `"_dummy"`.
    
## Literature

[1] J. Y. Halpern. "A Modification of the Halpern-Pearl Definition of Causality." In: Proceedings of the Twenty-Fourth International Joint Conference on Artificial Intelligence, IJCAI 2015, Buenos Aires, Argentina, July 25-31, 2015. 2015, pp. 3022–3033.
