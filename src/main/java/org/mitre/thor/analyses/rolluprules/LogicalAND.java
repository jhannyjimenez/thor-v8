package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

class LogicalAND implements IARollRule {

    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        /*
        boolean allChildren100 = true;
        for(Node child : children){
            if(child.analysisDataHolders.get(rollUpIndex).operability != 100.0){
                allChildren100 = false;
                break;
            }
        }
        if(allChildren100){
            return 100.0;
        }else{
            return 0.0;
        }
         */

        double min = 9999;
        for(Node child : children){
            if(child.analysisDataHolders.get(rollUpIndex).operability < min){
                min = child.analysisDataHolders.get(rollUpIndex).operability;
            }
        }
        return min;
    }
}
