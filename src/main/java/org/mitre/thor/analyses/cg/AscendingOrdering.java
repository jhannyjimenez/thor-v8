package org.mitre.thor.analyses.cg;

import javafx.util.Pair;
import org.mitre.thor.network.nodes.Node;


public class AscendingOrdering implements ICGOrdering{

    @Override
    public Pair<Double, Double> returnDerivatives(Node node, int rollUpI) {
        double op0 = node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[0];
        double closestOp10 = 0.0;
        int cOp10Index = 0;

        double closestDiff = Double.MAX_VALUE;
        Pair<Double, Double> derivatives;

        for(int a = 0; a < node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability.length; a++){
            double diff = Math.abs(node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[a] - op0);
            diff = 10.0 - diff;
            if(diff < closestDiff && a != 0){
                closestDiff = diff;
                closestOp10 = node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[a];
                cOp10Index = a;
            }
        }
        double aD = (closestOp10 - node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[0]) / (cOp10Index - 0.0);
        double eD = node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[1] - node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[0];
        derivatives = new Pair<>(aD, eD);

        return derivatives;
    }

    @Override
    public Double returnNodeScore(Node node, int maxDivs, int rollUpIndex) {
        double value = 0;
        for(int d = 0; d < maxDivs; d++){
            double nodeDivData = node.analysisDataHolders.get(rollUpIndex).sizePerDivision.get(d);
            value += nodeDivData / Math.pow(100, d);
        }
        return value / 10;
    }
}
