package org.mitre.thor.analyses.crit;

import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.Input;
import org.mitre.thor.input.InputQueue;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;

public class PhiRunnable implements Runnable{

    public final BigInteger startIndex;
    public final BigInteger endIndex;
    public final double maxMinutes;
    public List<InputQueue> inputQueues;
    public final Input input;
    public Network network;

    private final PhiCalculationEnum phiCalculationMethod;

    public PhiRunnable(Input input, PhiCalculationEnum phiCalculationMethod, List<InputQueue> inputQueues, double maxMinutes, BigInteger startIndex, BigInteger endIndex){
        this.input = input;
        this.phiCalculationMethod = phiCalculationMethod;
        this.inputQueues = inputQueues;
        this.maxMinutes = maxMinutes;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public void run() {
        try {
            network = (Network) input.network.clone();
            network.addNetworkAnalysisDataHolders(true);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        phiCalculationMethod.run(this);
        input.synchronizeThreads(network);

        CoreLogger.logger.log(Level.INFO, "Thread runnable complete");
    }

    public void updatePhiCounts(double phi, int rollUpIndex, int trial){
        if(!Double.isNaN(phi)){
            if(network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].phiSum == Double.MAX_VALUE - 1.0){
                network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].phiSum /= 2;
                network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].phiSumDivisionCounts++;
            }
            network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].phiSum += phi;

            for(int i = 0; i < network.getNodes().size(); i++){
                if(network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON == Double.MAX_VALUE - 1.0){
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON /= 2;
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF /= 2;
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount++;
                }
                if(network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF == Double.MAX_VALUE - 1.0){
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF /= 2;
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON /= 2;
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount++;
                }
                if(network.getNodes().get(i).isOn){
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON += phi;
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].onCount++;
                }else{
                    network.getNodes().get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF += phi;
                }
            }
            network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount.add(BigInteger.ONE);
        }
    }

    public void updateEMatrix(List<Node> nodes, int rollUpIndex, int trial) {
        /*
        if(network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].E == null){
            network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].E = new SimpleMatrix(nodes.size() + 1, nodes.size() + 1);
        }

        for(int row = 0; row < nodes.size() + 1; row++){
            for(int col = 0; col < nodes.size() + 1; col++){
                double prevVal = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].E.get(row, col);
                int x_n = 1;
                int x_m = 1;
                if(col != nodes.size()){
                    x_n = nodes.get(col).isOn ? 1 : 0;
                }
                if(row != nodes.size()){
                    x_m = nodes.get(row).isOn ? 1 : 0;
                }
                network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].E.set(row, col, prevVal + x_n * x_m);
            }
        }
         */
    }
}
