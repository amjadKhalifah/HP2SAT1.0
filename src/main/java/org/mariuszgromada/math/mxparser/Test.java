package org.mariuszgromada.math.mxparser;

import java.util.List;

import org.mariuszgromada.math.mxparser.parsertokens.Operator;
import org.mariuszgromada.math.mxparser.parsertokens.ParserSymbol;
import org.mariuszgromada.math.mxparser.parsertokens.Token;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Test {
	
	
	private static final String DISTANCE_VAR_NAME = "res";
	private static final String IND_1_PREFIX = "IND1_";
	private static final String IND_2_PREFIX = "IND2_";
	private static final String SLACK_1_PREFIX = "SLA1_";
	private static final String SLACK_2_PREFIX = "SLA2_";


	public static void main(String[] args) {
		
		// Examples of Exos
		Argument z = new Argument("z", 3.2);
		Argument n = new Argument("n", 4);
//		         
		// Example of Endos
		Argument x = new Argument("x = 5 * z",z);     
		Argument y = new Argument("y = x - 3 * n + (z + 5)", n,z,x);
		Argument x1 = new Argument("x1= y + 100", y);   
		
		
		
		
//		System.out.println(y.argumentExpression);
		Expression e = new Expression("z=3.2+8", z);
		mXparser.consolePrintTokens(e.getCopyOfInitialTokens());
		//
	
//		mXparser.consolePrintln("Res 4: " + e.getExpressionString() + " = " + e.));

	
	
		}

	
	// this function can be used for representing the linear equalities of variables. We use this 
	// for Phi vars and the first part of the disjunctive constraints
	// Examples OF fX in mind:  X + y +4 ;-x - y- 4;3*Y + 5*x -40; -3 *Y - 5*X + 40 ; 3 + Z; 400
	public static void toLinearEquality (Argument variable, Expression equation, boolean isDisjunctive,GRBModel model, boolean addSlack) throws GRBException {
		// we need to turn the equation to the equality: Fx - V = 0 
		
		List<Token> tokens = equation.getCopyOfInitialTokens();
		Double currentCoefficient = null;
		double sign = 1.0; // 1.0 or -1.0 
		GRBLinExpr expr= null;
		GRBVar currentArgument = null;
		for (int i = 0; i < tokens.size(); i++) {
			 expr = new GRBLinExpr();
			Token token = tokens.get(i);
			// loop the tokens if tokentypeid= 101 which is an argument; 
			// then get the Gurobi variable of this;
			// each term is an operator, coefficient, and an argument. A term could also be a constant 
			if (token.tokenTypeId == Argument.TYPE_ID) {
				currentArgument = model.getVarByName(token.tokenStr);
			} else if (token.tokenTypeId == ParserSymbol.NUMBER_TYPE_ID) {
				// if it was type id = 0 then it is a 
				//number that can be a coefficient or a constant in an equation 
				currentCoefficient = token.tokenValue;
				if (i==tokens.size()-1) {// if this is the last element (number)
					expr.addConstant(currentCoefficient*sign);
				}
			}
			else if (token.tokenTypeId == Operator.TYPE_ID) {// + - * /...
				// if it was 1 then it is an operator 
				if ((token.tokenId == Operator.PLUS_ID) || (token.tokenId == Operator.MINUS_ID) ) {
					// when i get a +- not at the beginning i add the previous Term or constant, and use the operator as sign for next
					if ( i != 0 ) { 
						if (currentArgument!= null)
							expr.addTerm(currentCoefficient*sign, currentArgument);
						else if (currentCoefficient!=null)
							expr.addConstant(currentCoefficient*sign);
					}
					// set next term sign
					sign = (token.tokenId == Operator.PLUS_ID)? 1.0: -1.0;
					currentCoefficient= null;
					currentArgument = null;
				}else {

					// this is a *; should we do anything
				}



			}
			else if (token.NOT_MATCHED==1) { // TODO handle unmatched tokens

			}
		}

		// The expression so far simulates Fx; Now Fx-V = 0 
		expr.addTerm(-1.0, model.getVarByName(variable.getArgumentName()));
		
		if (addSlack) {
			expr.addTerm(1.0, model.getVarByName(SLACK_1_PREFIX+variable.getArgumentName()));
			// the constraints of the slack and the indicator vars are already added; this would be another location to add them
			// Now lets add the second constraint to account fow W; V - v + w2 = 0
			GRBLinExpr contingencyExpression = new GRBLinExpr();
			contingencyExpression.addTerm(1, model.getVarByName(variable.getArgumentName()));
			contingencyExpression.addConstant(-variable.getArgumentValue());
			contingencyExpression.addTerm(1,model.getVarByName(SLACK_2_PREFIX+variable.getArgumentName()));
			
			model.addConstr(contingencyExpression, GRB.EQUAL, 0.0, "R_2_" +variable.getArgumentName());
		}
		
		// finalize the constraint and add it 
		model.addConstr(expr, GRB.EQUAL, 0.0, "R_" +variable.getArgumentName());
	}
	}


