# hp2sat

[![Build Status](https://travis-ci.com/srehwald/hp2sat.svg?token=YUmexXqP9AGj9wNMuDhx&branch=master)](https://travis-ci.com/srehwald/hp2sat)

## Installation

```bash
$ mvn install
```

## Usage

Creation of a causal model:
```java
FormulaFactory f = new FormulaFactory();
Variable BTExo = f.variable("BT_exo");
Variable STExo = f.variable("ST_exo");

Variable BT = f.variable("BT");
Variable ST = f.variable("ST");
Variable BH = f.variable("BH");
Variable SH = f.variable("SH");
Variable BS = f.variable("BS");

Formula BTFormula = BTExo;
Formula STFormula = STExo;
Formula SHFormula = ST;
Formula BHFormula = f.and(BT, f.not(SH));
Formula BSFormula = f.or(SH, BH);

Equation BTEquation = new Equation(BT, BTFormula);
Equation STEquation = new Equation(ST, STFormula);
Equation SHEquation = new Equation(SH, SHFormula);
Equation BHEquation = new Equation(BH, BHFormula);
Equation BSEquation = new Equation(BS, BSFormula);

Set<Equation> equations = new HashSet<>(Arrays.asList(BTEquation, STEquation, SHEquation, BHEquation, BSEquation));
Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(BTExo, STExo));

CausalModel causalModel = new CausalModel("RockThrowing", equations, exogenousVariables, f);
```

Check whether *ST = 1* is a cause of *BS = 1* in the previously created causal model given *ST_exo, BT_exo = 1*:
```java
Set<Literal> context = new HashSet<>(Arrays.asList(f.literal("BT_exo", true), f.literal("ST_exo", true)));
Set<Literal> cause = new HashSet<>(Collections.singletonList(f.variable("ST")));
Formula phi = f.variable("BS");
CausalitySolverResult causalitySolverResult = CauscausalModel.isCause(context, phi, cause, SolvingStrategy.SAT);
```