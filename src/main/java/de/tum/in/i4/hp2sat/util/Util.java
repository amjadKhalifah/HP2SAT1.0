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
                e.getSourceNode().getId(),true));
        return graphReversed;
    }
}
