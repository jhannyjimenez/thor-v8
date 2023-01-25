package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

class FDNA2 implements IARollRule {

    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        FDNA fdnaRule = new FDNA();
        double fdnaResult = fdnaRule.calculateNodeScoreFromChildren(node, network, children, rollUpIndex);
        if(!Double.isNaN(node.analysisDataHolders.get(rollUpIndex).operability)){
            return Math.min(fdnaResult, node.fdna2Value);
        }else{
            return fdnaResult;
        }
    }
}
