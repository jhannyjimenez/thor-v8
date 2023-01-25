package org.mitre.thor.analyses.target;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.ArrayList;

public enum TargetType {
    NODES(new NodeTarget()),
    FACTORS(new FactorTarget());

    private final ITarget target;

    TargetType(ITarget target){
        this.target = target;
    }

    public ArrayList<Node> getTargetNodes(Network network){
        return target.getTargetNodes(network);
    }
}
