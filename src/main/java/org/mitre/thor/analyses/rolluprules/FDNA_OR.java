package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.links.ActivityLink;
import org.mitre.thor.network.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class FDNA_OR implements IARollRule {
    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        ArrayList<Double> pkj = new ArrayList<>();
        for(ActivityLink link : network.getActivityLinks()){
            if(link.parent == node  && children.contains(link.child)){
                double val = Math.min(link.SOD * link.child.analysisDataHolders.get(rollUpIndex).operability + 100 * (1 - link.SOD),
                        link.child.analysisDataHolders.get(rollUpIndex).operability + link.COD);
                pkj.add(val);
            }
        }
        double max = 0;
        for(Double val : pkj){
            if(val > max){
                max = val;
            }
        }
        return max;
    }
}
