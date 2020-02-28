package de.tum.in.i4.hp2sat.causality;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.logicng.formulas.*;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import de.tum.in.i4.hp2sat.causality.CausalModel;
import de.tum.in.i4.hp2sat.exceptions.InvalidCausalModelException;
import de.tum.in.i4.hp2sat.util.ExampleProvider;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Playground {

	@Test
	public void assignmentAndSat() throws ParserException, GRBException, InvalidCausalModelException {
		FormulaFactory f = new FormulaFactory();

		PropositionalParser pp = new PropositionalParser(f);
		Formula formula = pp.parse(
				"~BS & ST_exo & BT_exo & (BS | (BS <=> SH | BH)) & (XT | ~XT ) & (~BT | BT ) & (SH | (SH <=> XT)) & (~BH | (BH <=> BT & ~SH))");
		// ~BS & ST_exo & BT_exo & (BS | (BS <=> SH | BH)) & (ST | ~ST ) & (~BT
		// | BT ) & (SH | (SH <=> ST)) & (~BH | (BH <=> BT & ~SH))
		// TODO should be changed to the cause
		List<String> causes = Arrays.asList("XT", "BT");
		GRBEnv env = new GRBEnv("mip1.log");
		GRBModel model = new GRBModel(env);
		CausalModel cm = ExampleProvider.billySuzy();
		addLPVars(cm, model);
		// 1>= res <= size of causes set
		GRBVar res = model.addVar(1.0, causes.size(), 0.0, GRB.INTEGER, "res");
		// Set objective: min res
		GRBLinExpr expr = new GRBLinExpr();
		expr.addTerm(1.0, res);
		model.setObjective(expr, GRB.MINIMIZE);

		// Apparently should call this method so that the model is in sync
		model.update();
		// this should be the fastest way to loop
		// add constraints
		Iterator<Formula> iter = formula.cnf().iterator();
		while (iter.hasNext()) {
			// one clause in the cnf
			Formula clause = iter.next();
			if (clause.isAtomicFormula()) { // Assignment
				// R118: BS = 0
				Literal literal = (Literal) clause;
				// System.out.println(literal+" ATOMIC:
				// "+literal.isAtomicFormula()+"
				// "+literal.iterator().hasNext()+" "+ (literal.name())+"
				// "+(literal.phase()? 1.0: 0.0));
				expr = new GRBLinExpr();
				expr.addTerm(1.0, model.getVarByName(literal.name()));
				model.addConstr(expr, GRB.EQUAL, literal.phase() ? 1.0 : 0.0, "R_" + literal);
			} else {
				Iterator<Formula> literals = clause.iterator();
				expr = new GRBLinExpr();
				while (literals.hasNext()) {
					Literal literal = (Literal) literals.next();
					// System.out.println(" "+literal+" ATOMIC: "+" "+
					// literal.name()+" "+ literal.phase() );
					// e.g R140: XT + NXT >= 1
					if (literal.phase()) {
						expr.addTerm(1.0, model.getVarByName(literal.name()));
					} else {
						expr.addTerm(-1.0, model.getVarByName(literal.name()));
						expr.addConstant(1.0);
					}
				}
				model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "R_" + clause);
			}

		}

		// res+ what was 1 - what was zero = the sum of all what was
		// res = segma Di Di = {1-x xact = 1 x xact=0}
		// Rres: res = 1- BT + 1- XT {BTact,XTact=1}

		Set<Literal> evaluation = new HashSet<>(Arrays.asList(f.literal("BT", true), f.literal("XT", true)));
		expr = new GRBLinExpr();
		// TODO should check what to change it too, this should be the causes
		// and their actual values
		Iterator<Literal> causesItr = evaluation.iterator();
		while (causesItr.hasNext()) {
			Literal literal = causesItr.next();
			if (literal.phase()) {
				expr.addConstant(1.0);
				expr.addTerm(-1.0, model.getVarByName(literal.name()));
			} else {
				expr.addTerm(1.0, model.getVarByName(literal.name()));
			}
		}

		model.addConstr(res, GRB.EQUAL, expr, "R_result");
		// Optimize model
		model.optimize();

		GRBVar[] fvars = model.getVars();
		double[] x = model.get(GRB.DoubleAttr.X, fvars);
		String[] vnames = model.get(GRB.StringAttr.VarName, fvars);

		for (int j = 0; j < fvars.length; j++) {
			if (x[j] != 0.0) {
				System.out.println(vnames[j] + " " + x[j]);
			}
		}

		// Dispose of model and environment
		model.write("./ptest.lp");
		model.dispose();
		env.dispose();
	}

	// @Test
	// TODO turn this into a test case
	public void createLP() {
		try {
			GRBEnv env = new GRBEnv("mip1.log");
			GRBModel model = new GRBModel(env);

			// Create variables
			// The first and second arguments to the addVar() call are the
			// variable lower and upper bounds,
			// respectively. The third argument is the linear objective
			// coefficient (zero here - we’ll set the objective
			// later). The fourth argument is the variable type
			// somehow the variable declaration is affecting the result.
			GRBVar ST_EXO = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "ST_EXO");
			GRBVar BT_EXO = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "BT_EXO");

			GRBVar XT = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "XT");
			GRBVar BT = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "BT");

			GRBVar SH = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "SH");
			GRBVar BH = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "BH");
			GRBVar BS = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "BS");

			GRBVar res = model.addVar(0.0, 2.0, 0.0, GRB.INTEGER, "res");
			// Set objective: min res
			GRBLinExpr expr = new GRBLinExpr();
			expr.addTerm(1.0, res);
			model.setObjective(expr, GRB.MINIMIZE);

			// R118: BS = 0
			expr = new GRBLinExpr();
			expr.addTerm(1.0, BS);
			model.addConstr(expr, GRB.EQUAL, 0.0, "R118");
			// R119: ST_exo = 1
			expr = new GRBLinExpr();
			expr.addTerm(1.0, ST_EXO);
			model.addConstr(expr, GRB.EQUAL, 1.0, "R119");
			// R120: BT_exo = 1
			expr = new GRBLinExpr();
			expr.addTerm(1.0, BT_EXO);
			model.addConstr(expr, GRB.EQUAL, 1.0, "R120");

			// R140: XT + NXT >= 1
			expr = new GRBLinExpr();
			expr.addTerm(1.0, XT);
			expr.addTerm(-1.0, XT);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "R140");
			// R141: BT - BT >= 0
			expr = new GRBLinExpr();
			expr.addTerm(1.0, BT);
			expr.addTerm(-1.0, BT);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "R141");
			// R122: SH - XT >= 0
			expr = new GRBLinExpr();
			expr.addTerm(1.0, SH);
			expr.addTerm(-1.0, XT);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "R122");
			// R124: BT - BH >= 0
			expr = new GRBLinExpr();
			expr.addTerm(1.0, BT);
			expr.addTerm(-1.0, BH);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "R124");
			// R125: - BT - BH >= -1
			expr = new GRBLinExpr();
			expr.addTerm(-1.0, BT);
			expr.addTerm(-1.0, BH);
			model.addConstr(expr, GRB.GREATER_EQUAL, -1.0, "R125");

			// R126: BS - SH >= 0
			expr = new GRBLinExpr();
			expr.addTerm(1.0, BS);
			expr.addTerm(-1.0, SH);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "R126");
			// R127: BS - BH >= 0
			expr = new GRBLinExpr();
			expr.addTerm(1.0, BS);
			expr.addTerm(-1.0, BH);
			model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "R127");

			// Rres: res + BT + XT >= 2
			expr = new GRBLinExpr();
			expr.addTerm(1.0, res);
			expr.addTerm(1.0, BT);
			expr.addTerm(1.0, XT);
			model.addConstr(expr, GRB.EQUAL, 2.0, "Rres");
			// Optimize model
			// res2: res >= 1
			expr = new GRBLinExpr();
			expr.addTerm(1.0, res);
			model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "Rres2");
			model.optimize();

			GRBVar[] fvars = model.getVars();
			double[] x = model.get(GRB.DoubleAttr.X, fvars);
			String[] vnames = model.get(GRB.StringAttr.VarName, fvars);

			for (int j = 0; j < fvars.length; j++) {
				if (x[j] != 0.0) {
					System.out.println(vnames[j] + " " + x[j]);
				}
			}

			// System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

			// Dispose of model and environment
			model.write("./ptest.lp");
			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
		}
	}

	/**
	 * a function that turns a custom causal model to Gurobi causal model the
	 * location should be changed to inside the causal model for example 
	 * TODO check if we can do this with addvars
	 * 
	 * @param cm
	 * @param model
	 */
	public void addLPVars(CausalModel cm, GRBModel model) throws GRBException {
		// start with exo vars

		cm.getExogenousVariables().forEach(item -> {
			try {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, item.name());

			} catch (GRBException e) {
				e.printStackTrace();
			}
		});

		cm.getEquationsSorted().forEach(e -> {
			try {
				model.addVar(0.0, 1.0, 0.0, GRB.BINARY, e.getVariable().name());
			} catch (GRBException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
	}

	/**
	 * a function that converts CNF clauses to GRB constraints
	 * 
	 * @param fm
	 * @param model
	 */
	public void addLPConstraints(Formula fm, GRBModel model) throws GRBException {

	}
}
