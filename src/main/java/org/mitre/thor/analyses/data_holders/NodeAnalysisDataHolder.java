package org.mitre.thor.analyses.data_holders;

import org.mitre.thor.analyses.Analysis;
import org.mitre.thor.input.InputQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NodeAnalysisDataHolder implements Cloneable{
    public InputQueue queue;
    public List<Analysis> targetAnalyses;

    //ALL
    public double operability = Double.NaN;

    //CRITICALITY ANALYSIS
    public double colorScore = Double.NaN;
    public double alpha = Double.NaN;
    public NodeCriticalityTrial[] nodeCriticalityTrials;

    //CG
    public double[] coolGraphMissionOperability = new double[101];
    public ArrayList<Integer> sizePerDivision = new ArrayList<>();
    public double cgSCore = 0.0;
    public double normalCgScore = 0.0;

    //OPERABILITY
    public double[] operabilityPerTime;
    public double[] sePerTime;

    public NodeAnalysisDataHolder(InputQueue queue, List<Analysis> targetAnalyses){
        this.queue = queue;
        this.targetAnalyses = targetAnalyses;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return queue.rollUpRule + " : " + targetAnalyses.toString();
    }
}