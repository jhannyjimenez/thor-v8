package org.mitre.thor.analyses.data_holders;

import org.mitre.thor.input.InputQueue;
import org.mitre.thor.network.attack.AttackPoint;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.attack.AttackTreeBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class NetworkAnalysisDataHolder implements Cloneable{
    public InputQueue queue;
    public NetworkCriticalityTrial[] networkCriticalityTrials;
    public Network[] trialNetworks;

    public int tableRowCount = 0;
    public ArrayList<ArrayList<Integer>> possibilitiesTable = new ArrayList<>();
    public ArrayList<Double> endResults = new ArrayList<>();

    public HashMap<Integer, HashMap<Integer, Double>[]> cgRunMap = new HashMap<>();

    public AttackTreeBuilder attackTreeBuilder;

    public NetworkAnalysisDataHolder(InputQueue queue){
        this.queue = queue;
    }

    @Override
    public String toString() {
        return queue.rollUpRule.toString();
    }

    @Override
    public NetworkAnalysisDataHolder clone() {
        try {
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return (NetworkAnalysisDataHolder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}