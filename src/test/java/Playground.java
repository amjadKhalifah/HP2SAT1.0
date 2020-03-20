import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

public class Playground {
    @Ignore
    @Test
    public void assignmentAndSat() {
        FormulaFactory f = new FormulaFactory();
        Variable a = f.variable("A");
        Variable b = f.variable("B");
        Literal notC = f.literal("C", false);

        // A & ~(B | ~C); CNF: A & ~B & C => true for A=1, B=0, C=1
        Formula formula = f.and(a, f.not(f.or(b, notC)));
        // assign a positive literal for TRUE and a negative literal for FALSE
        Assignment assignment = new Assignment();
        assignment.addLiteral(a);
        assignment.addLiteral(b.negate());
        assignment.addLiteral(notC.negate());
        System.out.println(formula.evaluate(assignment)); // true

        // NNF and CNF
        Formula nnf = formula.nnf();
        Formula cnf = formula.cnf();

        // SAT
        final SATSolver miniSat = MiniSat.miniSat(f);
        miniSat.add(formula);
        final Tristate result = miniSat.sat();
    }

    
    @Test
    public void restrict() {
        FormulaFactory f = new FormulaFactory();
        Variable a = f.variable("A");
        Variable b = f.variable("B");
        Literal notC = f.literal("C", false);

        // A & ~(B | ~C); CNF: A & ~B & C => true for A=1, B=0, C=1
        Formula formula = f.or(a, f.and(f.or(b, notC)));
        // assign a positive literal for TRUE and a negative literal for FALSE
        Assignment assignment = new Assignment();
        assignment.addLiteral(a);
        assignment.addLiteral(b.negate());
        assignment.addLiteral(notC.negate());
        Iterator<Formula> iter = formula.cnf().iterator();
		while (iter.hasNext()) {
			 System.out.println("--"+iter.next());
		}
       
//        System.out.println(assignment.formula(f));
//        System.out.println(formula.restrict(assignment));
    }
    
    
}
