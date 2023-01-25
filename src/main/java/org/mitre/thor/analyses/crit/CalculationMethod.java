package org.mitre.thor.analyses.crit;

import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.CriticalityAnalysis;
import org.mitre.thor.analyses.data_holders.NetworkAnalysisDataHolder;
import org.mitre.thor.analyses.data_holders.NodeAnalysisDataHolder;
import org.mitre.thor.input.Input;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Group;
import org.mitre.thor.network.nodes.Node;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public abstract class CalculationMethod {

    public void run(PhiRunnable runnable) {
        //set up before loop
        Instant startInstant = Instant.now();
        BigInteger index = runnable.startIndex;
        boolean condition = checkCustomCondition(runnable, index);

        for(NetworkAnalysisDataHolder data : runnable.network.analysisDataHolders){
            if(data.queue.containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                for(Network network : data.trialNetworks){
                    network.networkOrder.clear();
                    for (Activity activity : network.getActivities()) {
                        activity.isOn = true;
                    }
                    network.clearIteratedNodes();
                    network.findNetworkOrder(network.startActivity);
                }
            }
        }

        //calculation loop
        while (condition){
            //For each rule: get the operabilities of all nodes if the start node is on
            for(int i = 0; i < runnable.inputQueues.size(); i++){
                if(runnable.inputQueues.get(i).containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                    CriticalityAnalysis analysis = (CriticalityAnalysis) runnable.inputQueues.get(i).getAnalysis(AnalysesForm.CRITICALITY);

                    for(int a = 0; a < analysis.trials; a++){
                        Network network = runnable.network.analysisDataHolders.get(i).trialNetworks[a];

                        //Decide what nodes to turn off and on
                        ArrayList<Node> nodes = analysis.targetType.getTargetNodes(network);

                        network.resetOperabilities(i);
                        setNetworkStatus(network, nodes, runnable, index);

                        ArrayList<Group> groups = network.getGroups("crit");

                        /* OR RULE */
                        for(Group group : groups){
                            boolean isOn = false;
                            for(Node node : group.nodes){
                                if(node.isOn){
                                    isOn = true;
                                    break;
                                }
                            }
                            group.isOn = isOn;
                        }

                        /* XOR RULE */
                        /*
                        for(Group group : groups){
                            boolean oneOn = false;
                            boolean allOn = true;
                            for(Node node : group.nodes){
                                if(node.isOn)
                                    oneOn = true;
                                else
                                    allOn = false;
                                group.isOn = oneOn && !allOn;
                            }
                        }
                         */

                        /* AND Rule */
                        /*
                        for(Group group : groups){
                            boolean isOn = true;
                            for(Node node : group.nodes){
                                if(!node.isOn){
                                    isOn = false;
                                    break;
                                }
                            }
                            group.isOn = isOn;
                        }
                         */

                        mirrorStatus(runnable.network, network);

                        //reset start and goal node
                        for(NodeAnalysisDataHolder result : network.goalActivity.analysisDataHolders){
                            result.operability = Double.NaN;
                        }
                        if(network.startActivity.isOn){
                            network.oAlgorithmUsingOrder(runnable.inputQueues.get(i).rollUpRule, i, analysis.targetType);
                        }else{
                            network.goalActivity.analysisDataHolders.get(i).operability = 0;
                        }
                        runnable.inputQueues.get(i).hasBeenCalculated = true;

                        ArrayList<Node> allNodes = new ArrayList<>(nodes);
                        allNodes.addAll(network.getGroups("crit"));

                        writePossibilitiesTable(runnable.input.network, allNodes, network.goalActivity, i);

                        double phi = network.goalActivity.analysisDataHolders.get(i).operability / 100.0;
                        runnable.updatePhiCounts(phi, i, a);
                        //runnable.updateEMatrix(allNodes, i, a);

                        if(!Double.isNaN(network.goalActivity.analysisDataHolders.get(i).operability)){
                            network.connectsToGoal = true;
                        }
                    }
                }
            }

            if(index != null)
                index = index.add(BigInteger.ONE);

            runnable.input.updateLiveCount();

            //check if the time is up
            if(Duration.between(startInstant, Instant.now()).toSeconds() >= (60.0 * runnable.maxMinutes) || runnable.input.phiForceStop){
                break;
            }

            condition = checkCustomCondition(runnable, index);
        }
    }

    private void mirrorStatus(Network targetNetwork, Network origenNetwork){
        for(Node oriNode : origenNetwork.getNodes()){
            for(Node node : targetNetwork.getNodes()){
                if(node.id == oriNode.id){
                    node.isOn = oriNode.isOn;
                    break;
                }
            }
        }
    }

    private void writePossibilitiesTable(Network network, ArrayList<Node> nodes, Node goalNode, int rollUpI){
        if(network.analysisDataHolders.get(rollUpI).tableRowCount < Input.MAX_TABLE_ROWS){
            ArrayList<Integer> row = new ArrayList<>();
            for(Node node : nodes){
                row.add(node.isOn ? 1 : 0);
              }
            network.analysisDataHolders.get(rollUpI).possibilitiesTable.add(row);
            network.analysisDataHolders.get(rollUpI).endResults.add(goalNode.analysisDataHolders.get(rollUpI).operability / 100.0);
            network.analysisDataHolders.get(rollUpI).tableRowCount++;
        }
    }

    protected abstract boolean checkCustomCondition(PhiRunnable runnable, BigInteger index);

    protected abstract void setNetworkStatus(Network network, ArrayList<Node> nodes, PhiRunnable runnable, BigInteger index);
}
