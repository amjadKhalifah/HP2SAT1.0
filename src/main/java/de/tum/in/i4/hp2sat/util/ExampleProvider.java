package de.tum.in.i4.hp2sat.util;

import de.tum.in.i4.hp2sat.causality.CausalModel;
import de.tum.in.i4.hp2sat.causality.Equation;
import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExampleProvider {
    public static CausalModel billySuzy() throws InvalidCausalModelException {
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

        Set<Equation> equations = new HashSet<>(Arrays.asList(BTEquation, STEquation, SHEquation, BHEquation,
                BSEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(BTExo, STExo));

        CausalModel causalModel = new CausalModel("BillySuzy", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel forestFire(boolean disjunctive) throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable LExo = f.variable("L_exo");
        Variable MDExo = f.variable("MD_exo");

        Variable L = f.variable("L"); // lightning
        Variable MD = f.variable("MD"); // match dropped
        Variable FF = f.variable("FF"); // forest fire

        Formula LFormula = LExo;
        Formula MDFormula = MDExo;
        Formula FFFormula = disjunctive ? f.or(L, MD) : f.and(L, MD);

        Equation LEquation = new Equation(L, LFormula);
        Equation MDEquation = new Equation(MD, MDFormula);
        Equation FFEquation = new Equation(FF, FFFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(LEquation, MDEquation, FFEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(LExo, MDExo));

        String name = "ForestFire_" + (disjunctive ? "disjunctive" : "conjunctive");
        CausalModel causalModel = new CausalModel(name, equations, exogenousVariables);
        return causalModel;
    }

    // extended Billy Suzy Example: Mixed endo- and exogenous variables
    public static CausalModel billySuzyExtended() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable BTExo = f.variable("BT_exo");
        Variable STExo = f.variable("ST_exo");
        Variable WExo = f.variable("W_exo"); // W = Wind

        Variable BT = f.variable("BT");
        Variable ST = f.variable("ST");
        Variable BH = f.variable("BH");
        Variable SH = f.variable("SH");
        Variable BS = f.variable("BS");

        Formula BTFormula = BTExo;
        Formula STFormula = STExo;
        // Suzy hits only if she throws and there is no wind
        Formula SHFormula = f.and(ST, f.not(WExo));
        // Billy only hits if he throws, Suzy doesn't and no wind
        Formula BHFormula = f.and(BT, f.not(SH), f.not(WExo));
        Formula BSFormula = f.or(SH, BH);

        Equation BTEquation = new Equation(BT, BTFormula);
        Equation STEquation = new Equation(ST, STFormula);
        Equation SHEquation = new Equation(SH, SHFormula);
        Equation BHEquation = new Equation(BH, BHFormula);
        Equation BSEquation = new Equation(BS, BSFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(BTEquation, STEquation, SHEquation, BHEquation,
                BSEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(BTExo, STExo, WExo));

        CausalModel causalModel = new CausalModel("BillySuzy", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel guns() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable AExo = f.variable("A_exo");
        Variable BExo = f.variable("B_exo");
        Variable CExo = f.variable("C_exo");

        Variable A = f.variable("A");
        Variable B = f.variable("B");
        Variable C = f.variable("C");
        Variable AB = f.variable("AB");
        Variable D = f.variable("D");

        Formula AFormula = AExo;
        Formula BFormula = BExo;
        Formula CFormula = CExo;
        Formula ABFormula = f.and(A, B);
        Formula DFormula = f.or(AB, C);

        Equation AEquation = new Equation(A, AFormula);
        Equation BEquation = new Equation(B, BFormula);
        Equation CEquation = new Equation(C, CFormula);
        Equation ABEquation = new Equation(AB, ABFormula);
        Equation DEquation = new Equation(D, DFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(AEquation, BEquation, CEquation, ABEquation,
                DEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(AExo, BExo, CExo));

        CausalModel causalModel = new CausalModel("Guns", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel dummy() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable AExo = f.variable("A_exo");
        Variable BExo = f.variable("B_exo");

        Variable A = f.variable("A");
        Variable B = f.variable("B");
        Variable C = f.variable("C");
        Variable D = f.variable("D");
        Variable E = f.variable("E");
        Variable F = f.variable("F");
        Variable G = f.variable("G");
        Variable H = f.variable("H");

        Formula AFormula = AExo;
        Formula BFormula = f.and(BExo, f.not(A));
        Formula CFormula = f.or(A, B);
        Formula DFormula = A;
        Formula EFormula = f.not(A);
        Formula FFormula = f.or(C, D, E, G, H);
        Formula GFormula = f.not(C);
        Formula HFormula = f.and(f.not(C), f.not(G));

        Equation AEquation = new Equation(A, AFormula);
        Equation BEquation = new Equation(B, BFormula);
        Equation CEquation = new Equation(C, CFormula);
        Equation DEquation = new Equation(D, DFormula);
        Equation EEquation = new Equation(E, EFormula);
        Equation FEquation = new Equation(F, FFormula);
        Equation GEquation = new Equation(G, GFormula);
        Equation HEquation = new Equation(H, HFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(AEquation, BEquation, CEquation, DEquation, EEquation,
                FEquation, GEquation, HEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(AExo, BExo));

        CausalModel causalModel = new CausalModel("Dummy", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel dummy2() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable AExo = f.variable("A_exo");
        Variable BExo = f.variable("B_exo");
        Variable CExo = f.variable("C_exo");
        Variable DExo = f.variable("D_exo");

        Variable A1 = f.variable("A1");
        Variable B1 = f.variable("B1");
        Variable C1 = f.variable("C1");
        Variable D1 = f.variable("D1");
        Variable A2 = f.variable("A2");
        Variable B2 = f.variable("B2");
        Variable C2 = f.variable("C2");
        Variable D2 = f.variable("D2");
        Variable X = f.variable("X");
        Variable Y = f.variable("Y");

        Formula A1Formula = f.and(AExo, B1);
        Formula B1Formula = BExo;
        Formula C1Formula = f.and(CExo, f.not(B1));
        Formula D1Formula = f.and(DExo, C1);
        Formula A2Formula = A1;
        Formula B2Formula = B1;
        Formula C2Formula = C1;
        Formula D2Formula = D1;
        Formula XFormula = f.or(f.and(A2, B2), f.and(C2, D2));
        Formula YFormula = f.or(B2, C2);

        Equation A1Equation = new Equation(A1, A1Formula);
        Equation B1Equation = new Equation(B1, B1Formula);
        Equation C1Equation = new Equation(C1, C1Formula);
        Equation D1Equation = new Equation(D1, D1Formula);
        Equation A2Equation = new Equation(A2, A2Formula);
        Equation B2Equation = new Equation(B2, B2Formula);
        Equation C2Equation = new Equation(C2, C2Formula);
        Equation D2Equation = new Equation(D2, D2Formula);
        Equation XEquation = new Equation(X, XFormula);
        Equation YEquation = new Equation(Y, YFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(A1Equation, B1Equation, C1Equation, D1Equation,
                A2Equation, B2Equation, C2Equation, D2Equation, XEquation, YEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(AExo, BExo, CExo, DExo));

        CausalModel causalModel = new CausalModel("Dummy2", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel dummyXOR() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable AExo = f.variable("A_exo");
        Variable BExo = f.variable("B_exo");

        Variable A = f.variable("A");
        Variable B = f.variable("B");
        Variable C = f.variable("C");

        Formula AFormula = AExo;
        Formula BFormula = BExo;
        Formula CFormula = f.and(f.or(A, B), f.not(f.and(A, B))); // XOR

        Equation AEquation = new Equation(A, AFormula);
        Equation BEquation = new Equation(B, BFormula);
        Equation CEquation = new Equation(C, CFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(AEquation, BEquation, CEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(AExo, BExo));

        CausalModel causalModel = new CausalModel("DummyXOR", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel dummyXNOR() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable AExo = f.variable("A_exo");
        Variable BExo = f.variable("B_exo");

        Variable A = f.variable("A");
        Variable B = f.variable("B");
        Variable C = f.variable("C");

        Formula AFormula = AExo;
        Formula BFormula = BExo;
        Formula CFormula = f.or(f.and(A, B), (f.and(f.not(A), f.not(B)))); // XNOR

        Equation AEquation = new Equation(A, AFormula);
        Equation BEquation = new Equation(B, BFormula);
        Equation CEquation = new Equation(C, CFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(AEquation, BEquation, CEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(AExo, BExo));

        CausalModel causalModel = new CausalModel("DummyXNOR", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel benchmarkModel() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable A = f.variable("A");
        Variable B = f.variable("B");
        Variable C = f.variable("C");
        Variable D = f.variable("D");
        Variable E = f.variable("E");
        Variable F = f.variable("F");
        Variable G = f.variable("G");
        Variable H = f.variable("H");
        Variable I = f.variable("I");
        Variable J = f.variable("J");
        Variable K = f.variable("K");
        Variable L = f.variable("L");
        Variable M = f.variable("M");
        Variable N = f.variable("N");
        Variable O = f.variable("O");
        Variable P = f.variable("P");
        Variable Q = f.variable("Q");
        Variable R = f.variable("R");
        Variable S = f.variable("S");
        Variable T = f.variable("T");
        Variable U = f.variable("U");
        Variable V = f.variable("V");
        Variable W = f.variable("W");
        Variable X = f.variable("X");
        Variable Y = f.variable("Y");

        Variable SExo = f.variable("S_exo");
        Variable TExo = f.variable("T_exo");
        Variable UExo = f.variable("U_exo");
        Variable VExo = f.variable("V_exo");
        Variable WExo = f.variable("W_exo");
        Variable XExo = f.variable("X_exo");
        Variable YExo = f.variable("Y_exo");

        Equation AEquation = new Equation(A, f.or(D, E));
        Equation BEquation = new Equation(B, f.or(E, F));
        Equation CEquation = new Equation(C, f.or(F, G));
        Equation DEquation = new Equation(D, f.or(H, I));
        Equation EEquation = new Equation(E, f.or(I, J));
        Equation FEquation = new Equation(F, f.or(J, K));
        Equation GEquation = new Equation(G, f.or(K, L));
        Equation HEquation = new Equation(H, f.or(M, N));
        Equation IEquation = new Equation(I, f.or(N, O));
        Equation JEquation = new Equation(J, f.or(O, P));
        Equation KEquation = new Equation(K, f.or(P, Q));
        Equation LEquation = new Equation(L, f.or(Q, R));
        Equation MEquation = new Equation(M, f.or(S, T));
        Equation NEquation = new Equation(N, f.or(T, U));
        Equation OEquation = new Equation(O, f.or(U, V));
        Equation PEquation = new Equation(P, f.or(V, W));
        Equation QEquation = new Equation(Q, f.or(W, X));
        Equation REquation = new Equation(R, f.or(X, Y));
        Equation SEquation = new Equation(S, SExo);
        Equation TEquation = new Equation(T, TExo);
        Equation UEquation = new Equation(U, UExo);
        Equation VEquation = new Equation(V, VExo);
        Equation WEquation = new Equation(W, WExo);
        Equation XEquation = new Equation(X, XExo);
        Equation YEquation = new Equation(Y, YExo);

        Set<Equation> equations = new HashSet<>(Arrays.asList(AEquation, BEquation, CEquation, DEquation, EEquation,
                FEquation, GEquation, HEquation, IEquation, JEquation, KEquation, LEquation, MEquation, NEquation,
                OEquation, PEquation, QEquation, REquation, SEquation, TEquation, UEquation, VEquation, WEquation,
                XEquation, YEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(SExo, TExo, UExo, VExo, WExo, XExo, YExo));

        CausalModel causalModel = new CausalModel("BenchmarkModel", equations, exogenousVariables);
        return causalModel;
    }

    public static CausalModel generateBinaryTreeBenchmarkModel(int depth) throws InvalidCausalModelException {
        // TODO doc
        FormulaFactory f = new FormulaFactory();
        String name = "BinaryTreeBenchmarkModel";
        if (depth >= 0) {
            int numberOfNodes = (int) Math.pow(2, depth + 1) - 1;
            int numberOfLeaves = (numberOfNodes + 1) / 2; // no double needed; is always integer for full binary tree

            int[] rangeArray = IntStream.range(numberOfNodes - numberOfLeaves, numberOfNodes)
                    .map(i -> numberOfNodes - i + (numberOfNodes - numberOfLeaves) - 1) // reverse
                    .toArray();
            List<Variable> exogenousVariables = Arrays.stream(rangeArray).mapToObj(i -> f.variable(i + "_exo"))
                    .collect(Collectors.toList());

            Set<Equation> equations = new HashSet<>();
            List<Variable> variablesInPreviousLevel = new ArrayList<>();
            for (int i = 0; i < rangeArray.length; i++) {
                Variable endogenousVariable = f.variable("" + rangeArray[i]);
                variablesInPreviousLevel.add(endogenousVariable);
                Equation equation = new Equation(endogenousVariable, exogenousVariables.get(i));
                equations.add(equation);
            }

            int count = numberOfNodes - numberOfLeaves - 1;
            for (int i = depth - 1; i >= 0; i--) {
                int numberOfNodesInCurrentLevel = ((int) Math.pow(2, i + 1) - 1 + 1) / 2;
                List<Variable> variablesInPreviousLevelNew = new ArrayList<>();
                for (int k = 0; k < numberOfNodesInCurrentLevel; k++) {
                    Variable endogenousVariable = f.variable("" + count--);
                    variablesInPreviousLevelNew.add(endogenousVariable);

                    Formula formula = f.or(variablesInPreviousLevel.get(2 * k),
                            variablesInPreviousLevel.get(2 * k + 1));
                    Equation equation = new Equation(endogenousVariable, formula);
                    equations.add(equation);
                }
                variablesInPreviousLevel = variablesInPreviousLevelNew;
            }
            CausalModel causalModel = new CausalModel(name, equations, new HashSet<>(exogenousVariables));
            return causalModel;
        } else {
            return null;
        }
    }
}
