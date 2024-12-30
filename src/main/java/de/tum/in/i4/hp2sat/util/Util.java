package de.tum.in.i4.hp2sat.util;


import com.google.common.collect.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.logicng.formulas.Literal;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Util<T> {
    public List<Set<T>> generatePowerSet(Set<T> set) {
        List<Set<T>> powerSet;
        if (set.size() <= 30) {
            powerSet = Sets.powerSet(set).stream()
                    .sorted(Comparator.comparingInt(Set::size))
                    .collect(Collectors.toList());
        } else {
            powerSet = new UnifiedSet<>(set).powerSet()
                    .stream().map(s -> s.toImmutable().castToSet())
                    .sorted(Comparator.comparingInt(Set::size))
                    .collect(Collectors.toList());
        }
        return powerSet;
    }

    public static Graph reverseGraph(Graph graph) {
        Graph graphReversed = new SingleGraph(graph.getId() + "_reversed");
        graph.nodes().forEach(n -> graphReversed.addNode(n.getId()));
        // switch source and target
        graph.edges().forEach(e -> graphReversed.addEdge(e.getId(), e.getTargetNode().getId(),
                e.getSourceNode().getId(), true));
        return graphReversed;
    }

    public static Set<Argument> generateRandomCauseSet(NumericCausalModel causalModel, int numberofcauses) {
    	
    	        Set<Argument> causes = new LinkedHashSet();     
    	        Random r = new Random();
    	        
    	        for (int i =0; i<numberofcauses; i++) {
    	        	int index = r.nextInt(causalModel.getEquationsSorted().size());
    	        	 Argument e = causalModel.getEquationsSorted().get(index);
    	        	if (!e.getArgumentName().equals("n_0"))	     // reserved for phi   	 
    	        		causes.add(new Argument(e.getArgumentName(), e.getArgumentValue()));
    	        	else {
    	        		 Argument e2 = causalModel.getEquationsSorted().get(index--);
    	        		 causes.add(new Argument(e2.getArgumentName(), e2.getArgumentValue()));
    	        	}
    	        }
    	    	return causes;
    }
    
    public static Expression getPhiExpression (NumericCausalModel causalModel) {
    	 double rootValue = causalModel.getVaribale("n_0").getArgumentValue();
	    	Expression phi;
	        if (rootValue<0) {
	        	 phi = new Expression("n_0+"+(-1*rootValue)+"= 0", causalModel.getVaribale("n_0"));
	        }
	        else {
	        	 phi = new Expression("n_0-"+rootValue+"= 0", causalModel.getVaribale("n_0"));
	        }
    	return phi;
    }
    
    public static Expression getContrastivePhiExpression (NumericCausalModel causalModel) {
   	 double rootValue = causalModel.getVaribale("n_0").getArgumentValue();
	    	Expression phi2;
	        if (rootValue<0) {
	        	 phi2 = new Expression("n_0+"+((-1*rootValue)+100)+"!= 0", causalModel.getVaribale("n_0")); 
	        }
	        else {
	        	 phi2 = new Expression("n_0-"+(rootValue+100)+"!= 0", causalModel.getVaribale("n_0")); 
	        }
	       return phi2;
   	
   	
   	
   }
}
