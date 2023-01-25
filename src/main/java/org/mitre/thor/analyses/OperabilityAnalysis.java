package org.mitre.thor.analyses;

import org.mitre.thor.input.Input;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Node;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;

import java.util.ArrayList;
import java.util.List;

public class OperabilityAnalysis extends Analysis{

    public OperabilityAnalysis(List<RollUpEnum> rollUpRules){
        super(AnalysesForm.DYNAMIC_OPERABILITY);
        this.rollUpRules = rollUpRules;
    }

    //TODO add error catching
    @Override
    public boolean inputProcess(Input input) {
        calculateOperabilities(input);
        return true;
    }

    public void calculateOperabilities(Input input){
        if(input.app.controller != null){
            input.app.controller.showProcess(input.iConfig.process);
        }

        for(Activity activity : input.network.getActivities()){
            activity.isOn = true;
        }

        input.network.networkOrder.clear();
        input.network.clearIteratedNodes();
        input.network.findNetworkOrder(input.network.startActivity);

        ArrayList<Node> tNodes = targetType.getTargetNodes(input.network);

        for(int a = 0; a < input.iConfig.inputQueues.size(); a++){
            if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.DYNAMIC_OPERABILITY)){
                for(int i = 0; i < input.numTimes; i++){
                    for(Node node : tNodes){
                        node.analysisDataHolders.get(a).operability = node.analysisDataHolders.get(a).operabilityPerTime[i];
                    }
                    if(!input.seTimeColumns.isEmpty()){
                        for(Activity activity : input.network.getActivities()){
                            activity.SE = activity.analysisDataHolders.get(a).sePerTime[i];
                        }
                    }

                    if(!Double.isNaN(input.network.startActivity.analysisDataHolders.get(a).operabilityPerTime[i])){
                        input.network.startActivity.analysisDataHolders.get(a).operability = input.network.startActivity.analysisDataHolders.get(a).operabilityPerTime[i];
                    }

                    if(Double.isNaN(input.network.goalActivity.analysisDataHolders.get(a).operabilityPerTime[i])){
                        input.network.goalActivity.analysisDataHolders.get(a).operability = Double.NaN;
                    }else{
                        input.network.goalActivity.analysisDataHolders.get(a).operability = input.network.goalActivity.analysisDataHolders.get(a).operabilityPerTime[i];
                    }

                    input.network.oAlgorithmUsingOrder(input.iConfig.inputQueues.get(a).rollUpRule, a, targetType);
                    for(Node node : tNodes){
                        node.analysisDataHolders.get(a).operabilityPerTime[i] = node.analysisDataHolders.get(a).operability;
                    }
                    input.network.goalActivity.analysisDataHolders.get(a).operabilityPerTime[i] = input.network.goalActivity.analysisDataHolders.get(a).operability;
                    input.network.startActivity.analysisDataHolders.get(a).operabilityPerTime[i] = input.network.startActivity.analysisDataHolders.get(a).operability;

                    input.updateLoadScreen(i + " / " + input.numTimes + " times", i / (double) input.numTimes);
                }
            }
        }

        if(input.app.controller != null){
            input.app.controller.hideProcess(input.iConfig.process);
        }

    }
}
