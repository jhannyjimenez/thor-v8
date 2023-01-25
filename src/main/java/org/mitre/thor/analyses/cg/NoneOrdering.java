package org.mitre.thor.analyses.cg;

import javafx.util.Pair;
import org.mitre.thor.network.nodes.Node;

public class NoneOrdering implements ICGOrdering{

    @Override
    public Pair<Double, Double> returnDerivatives(Node node, int rollUpI) {
        return new Pair<>(0.0, 0.0);
    }

    @Override
    public Double returnNodeScore(Node node, int maxDivs, int rollUpIndex) {
        return 0.0;
    }
}
