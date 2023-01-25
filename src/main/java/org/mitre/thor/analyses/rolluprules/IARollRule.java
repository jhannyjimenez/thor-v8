package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Factor;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

interface IARollRule {
    //Calculates the score of the node based on the operability of node's activity children
    double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex);

    default double getFactorScore(Factor factor){
        if(factor.isOn){
            return 100.0;
        }else{
            return 0.0;
        }
    }
}
