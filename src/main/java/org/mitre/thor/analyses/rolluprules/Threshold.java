package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

public class Threshold implements IARollRule{
    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        double operability = 100.0;
        for(Node child : children){
            if(child.analysisDataHolders.get(rollUpIndex).operability == 0){
                operability = 0;
                break;
            }
        }

        return operability;
    }
}
