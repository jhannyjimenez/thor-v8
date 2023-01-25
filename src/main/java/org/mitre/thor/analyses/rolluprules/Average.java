package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

public class Average implements IARollRule{
    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        double sum = 0;
        int count = 0;
        for(Node child : children){
            sum += child.analysisDataHolders.get(rollUpIndex).operability;
            count++;
        }
        if (count != 0){
            return sum/count;
        }else{
            return 0.0;
        }
    }
}
