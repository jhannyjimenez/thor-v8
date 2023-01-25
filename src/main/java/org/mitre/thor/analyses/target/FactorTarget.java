package org.mitre.thor.analyses.target;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.ArrayList;

public class FactorTarget implements ITarget{

    @Override
    public ArrayList<Node> getTargetNodes(Network network) {
        return network.filterReal(new ArrayList<>(network.getFactors()), false);
    }
}
