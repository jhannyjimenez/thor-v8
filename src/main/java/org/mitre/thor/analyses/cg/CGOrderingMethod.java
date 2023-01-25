package org.mitre.thor.analyses.cg;

import javafx.util.Pair;
import org.mitre.thor.network.nodes.Node;

public enum CGOrderingMethod {
    NONE(new NoneOrdering()),
    ASCENDING(new AscendingOrdering()),
    DESCENDING(new DescendingOrdering());

    private final ICGOrdering ordering;

    CGOrderingMethod(ICGOrdering ordering){this.ordering = ordering;}

    public Pair<Double, Double> getDerivatives(Node node, int rollUpI){
        return ordering.returnDerivatives(node, rollUpI);
    }

    public Double getNodeScore(Node node, int maxDivs, int rollUpIndex){
        return ordering.returnNodeScore(node, maxDivs, rollUpIndex);
    }
}
