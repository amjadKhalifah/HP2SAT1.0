package de.tum.in.i4.hp2sat.util;

import de.tum.in.i4.hp2sat.causality.CausalModel;
import de.tum.in.i4.hp2sat.causality.Equation;
import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.util.NewModelData.EndoVar;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Function;
import org.mariuszgromada.math.mxparser.NumericCausalModel;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExampleProvider {
    public static CausalModel billySuzy() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable BTExo = f.variable("BT_exo");
        Variable STExo = f.variable("ST_exo");

        Variable BT = f.variable("BT");
        Variable ST = f.variable("XT");
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

        CausalModel causalModel = new CausalModel("BillySuzy", equations, exogenousVariables, f);
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
        CausalModel causalModel = new CausalModel(name, equations, exogenousVariables, f);
        return causalModel;
    }

    // extended Billy Suzy Example: Mixed endo- and exogenous variables
    public static CausalModel billySuzyExtended() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable BTExo = f.variable("BT_exo");
        Variable STExo = f.variable("ST_exo");
        Variable WExo = f.variable("W_exo"); // W = Wind

        Variable BT = f.variable("BT");
        Variable ST = f.variable("XT");
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

        CausalModel causalModel = new CausalModel("BillySuzy", equations, exogenousVariables, f);
        return causalModel;
    }

    public static CausalModel prisoners() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable AExo = f.variable("A_exo");
        Variable BExo = f.variable("B_exo");
        Variable CExo = f.variable("C_exo");

        Variable A = f.variable("A");
        Variable B = f.variable("B");
        Variable C = f.variable("C");
        Variable D = f.variable("D");

        Formula AFormula = AExo;
        Formula BFormula = BExo;
        Formula CFormula = CExo;
        Formula DFormula = f.or(f.and(A, B), C);

        Equation AEquation = new Equation(A, AFormula);
        Equation BEquation = new Equation(B, BFormula);
        Equation CEquation = new Equation(C, CFormula);
        Equation DEquation = new Equation(D, DFormula);

        Set<Equation> equations = new HashSet<>(Arrays.asList(AEquation, BEquation, CEquation, DEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(AExo, BExo, CExo));

        CausalModel causalModel = new CausalModel("Guns", equations, exogenousVariables, f);
        return causalModel;
    }

    public static CausalModel assassin(boolean firstVariant) throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable AExo = f.variable("A_exo");
        Variable BExo = f.variable("B_exo");

        Variable A = f.variable("A");
        Variable B = f.variable("B");
        Variable VS = f.variable("VS");

        Equation AEquation = new Equation(A, firstVariant ? AExo : f.and(AExo, B));
        Equation BEquation = new Equation(B, BExo);
        Equation VSEquation = new Equation(VS, f.or(f.not(A), B));

        Set<Equation> equations = new HashSet<>(Arrays.asList(AEquation, BEquation, VSEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(AExo, BExo));

        String name = "Assassin_" + (firstVariant ? "firstVariant" : "secondVariant");
        CausalModel causalModel = new CausalModel(name, equations, exogenousVariables, f);
        return causalModel;
    }

    public static CausalModel railroad() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable LBExo = f.variable("LB_exo");
        Variable FExo = f.variable("F_exo");
        Variable RBExo = f.variable("RB_exo");

        Variable LB = f.variable("LB");
        Variable F = f.variable("F");
        Variable RB = f.variable("RB");
        Variable A = f.variable("A");

        Equation LBEquation = new Equation(LB, LBExo);
        Equation FEquation = new Equation(F, FExo);
        Equation RBEquation = new Equation(RB, RBExo);
        Equation AEquation = new Equation(A, f.not(f.or(f.and(F, RB), f.and(f.not(F), LB))));

        Set<Equation> equations = new HashSet<>(Arrays.asList(LBEquation, FEquation, RBEquation, AEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(LBExo, FExo, RBExo));

        CausalModel causalModel = new CausalModel("Railroad", equations, exogenousVariables, f);
        return causalModel;
    }

    public static CausalModel stealMasterKey() throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        Variable FS_U1_Exo = f.variable("FS_U1_Exo");
        Variable FN_U1_Exo = f.variable("FN_U1_Exo");
        Variable FF_U1_Exo = f.variable("FF_U1_Exo");
        Variable FDB_U1_Exo = f.variable("FDB_U1_Exo");
        Variable FS_U2_Exo = f.variable("FS_U2_Exo");
        Variable FN_U2_Exo = f.variable("FN_U2_Exo");
        Variable FF_U2_Exo = f.variable("FF_U2_Exo");
        Variable FDB_U2_Exo = f.variable("FDB_U2_Exo");
        Variable FS_U3_Exo = f.variable("FS_U3_Exo");
        Variable FN_U3_Exo = f.variable("FN_U3_Exo");
        Variable FF_U3_Exo = f.variable("FF_U3_Exo");
        Variable FDB_U3_Exo = f.variable("FDB_U3_Exo");
        Variable A_U1_Exo = f.variable("A_U1_Exo");
        Variable AD_U1_Exo = f.variable("AD_U1_Exo");
        Variable A_U2_Exo = f.variable("A_U2_Exo");
        Variable AD_U2_Exo = f.variable("AD_U2_Exo");
        Variable A_U3_Exo = f.variable("A_U3_Exo");
        Variable AD_U3_Exo = f.variable("AD_U3_Exo");

        Variable FS_U1 = f.variable("FS_U1");
        Variable FN_U1 = f.variable("FN_U1");
        Variable FF_U1 = f.variable("FF_U1");
        Variable FDB_U1 = f.variable("FDB_U1");
        Variable FS_U2 = f.variable("FS_U2");
        Variable FN_U2 = f.variable("FN_U2");
        Variable FF_U2 = f.variable("FF_U2");
        Variable FDB_U2 = f.variable("FDB_U2");
        Variable FS_U3 = f.variable("FS_U3");
        Variable FN_U3 = f.variable("FN_U3");
        Variable FF_U3 = f.variable("FF_U3");
        Variable FDB_U3 = f.variable("FDB_U3");
        Variable A_U1 = f.variable("A_U1");
        Variable AD_U1 = f.variable("AD_U1");
        Variable A_U2 = f.variable("A_U2");
        Variable AD_U2 = f.variable("AD_U2");
        Variable A_U3 = f.variable("A_U3");
        Variable AD_U3 = f.variable("AD_U3");

        Variable GP_U1 = f.variable("GP_U1");
        Variable GK_U1 = f.variable("GK_U1");
        Variable GP_U2 = f.variable("GP_U2");
        Variable GK_U2 = f.variable("GK_U2");
        Variable GP_U3 = f.variable("GP_U3");
        Variable GK_U3 = f.variable("GK_U3");
        Variable KMS_U1 = f.variable("KMS_U1");
        Variable KMS_U2 = f.variable("KMS_U2");
        Variable KMS_U3 = f.variable("KMS_U3");

        Variable DK_U1 = f.variable("DK_U1");
        Variable DK_U2 = f.variable("DK_U2");
        Variable DK_U3 = f.variable("DK_U3");
        Variable SD_U1 = f.variable("SD_U1");
        Variable SD_U2 = f.variable("SD_U2");
        Variable SD_U3 = f.variable("SD_U3");

        Variable DK = f.variable("DK");
        Variable SD = f.variable("SD");
        Variable SMK = f.variable("SMK");

        Equation FS_U1Equation = new Equation(FS_U1, FS_U1_Exo);
        Equation FN_U1Equation = new Equation(FN_U1, FN_U1_Exo);
        Equation FF_U1Equation = new Equation(FF_U1, FF_U1_Exo);
        Equation FDB_U1Equation = new Equation(FDB_U1, FDB_U1_Exo);
        Equation FS_U2Equation = new Equation(FS_U2, FS_U2_Exo);
        Equation FN_U2Equation = new Equation(FN_U2, FN_U2_Exo);
        Equation FF_U2Equation = new Equation(FF_U2, FF_U2_Exo);
        Equation FDB_U2Equation = new Equation(FDB_U2, FDB_U2_Exo);
        Equation FS_U3Equation = new Equation(FS_U3, FS_U3_Exo);
        Equation FN_U3Equation = new Equation(FN_U3, FN_U3_Exo);
        Equation FF_U3Equation = new Equation(FF_U3, FF_U3_Exo);
        Equation FDB_U3Equation = new Equation(FDB_U3, FDB_U3_Exo);
        Equation A_U1Equation = new Equation(A_U1, A_U1_Exo);
        Equation AD_U1Equation = new Equation(AD_U1, AD_U1_Exo);
        Equation A_U2Equation = new Equation(A_U2, A_U2_Exo);
        Equation AD_U2Equation = new Equation(AD_U2, AD_U2_Exo);
        Equation A_U3Equation = new Equation(A_U3, A_U3_Exo);
        Equation AD_U3Equation = new Equation(AD_U3, AD_U3_Exo);

        Equation GP_U1Equation = new Equation(GP_U1, f.or(FS_U1, FN_U1));
        Equation GK_U1Equation = new Equation(GK_U1, f.or(FF_U1, FDB_U1));
        Equation GP_U2Equation = new Equation(GP_U2, f.or(FS_U2, FN_U2));
        Equation GK_U2Equation = new Equation(GK_U2, f.or(FF_U2, FDB_U2));
        Equation GP_U3Equation = new Equation(GP_U3, f.or(FS_U3, FN_U3));
        Equation GK_U3Equation = new Equation(GK_U3, f.or(FF_U3, FDB_U3));
        Equation KMS_U1Equation = new Equation(KMS_U1, f.and(A_U1, AD_U1));
        Equation KMS_U2Equation = new Equation(KMS_U2, f.and(A_U2, AD_U2));
        Equation KMS_U3Equation = new Equation(KMS_U3, f.and(A_U3, AD_U3));

        Equation DK_U1Equation = new Equation(DK_U1, f.and(GP_U1, GK_U1));
        Equation DK_U2Equation = new Equation(DK_U2, f.and(GP_U2, GK_U2, f.not(DK_U1))); // preemption
        Equation DK_U3Equation = new Equation(DK_U3, f.and(GP_U3, GK_U3, f.not(DK_U1), f.not(DK_U2))); // preemption
        Equation SD_U1Equation = new Equation(SD_U1, KMS_U1);
        Equation SD_U2Equation = new Equation(SD_U2, f.and(KMS_U2, f.not(SD_U1))); // preemption
        Equation SD_U3Equation = new Equation(SD_U3, f.and(KMS_U3, f.not(SD_U1), f.not(SD_U2))); // preemption

        Equation DKEquation = new Equation(DK, f.or(DK_U1, DK_U2, DK_U3));
        Equation SDEquation = new Equation(SD, f.or(SD_U1, SD_U2, SD_U3));
        Equation SMKEquation = new Equation(SMK, f.or(DK, SD));

        Set<Equation> equations = new HashSet<>(Arrays.asList(FS_U1Equation, FN_U1Equation, FF_U1Equation,
                FDB_U1Equation, FS_U2Equation, FN_U2Equation, FF_U2Equation, FDB_U2Equation, FS_U3Equation,
                FN_U3Equation, FF_U3Equation, FDB_U3Equation, A_U1Equation, AD_U1Equation, A_U2Equation,
                AD_U2Equation, A_U3Equation, AD_U3Equation, GP_U1Equation, GK_U1Equation, GP_U2Equation,
                GK_U2Equation, GP_U3Equation, GK_U3Equation, KMS_U1Equation, KMS_U2Equation, KMS_U3Equation,
                DK_U1Equation, DK_U2Equation, DK_U3Equation, SD_U1Equation, SD_U2Equation, SD_U3Equation, DKEquation,
                SDEquation, SMKEquation));
        Set<Variable> exogenousVariables = new HashSet<>(Arrays.asList(FS_U1_Exo, FN_U1_Exo, FF_U1_Exo, FDB_U1_Exo,
                FS_U2_Exo, FN_U2_Exo, FF_U2_Exo, FDB_U2_Exo, FS_U3_Exo, FN_U3_Exo, FF_U3_Exo, FDB_U3_Exo, A_U1_Exo,
                AD_U1_Exo, A_U2_Exo, AD_U2_Exo, A_U3_Exo, AD_U3_Exo));

        CausalModel causalModel = new CausalModel("StealMasterKey", equations, exogenousVariables, f);
        return causalModel;
    }

    public static CausalModel leakage(boolean preemption) throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();
        HashMap<String, Variable> endos = new HashMap<>();
        HashMap<String, Variable> exos = new HashMap<>();
        Set<Equation> equations = new HashSet<>();

        for (int i = 1; i <= 41; i++) {
            String nameEndo = "X" + i;
            Variable endo = f.variable(nameEndo);
            endos.put(nameEndo, endo);

            if (i <= 26) {
                String nameExo = nameEndo + "_exo";
                Variable exo = f.variable(nameExo);
                exos.put(nameExo, exo);
                if (i < 26) {
                    equations.add(new Equation(endo, exo));
                } else {
                    equations.add(new Equation(endo, f.and(exo,
                            (preemption ? f.not(f.variable("X39")) : f.verum()))));
                }
            }
        }

        Equation X27Equation = new Equation(endos.get("X27"), f.or(endos.get("X3"), endos.get("X4")));
        Equation X28Equation = new Equation(endos.get("X28"), f.or(endos.get("X5"), endos.get("X6")));
        Equation X29Equation = new Equation(endos.get("X29"), f.or(endos.get("X7"), endos.get("X8")));
        Equation X30Equation = new Equation(endos.get("X30"), f.or(endos.get("X9"), endos.get("X10")));
        Equation X31Equation = new Equation(endos.get("X31"), f.or(endos.get("X12"), endos.get("X13"), endos.get("X14"),
                endos.get("X15"), endos.get("X16")));
        Equation X32Equation = new Equation(endos.get("X32"), f.and(endos.get("X18"), endos.get("X19")));
        Equation X33Equation = new Equation(endos.get("X33"), f.and(endos.get("X20"), endos.get("X21")));
        Equation X34Equation = new Equation(endos.get("X34"), f.and(endos.get("X22"), endos.get("X23")));
        Equation X35Equation = new Equation(endos.get("X35"), f.and(endos.get("X24"), endos.get("X25")));
        Equation X36Equation = new Equation(endos.get("X36"), f.or(endos.get("X27"), endos.get("X28"), endos.get("X29"),
                endos.get("X30")));
        Equation X37Equation = new Equation(endos.get("X37"), f.and(endos.get("X31"), endos.get("X17")));
        Equation X38Equation = new Equation(endos.get("X38"), f.and(endos.get("X1"), endos.get("X2"),
                preemption ? f.not(endos.get("X39")) : f.verum()));
        Equation X39Equation = new Equation(endos.get("X39"), f.and(endos.get("X36"), endos.get("X11")));
        Equation X40Equation = new Equation(endos.get("X40"), f.and(f.or(endos.get("X37"), endos.get("X32"), endos.get
                        ("X33"),
                endos.get("X34"), endos.get("X35")), preemption ? f.not(endos.get("X39")) : f.verum()));
        Equation X41Equation = new Equation(endos.get("X41"), f.or(endos.get("X38"), endos.get("X39"), endos.get("X40"),
                endos.get("X26")));

        equations.addAll(Arrays.asList(X27Equation, X28Equation, X29Equation, X30Equation, X31Equation, X32Equation,
                X33Equation, X34Equation, X35Equation, X36Equation, X37Equation, X38Equation, X39Equation,
                X40Equation, X41Equation));
        Set<Variable> exogenousVariables = new HashSet<>(exos.values());

        CausalModel causalModel = new CausalModel("Leakage", equations, exogenousVariables, f);
        return causalModel;
    }

    public static CausalModel stealMasterKey(int users) throws InvalidCausalModelException {
        FormulaFactory f = new FormulaFactory();

        String fromScript = "FS";
        String fromNetwork = "FN";
        String fromFile = "FF";
        String fromDB = "FDB";
        String getThePassphrase = "GP";
        String getTheKey = "GK";
        String decryptTheKey = "DK";
        String access = "A";
        String attachDebugger = "AD";
        String fromKeyManagementService = "KMS";
        String stealDecrypted = "SD";
        String stealMasterKey = "SMK";

        // will be set later on
        Equation DK_GlobalEquation = new Equation(f.variable(decryptTheKey), f.falsum());
        Equation SD_GlobalEquation = new Equation(f.variable(stealDecrypted), f.falsum());
        Equation SMK_Equation = new Equation(f.variable(stealMasterKey), f.or(f.variable(decryptTheKey),
                f.variable(stealDecrypted)));

        Set<Variable> exogenousVariables = new HashSet<>();
        Set<Equation> equations = new HashSet<>(Arrays.asList(DK_GlobalEquation, SD_GlobalEquation, SMK_Equation));

        for (int i = 1; i <= users; i++) {
            for (String s : Arrays.asList(fromScript, fromNetwork, fromFile, fromDB, access, attachDebugger)) {
                String name = s + "_U" + i;
                Variable exo = f.variable(name + "_Exo");

                exogenousVariables.add(exo);
                equations.add(new Equation(f.variable(name), exo));
            }

            Variable GP = f.variable(getThePassphrase + "_U" + i);
            Variable GK = f.variable(getTheKey + "_U" + i);
            Variable KMS = f.variable(fromKeyManagementService + "_U" + i);
            Variable DK = f.variable(decryptTheKey + "_U" + i);
            Variable SD = f.variable(stealDecrypted + "_U" + i);

            Equation GP_Equation = new Equation(GP,
                    f.or(f.variable(fromScript + "_U" + i), f.variable(fromNetwork + "_U" + i)));
            Equation GK_Equation = new Equation(GK,
                    f.or(f.variable(fromFile + "_U" + i), f.variable(fromDB + "_U" + i)));
            Equation KMS_Equation = new Equation(KMS,
                    f.and(f.variable(access + "_U" + i), f.variable(attachDebugger + "_U" + i)));
            Equation DK_Equation = new Equation(DK, f.and(GP, GK));
            Equation SD_Equation = new Equation(SD, KMS);
            equations.addAll(Arrays.asList(GP_Equation, GK_Equation, KMS_Equation, DK_Equation, SD_Equation));


            for (int k = i - 1; k >= 1; k--) {
                DK_Equation.setFormula(f.and(DK_Equation.getFormula(),
                        f.not(f.variable(decryptTheKey + "_U" + k))));
                SD_Equation.setFormula(f.and(SD_Equation.getFormula(),
                        f.not(f.variable(stealDecrypted + "_U" + k))));
            }

            DK_GlobalEquation.setFormula(f.or(DK_GlobalEquation.getFormula(), DK));
            SD_GlobalEquation.setFormula(f.or(SD_GlobalEquation.getFormula(), SD));
        }

        CausalModel causalModel = new CausalModel("StealMasterKey_" + users + "Users", equations,
                exogenousVariables, f);
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

        CausalModel causalModel = new CausalModel("Dummy", equations, exogenousVariables, f);
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

        CausalModel causalModel = new CausalModel("Dummy2", equations, exogenousVariables, f);
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

        CausalModel causalModel = new CausalModel("DummyXOR", equations, exogenousVariables, f);
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

        CausalModel causalModel = new CausalModel("DummyXNOR", equations, exogenousVariables, f);
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

        CausalModel causalModel = new CausalModel("BenchmarkModel", equations, exogenousVariables, f);
        return causalModel;
    }

    public static CausalModel dummyCombinedWithBinaryTree() throws InvalidCausalModelException {
        CausalModel dummy = ExampleProvider.dummy();
        FormulaFactory f = dummy.getFormulaFactory();
        CausalModel binaryTree = generateBinaryTreeBenchmarkModel(11, f);
        Equation equationA = dummy.getVariableEquationMap().get(f.variable("A"));
        equationA.setFormula(f.variable("n_0")); // 0 is the root node
        dummy.getExogenousVariables().remove(f.variable("A_exo"));

        Set<Equation> equations = new HashSet<>(dummy.getEquationsSorted());
        equations.addAll(binaryTree.getEquationsSorted());
        Set<Variable> exogenousVariables = dummy.getExogenousVariables();
        exogenousVariables.addAll(binaryTree.getExogenousVariables());
        CausalModel dummyCombinedWithBinaryTree = new CausalModel("DummyCombinedWithBinaryTree", equations,
                exogenousVariables, f);

        return dummyCombinedWithBinaryTree;
    }

    
    public static CausalModel dummyCombinedWithBinaryTree2() throws InvalidCausalModelException {
    	String prefix1= "p1";
    	String prefix2= "p2";
        CausalModel dummy = ExampleProvider.dummyXOR();
        FormulaFactory f = dummy.getFormulaFactory();
        
        
        CausalModel binaryTree = generateBinaryTreeBenchmarkModel(11, f,prefix1);
        Equation equationA = dummy.getVariableEquationMap().get(f.variable("A"));
        equationA.setFormula(f.variable(prefix1+"_"+"n_0")); // 0 is the root node
        dummy.getExogenousVariables().remove(f.variable("A_exo"));
        
        CausalModel binaryTree2 = generateBinaryTreeBenchmarkModel(11, f,prefix2);
        Equation equationB = dummy.getVariableEquationMap().get(f.variable("B"));
        equationB.setFormula(f.variable(prefix2+"_"+"n_0")); // 0 is the root node
        dummy.getExogenousVariables().remove(f.variable("B_exo"));
        

        Set<Equation> equations = new HashSet<>(dummy.getEquationsSorted());
        equations.addAll(binaryTree.getEquationsSorted());
        equations.addAll(binaryTree2.getEquationsSorted());
        Set<Variable> exogenousVariables = dummy.getExogenousVariables();
        exogenousVariables.addAll(binaryTree.getExogenousVariables());
        exogenousVariables.addAll(binaryTree2.getExogenousVariables());
           
        
        
        CausalModel dummyCombinedWithBinaryTree = new CausalModel("DummyCombinedWithBinaryTree2", equations,
                exogenousVariables, f);

        return dummyCombinedWithBinaryTree;
    }
    
    
    public static CausalModel generateBinaryTreeBenchmarkModel(int depth) throws InvalidCausalModelException {
        return generateBinaryTreeBenchmarkModel(depth, new FormulaFactory());
    }

    private static CausalModel generateBinaryTreeBenchmarkModel(int depth, FormulaFactory f)
            throws InvalidCausalModelException {
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
                Variable endogenousVariable = f.variable("n_" + rangeArray[i]);
                variablesInPreviousLevel.add(endogenousVariable);
                Equation equation = new Equation(endogenousVariable, exogenousVariables.get(i));
                equations.add(equation);
            }

            int count = numberOfNodes - numberOfLeaves - 1;
            for (int i = depth - 1; i >= 0; i--) {
                int numberOfNodesInCurrentLevel = ((int) Math.pow(2, i + 1) - 1 + 1) / 2;
                List<Variable> variablesInPreviousLevelNew = new ArrayList<>();
                for (int k = 0; k < numberOfNodesInCurrentLevel; k++) {
                    Variable endogenousVariable = f.variable("n_" + count--);
                    variablesInPreviousLevelNew.add(endogenousVariable);

                    Formula formula = f.or(variablesInPreviousLevel.get(2 * k),
                            variablesInPreviousLevel.get(2 * k + 1));
                    Equation equation = new Equation(endogenousVariable, formula);
                    equations.add(equation);
                }
                variablesInPreviousLevel = variablesInPreviousLevelNew;
            }
            CausalModel causalModel = new CausalModel(name, equations, new HashSet<>(exogenousVariables), f);
            return causalModel;
        } else {
            return null;
        }
    }
    
    
    private static CausalModel generateBinaryTreeBenchmarkModel(int depth, FormulaFactory f, String prefix)
            throws InvalidCausalModelException {
        String name = "BinaryTreeBenchmarkModel2";
        if (depth >= 0) {
            int numberOfNodes = (int) Math.pow(2, depth + 1) - 1;
            int numberOfLeaves = (numberOfNodes + 1) / 2; // no double needed; is always integer for full binary tree

            int[] rangeArray = IntStream.range(numberOfNodes - numberOfLeaves, numberOfNodes)
                    .map(i -> numberOfNodes - i + (numberOfNodes - numberOfLeaves) - 1) // reverse
                    .toArray();
            List<Variable> exogenousVariables = Arrays.stream(rangeArray).mapToObj(i -> f.variable(prefix+"_"+i + "_exo"))
                    .collect(Collectors.toList());

            Set<Equation> equations = new HashSet<>();
            List<Variable> variablesInPreviousLevel = new ArrayList<>();
            for (int i = 0; i < rangeArray.length; i++) {
                Variable endogenousVariable = f.variable(prefix+"_"+"n_" + rangeArray[i]);
                variablesInPreviousLevel.add(endogenousVariable);
                Equation equation = new Equation(endogenousVariable, exogenousVariables.get(i));
                equations.add(equation);
            }

            int count = numberOfNodes - numberOfLeaves - 1;
            for (int i = depth - 1; i >= 0; i--) {
                int numberOfNodesInCurrentLevel = ((int) Math.pow(2, i + 1) - 1 + 1) / 2;
                List<Variable> variablesInPreviousLevelNew = new ArrayList<>();
                for (int k = 0; k < numberOfNodesInCurrentLevel; k++) {
                    Variable endogenousVariable = f.variable(prefix+"_"+"n_" + count--);
                    variablesInPreviousLevelNew.add(endogenousVariable);

                    Formula formula = f.or(variablesInPreviousLevel.get(2 * k),
                            variablesInPreviousLevel.get(2 * k + 1));
                    Equation equation = new Equation(endogenousVariable, formula);
                    equations.add(equation);
                }
                variablesInPreviousLevel = variablesInPreviousLevelNew;
            }
            CausalModel causalModel = new CausalModel(name, equations, new HashSet<>(exogenousVariables), f);
            return causalModel;
        } else {
            return null;
        }
    }
    
    
    public static CausalModel generateCMFromJSON(String path)
            throws InvalidCausalModelException {
    	 Gson gson = new Gson();
         try (Reader reader = new FileReader(path)) {
             // Convert JSON File to Java Object
        	 NewModelData cm = gson.fromJson(reader, NewModelData.class);

			FormulaFactory f = new FormulaFactory();
			PropositionalParser p = new PropositionalParser(f);
         

         Set<Variable> exoVars = new HashSet<>();
         for (String exo : cm.exos) {
             exoVars.add(f.variable(exo));
         }

         Set<Equation> equations = new HashSet<>();
         for (EndoVar endo : cm.endos) {
             Variable v = f.variable(endo.name);
             try {
                 equations.add(new Equation(v, p.parse(endo.formula)));
             } catch (ParserException e) {
                 // TODO Auto-generated catch block
                 System.out.println("err");
                 e.printStackTrace();
             }
         }
         CausalModel causalModel = new CausalModel(cm.name, equations, exoVars, f);
             return causalModel;
         } catch (IOException e) {
             e.printStackTrace();
             return null;
         }

      
    }
    
    
    
    public static NumericCausalModel billySuzyNumeric() throws InvalidCausalModelException {
     		Argument BTExo = new Argument("BT_exo", 1.0);
     		Argument STExo = new Argument("ST_exo", 1.0);
     		// Example of Endos
     		Argument BT = new Argument("BT = BT_exo",BTExo);     
     		Argument ST = new Argument("XT = ST_exo",STExo);    
     		Argument SH = new Argument("SH = XT", ST);
     		Argument BH = new Argument("BH= BT-SH", BT,SH);   
     		Argument BS = new Argument("BS= BH + SH", BH,SH);  

        Set<Argument> equations = new HashSet<>(Arrays.asList(BT, ST, SH, BH, BS));
        Set<Argument> exogenousVariables = new HashSet<>(Arrays.asList(BTExo, STExo));

        NumericCausalModel causalModel = new NumericCausalModel("BillySuzyNumeric", equations, exogenousVariables);
        return causalModel;
        
        
    }
    
    public static NumericCausalModel Numeric_dummy_1() throws InvalidCausalModelException {
     		Argument XExo = new Argument("X_exo", 12.0);
     		Argument YExo = new Argument("Y_exo", 38.0);
     		// Example of Endos
     		Argument X = new Argument("X = X_exo",XExo);     
     		Argument Y = new Argument("Y = Y_exo",YExo);    
     		Argument Z = new Argument("Z = 3 * X + 5 * Y", X,Y);
     		Argument G = new Argument("G= Z - 5", Z);   
     		Argument H = new Argument("H= Y - G", Y,G);  

        Set<Argument> equations = new HashSet<>(Arrays.asList(X, Y, Z, G, H));
        Set<Argument> exogenousVariables = new HashSet<>(Arrays.asList(XExo, YExo));

        NumericCausalModel causalModel = new NumericCausalModel("Dummy_1Numeric", equations, exogenousVariables);
        return causalModel;
        
        
    }
    
    
    
    public static NumericCausalModel generateNumericTreeBenchmarkModel(int depth)
            throws InvalidCausalModelException {
        String name = "NumericBinaryTreeBenchmarkModel";
        if (depth >= 0) {
            int numberOfNodes = (int) Math.pow(2, depth + 1) - 1;
            int numberOfLeaves = (numberOfNodes + 1) / 2; // no double needed; is always integer for full binary tree

          
            
            int[] rangeArray = IntStream.range(numberOfNodes - numberOfLeaves, numberOfNodes)
                    .map(i -> numberOfNodes - i + (numberOfNodes - numberOfLeaves) - 1) // reverse
                    .toArray();
            
            Map<String, Argument> exogenousVariables = new HashMap<String, Argument>();
            exogenousVariables = Arrays.stream(rangeArray).mapToObj(i -> new Argument("exo_"+i , 1.0))
                    .collect(Collectors.toMap(Argument::getArgumentName, Argument::clone));
            
          
            Set<Argument> equations = new HashSet<>();
            List<Argument> variablesInPreviousLevel = new ArrayList<>();
            for (int i = 0; i < rangeArray.length; i++) {
                Argument endogenousVariable = new Argument("n_" + rangeArray[i]+"= exo_"+rangeArray[i], exogenousVariables.get("exo_"+rangeArray[i]));
                variablesInPreviousLevel.add(endogenousVariable);
                equations.add(endogenousVariable);
            }

            int count = numberOfNodes - numberOfLeaves - 1;
            for (int i = depth - 1; i >= 0; i--) {
                int numberOfNodesInCurrentLevel = ((int) Math.pow(2, i + 1) - 1 + 1) / 2;
                List<Argument> variablesInPreviousLevelNew = new ArrayList<>();
                for (int k = 0; k < numberOfNodesInCurrentLevel; k++) {
                	
                	String var = "n_" + count--;
                	Argument arg1 = variablesInPreviousLevel.get(2 * k);
                	Argument arg2 = variablesInPreviousLevel.get(2 * k+1);
                	
                    Argument endogenousVariable = new  Argument(var+ "= "+arg1.getArgumentName()+"+"+arg2.getArgumentName(), arg1,arg2);
                    
                    variablesInPreviousLevelNew.add(endogenousVariable);
                    equations.add(endogenousVariable);
                }
                variablesInPreviousLevel = variablesInPreviousLevelNew;
            }
            
            
    		 NumericCausalModel causalModel = new NumericCausalModel(name, equations, exogenousVariables.values().stream().collect(Collectors.toSet()));
            return causalModel;
        } else {
            return null;
        }
        
        
        
    }
    
    
    /** From https://rpubs.com/ezrasote/bostonhousing
     * @return
     * @throws InvalidCausalModelException
     */
    public static NumericCausalModel bostonHousingNumeric() throws InvalidCausalModelException {
//    	medv = -0.071 * crim + 5.58 * rm - 0.007 * tax  - 0.484 * lstat - 3.767 
// 	example: crim 0.00632	rm	6.575 tax 296		lstat 4.98	medv: 28.43873128
    	
 		Argument crim_exo = new Argument("crim_exo", 1.0);
 		Argument rm_exo = new Argument("rm_exo", 7.0);
 		Argument tax_exo = new Argument("tax_exo", 296);
 		Argument lstat_exo = new Argument("lstat_exo", 4.98);
 		// Example of Endos
 		Argument crim = new Argument("crim = crim_exo",crim_exo);     
 		Argument rm = new Argument("rm = rm_exo",rm_exo);    
 		Argument tax =  new Argument("tax = tax_exo",tax_exo); 
 		Argument lstat = new Argument("lstat = lstat_exo",lstat_exo); 
 		Argument medv = new Argument("medv =  -0.071 * crim + 5.58 * rm - 0.007 * tax  - 0.484 * lstat - 3.767 ", crim, rm,tax,lstat);

    Set<Argument> equations = new HashSet<>(Arrays.asList(crim, rm, tax, lstat, medv));
    Set<Argument> exogenousVariables = new HashSet<>(Arrays.asList(crim_exo, rm_exo,tax_exo,lstat_exo));

    NumericCausalModel causalModel = new NumericCausalModel("BoustonHousing", equations, exogenousVariables);
    return causalModel;
    
    
}
    
    /** from https://rpubs.com/ezrasote/bostonhousing and https://rpubs.com/ezrasote/crimerate
     * @return
     * @throws InvalidCausalModelException
     */
    public static NumericCausalModel bostonHousingNumericWithCrime() throws InvalidCausalModelException {
//    	medv = -0.071 * crim + 5.58 * rm - 0.007 * tax  - 0.484 * lstat - 3.767 
//    	crime_rate = 8.7238 * (percent_m) + 16.7212 * (mean_education) + 7.0635 * (unemploy_m39) + 
//    	7.0513 * (inequality) - 3863.7869 * (prob_prison) + 6.7957 * (pol_exp) - 4605.8496
 		Argument percent_m_exo = new Argument("percent_m_exo", 12.0);
 		Argument mean_education_exo = new Argument("mean_education_exo", 12.0);
 		Argument unemploy_m39_exo = new Argument("unemploy_m39exo", 12.0);
 		Argument inequality_exo = new Argument("inequality_exo", 12.0);
 		Argument prob_prison_exo = new Argument("prob_prison_exo", 12.0);
 		Argument pol_exp_exo = new Argument("pol_exp_exo", 12.0);
 		
 		Argument rm_exo = new Argument("rm_exo", 6.575);
 		Argument tax_exo = new Argument("tax_exo", 296);
 		Argument lstat_exo = new Argument("lstat_exo", 4.98);
 		// Example of Endos
 		Argument percent_m = new Argument("percent_m = percent_m_exo",percent_m_exo);   
 		Argument mean_education = new Argument("mean_education = mean_education_exo",mean_education_exo);   
 		Argument unemploy_m39 = new Argument("unemploy_m39 = unemploy_m39_exo",unemploy_m39_exo);   
 		Argument inequality = new Argument("inequality = inequality_exo",inequality_exo);   
 		Argument prob_prison = new Argument("prob_prison = prob_prison_exo",prob_prison_exo); 
 		Argument pol_exp = new Argument("pol_exp = pol_exp_exo",pol_exp_exo); 
 		
 		Argument crim = new Argument("crim = 8.7238 * percent_m + 16.7212 * mean_education + 7.0635 * unemploy_m39 + 7.0513 * inequality - 3863.7869 * prob_prison + 6.7957 * pol_exp - 4605.8496",percent_m,mean_education,unemploy_m39,inequality,prob_prison,pol_exp);     
 		Argument rm = new Argument("rm = rm_exo",rm_exo);    
 		Argument tax =  new Argument("tax = tax_exo",tax_exo); 
 		Argument lstat = new Argument("lstat = lstat_exo",lstat_exo); 
 		Argument medv = new Argument("medv =  -0.071 * crim + 5.58 * rm - 0.007 * tax  - 0.484 * lstat - 3.767 ", crim, rm,tax,lstat);

    Set<Argument> equations = new HashSet<>(Arrays.asList(percent_m,mean_education,unemploy_m39,inequality, prob_prison,pol_exp,crim, rm, tax, lstat, medv));
    Set<Argument> exogenousVariables = new HashSet<>(Arrays.asList(percent_m_exo,mean_education_exo,unemploy_m39_exo,inequality_exo,prob_prison_exo,pol_exp_exo, rm_exo,tax_exo,lstat_exo));

    NumericCausalModel causalModel = new NumericCausalModel("BoustonHousingCrimeRate", equations, exogenousVariables);
    return causalModel;
    
    
}


}
