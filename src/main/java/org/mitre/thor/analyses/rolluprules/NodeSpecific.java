package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

class NodeSpecific implements IARollRule {

    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        return 100.0;
    }
}
