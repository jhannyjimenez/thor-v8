package org.mitre.thor.analyses.cg;

import javafx.util.Pair;
import org.mitre.thor.network.nodes.Node;

public interface ICGOrdering {

    Pair<Double, Double> returnDerivatives(Node node, int rollUpI);

    Double returnNodeScore(Node node, int maxDivs, int rollUpIndex);
}
