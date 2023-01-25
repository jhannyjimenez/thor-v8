package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.links.ActivityLink;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

class FDNA implements IARollRule {

    // NOTE: THIS WILL RETURN 100 IF 'node' = GROUP and 'children' IS A COLLECTION OF FACTORS
    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        int numberOfChildren = children.size();

        double P0J;
        double firstSum = 0, secondSum = 0;
        int linksParentCounter = 0;
        for(ActivityLink link : network.getActivityLinks()){
            if(link.parent == node && children.contains(link.child)){
                firstSum += (link.SOD / numberOfChildren) * link.child.analysisDataHolders.get(rollUpIndex).operability;
                secondSum += link.SOD / numberOfChildren;
                linksParentCounter++;
            }
        }
        P0J = firstSum + 100 * (1 - secondSum);


        double[] PIJ = new double[linksParentCounter];
        int i = 0;
        for(ActivityLink link : network.getActivityLinks()){
            if(link.parent == node && children.contains(link.child)){
                PIJ[i] = link.child.analysisDataHolders.get(rollUpIndex).operability + link.COD;
                i++;
            }
        }
        double minPIJ = Double.MAX_VALUE;
        for(double pij : PIJ){
            if(pij < minPIJ){
                minPIJ = pij;
            }
        }
        return Math.min(P0J, minPIJ);
    }
}
