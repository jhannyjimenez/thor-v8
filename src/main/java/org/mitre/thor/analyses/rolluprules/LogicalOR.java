package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

class LogicalOR implements IARollRule {

    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        /*
        boolean oneChild100 = false;
        for(Node child : children){
            if(child.analysisDataHolders.get(rollUpIndex).operability == 100.0){
                oneChild100 = true;
                break;
            }
        }
        if(oneChild100){
            return 100.0;
        }else{
            return 0.0;
        }
         */

        double max = -9999;
        for(Node child : children){
            if(child.analysisDataHolders.get(rollUpIndex).operability > max){
                max = child.analysisDataHolders.get(rollUpIndex).operability;
            }
        }
        return max;
    }
}
