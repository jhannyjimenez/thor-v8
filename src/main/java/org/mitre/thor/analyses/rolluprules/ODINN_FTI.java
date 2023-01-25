package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.links.ActivityLink;
import org.mitre.thor.network.links.FactorLink;
import org.mitre.thor.network.nodes.Factor;
import org.mitre.thor.network.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class ODINN_FTI implements IARollRule {
    @Override
    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex) {
        boolean setZero = false;
        int factorsSize = 0;
        if(Double.isNaN(node.SE)){
            double seN = 0;
            double seD = 0;
            ArrayList<Node> gChildren = new ArrayList<>(network.getGroupChildren(node, "factors"));
            for(FactorLink fl : network.getFactorLinks()){
                if(fl.parent == node || gChildren.contains(fl.parent)){

                    if(Double.isNaN(fl.child.analysisDataHolders.get(rollUpIndex).operability)){
                        fl.child.setOperability(rollUpIndex, this.getFactorScore((Factor) fl.child));
                    }
                    //FACTOR OPERABILITY = FACTOR HEALTH
                    if(fl.binary && fl.child.analysisDataHolders.get(rollUpIndex).operability == 0.0){
                        setZero = true;
                        break;
                    }
                    //FACTOR OPERABILITY = FACTOR HEALTH
                    seN += fl.fvi * fl.child.analysisDataHolders.get(rollUpIndex).operability;
                    seD += fl.fvi;
                    factorsSize++;
                }
            }

            if(setZero){
                node.SE = 0.0;
            }else{
                node.SE = seN/seD;
            }

            //TODO: Speak with Les about this
            if(!setZero && (Double.isNaN(node.SE) || factorsSize == 0)){
                node.SE = 100.0;
            }
        }

        int numChildren = children.size();
        if(numChildren == 0){
            return node.SE;
        } else {
            double sum1 = 0.0;
            for (ActivityLink l : network.getActivityLinks()) {
                if (l.parent == node && children.contains(l.child)) {
                    double Oi = l.child.analysisDataHolders.get(rollUpIndex).operability;
                    double se_ij_star = 100 * (1 + l.SOD) - l.COD;
                    double m_ij;
                    if (node.SE > se_ij_star) {
                        m_ij = 1;
                    } else {
                        m_ij = node.SE / Math.min(100, se_ij_star);
                    }
                    sum1 += m_ij * l.SOD * (Oi - 100) + node.SE;
                }
            }

            double arg1 = sum1 / numChildren;
            double arg2 = Double.POSITIVE_INFINITY;
            for (ActivityLink l : network.getActivityLinks()) {
                if (l.parent == node && children.contains(l.child)) {
                    double Oi = l.child.analysisDataHolders.get(rollUpIndex).operability;
                    double W_ij = 0;
                    if (numChildren == 1) {
                        W_ij = 1;
                    } else {
                        for (Node k : children) {
                            if (k != l.child) {
                                W_ij += k.analysisDataHolders.get(rollUpIndex).operability;
                            }
                        }
                        W_ij = W_ij / (100 * (numChildren - 1));
                    }
                    double val = (100 / l.IOD) * Oi + (100 - l.COD) * (Math.pow(W_ij, 0.1));
                    if (val < arg2) {
                        arg2 = val;
                    }
                }
            }
            return Math.min(arg1, arg2);
        }
    }
}
