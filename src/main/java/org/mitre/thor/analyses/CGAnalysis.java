package org.mitre.thor.analyses;

import org.mitre.thor.input.Input;
import org.mitre.thor.analyses.cg.ColorSet;
import org.mitre.thor.analyses.grouping.Grouping;
import org.mitre.thor.analyses.cg.CGOrderingMethod;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Group;
import org.mitre.thor.network.nodes.Node;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;
import org.mitre.thor.analyses.target.TargetType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CGAnalysis extends Analysis{

    public Grouping grouping;
    public CGOrderingMethod orderingMethod;
    public boolean saveGraph;
    public ColorSet colorSet;

    public CGAnalysis(List<RollUpEnum> rollUpRules, CGOrderingMethod orderingMethod, Grouping grouping, boolean SaveGraph, ColorSet colorSet, TargetType targetType){
        super(AnalysesForm.CG);
        this.rollUpRules = rollUpRules;
        this.orderingMethod = orderingMethod;
        this.grouping = grouping;
        this.saveGraph = SaveGraph;
        this.targetType = targetType;
        this.colorSet = colorSet;
    }

    //TODO add error catching
    @Override
    public boolean inputProcess(Input input) {

        calculateNodeMissionOperabilityPerTime(input);

        return true;
    }

    public void calculateNodeMissionOperabilityPerTime(Input input){
        if(input.app.controller != null){
            input.app.controller.showProcess(input.iConfig.process);
        }

        for(Node node : input.network.getNodes()){
            node.isOn = true;
        }

        input.network.clearIteratedNodes();
        input.network.networkOrder.clear();
        input.network.findNetworkOrder(input.network.startActivity);

        ArrayList<Integer> tempDivs = new ArrayList<>(input.cGDivisionNumbers);
        tempDivs.add(0, -1);

        input.network.removeGroups("cg");
        if(grouping == Grouping.PAIRS){
            input.network.generatePairs("cg", targetType);
        }else if(grouping == Grouping.PAIRS_AND_TRIPLES){
            input.network.generatePairs("cg", targetType);
            input.network.generateTriples("cg", targetType);
        }

        double totalNodes = 0;
        ArrayList<Node> finalTargetNodes = new ArrayList<>();
        ArrayList<Group> cgGroups = input.network.getGroups("cg");
        ArrayList<Group> cgGroupsCustom = input.network.getGroups("cg-custom");
        ArrayList<Node> tNodes = targetType.getTargetNodes(input.network);
        for(int i = 0; i < input.iConfig.inputQueues.size(); i++){
            if(cgGroups.size() > 0){
                totalNodes += cgGroups.size();
                if(i == 0){
                    finalTargetNodes = new ArrayList<>(cgGroups);
                }
            }else if(cgGroupsCustom.size() > 0){
                totalNodes += cgGroupsCustom.size();
                if(i == 0){
                    finalTargetNodes = new ArrayList<>(cgGroupsCustom);
                }
            }else {
                totalNodes += tNodes.size();
                if(i == 0){
                    finalTargetNodes = new ArrayList<>(tNodes);
                }
            }
        }

        int j = 0;
        Network.CAPTURE_RUN_MAP = true;
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++){
            if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CG)){
                //Calculate the Mission Operability per time
                for(Node node : finalTargetNodes){
                    HashMap<Integer, Double>[] runCollection = new HashMap[101];
                    for(int i = 0; i < 101; i++){
                        Network.RUN_MAP.clear();
                        input.network.resetOperabilities(a);
                        node.setOperability(a, i);
                        input.network.oAlgorithmUsingOrder(input.iConfig.inputQueues.get(a).rollUpRule, a, targetType);
                        node.analysisDataHolders.get(a).coolGraphMissionOperability[i] = input.network.goalActivity.analysisDataHolders.get(a).operability;
                        runCollection[i] = new HashMap<>(Network.RUN_MAP);
                    }
                    j++;
                    input.network.analysisDataHolders.get(a).cgRunMap.put(node.id, runCollection);
                    input.updateLoadScreen(j + " / " + (int) totalNodes + " nodes", j / totalNodes);
                }

                System.out.println("Test");

                //Calculate the Nodes Size Per Division
                for (int i = 1; i < tempDivs.size(); i++) {
                    for(Node node : finalTargetNodes){
                        int size = 0;
                        for (int z = 0; z < node.analysisDataHolders.get(a).coolGraphMissionOperability.length; z++) {
                            if (node.analysisDataHolders.get(a).coolGraphMissionOperability[z] <= tempDivs.get(i) && node.analysisDataHolders.get(a).coolGraphMissionOperability[z] > tempDivs.get(i - 1)) {
                                size++;
                            }
                        }
                        node.analysisDataHolders.get(a).sizePerDivision.add(size);
                    }
                }

                //Calculate the CG nodes score
                for(Node node : finalTargetNodes){
                    node.analysisDataHolders.get(a).cgSCore = orderingMethod.getNodeScore(node, input.cGDivisionNumbers.size(), a);
                }

                //Calculate Normalized node scores
                double max = -Double.MAX_VALUE;
                double min = Double.MAX_VALUE;
                for(Node node : finalTargetNodes){
                    double cgScore = node.analysisDataHolders.get(a).cgSCore;
                    if(cgScore > max){
                        max = cgScore;
                    }else if(cgScore < min){
                        min = cgScore;
                    }
                }
                for(Node node : finalTargetNodes){
                    double cgScore = node.analysisDataHolders.get(a).cgSCore;
                    if(max - min != 0){
                        node.analysisDataHolders.get(a).normalCgScore = (int) (100.0 * (cgScore - min) / (max - min));
                    }else{
                        node.analysisDataHolders.get(a).normalCgScore = 100;
                    }
                }
            }
        }

        Network.CAPTURE_RUN_MAP = false;


        if(input.app.controller != null){
            input.app.controller.hideProcess(input.iConfig.process);
        }
    }
}
