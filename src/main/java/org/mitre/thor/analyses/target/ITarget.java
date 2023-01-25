package org.mitre.thor.analyses.target;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.ArrayList;

public interface ITarget {

    public ArrayList<Node> getTargetNodes(Network network);

}
