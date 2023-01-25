package org.mitre.thor.analyses.cg;

import javafx.util.Pair;
import org.mitre.thor.network.nodes.Node;

public class DescendingOrdering implements ICGOrdering{

    @Override
    public Pair<Double, Double> returnDerivatives(Node node, int rollUpI) {
        double op100 = node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[100];
        double closestOp90 = 0.0;
        int cOp90Index = 0;

        double closestDiff = Double.MAX_VALUE;
        Pair<Double, Double> derivatives;

        for (int a = 0; a < node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability.length; a++) {
            double diff = Math.abs(op100 - node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[a]);
            diff = 10.0 - diff;
            if (diff < closestDiff && a != 100) {
                closestDiff = diff;
                closestOp90 = node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[a];
                cOp90Index = a;
            }
        }
        double aD = (node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[100] - closestOp90) / (100.0 - cOp90Index);
        double eD = node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[100] - node.analysisDataHolders.get(rollUpI).coolGraphMissionOperability[99];
        derivatives = new Pair<>(aD, eD);

        return derivatives;
    }

    @Override
    public Double returnNodeScore(Node node, int maxDivs, int rollUpIndex) {
        double value = 0;
        for(int d = 0; d < maxDivs; d++){
            double nodeDivData = node.analysisDataHolders.get(rollUpIndex).sizePerDivision.get(d);
            value += (100 - nodeDivData) / Math.pow(100, maxDivs - (d + 1));
        }
        return value / 10;
    }
}
