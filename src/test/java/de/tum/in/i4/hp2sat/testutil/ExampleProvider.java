package de.tum.in.i4.hp2sat.testutil;

import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.causality.CausalModel;
import de.tum.in.i4.hp2sat.causality.Equation;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    public static CausalModel arsonists(boolean disjunctive) throws InvalidCausalModelException {
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

        String name = "Arsonists_" + (disjunctive ? "disjunctive" : "conjunctive");
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
}
