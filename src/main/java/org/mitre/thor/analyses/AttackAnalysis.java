package org.mitre.thor.analyses;

import org.mitre.thor.analyses.rolluprules.RollUpEnum;
import org.mitre.thor.input.Input;
import org.mitre.thor.network.attack.*;
import org.mitre.thor.network.nodes.Node;

import java.util.*;

public class AttackAnalysis extends Analysis {

    public final DecisionOption DECISION_OPTION;
    public final double MAX_POINTS;
    public final boolean USE_BUDGET;
    public final double BUDGET;
    public final boolean INCLUDE_TEXT_STOP;
    public final boolean INCLUDE_MATH_STOP;



    public AttackAnalysis(List<RollUpEnum> rollUpRules, double maxMinutes, boolean useBudget, double budget,
                          DecisionOption option, boolean includeTextStop, boolean includeMathStop) {
        super(AnalysesForm.ATTACK);
        super.rollUpRules = rollUpRules;
        this.MAX_POINTS = maxMinutes;
        this.USE_BUDGET = useBudget;
        this.BUDGET = budget;
        this.DECISION_OPTION = option;
        this.INCLUDE_TEXT_STOP = includeTextStop;
        this.INCLUDE_MATH_STOP = includeMathStop;
    }

    @Override
    public boolean inputProcess(Input input) {
        for(Node node : input.network.getNodes()){
            node.isOn = true;
        }
        simulateAttacks(input);
        buildTree(input);
        return true;
    }

    private void simulateAttacks(Input input){

        // Pre-calculate the oder to find the goal operability
        input.network.findNetworkOrder(input.network.startActivity);
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++) {
            if (input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.ATTACK)) {
                for(int r = 0; r < MAX_POINTS; r++){
                    AttackChain path =  input.decisionTree.createRandomAttackChain(a, input.network.goalActivity);
                    input.network.oAlgorithmUsingOrder(input.iConfig.inputQueues.get(a).rollUpRule, a, targetType);
                    double impact = 100.0 - input.network.goalActivity.analysisDataHolders.get(a).operability;
                    double cost = path.getAccumulatedCost();

                    AttackPoint point = new AttackPoint(cost, impact, path);
                    if (!input.network.analysisDataHolders.get(a).attackPoints.contains(point))
                        input.network.analysisDataHolders.get(a).attackPoints.add(point);
                    else {
                        AttackPoint np = input.network.analysisDataHolders.get(a).attackPoints.get(input.network.analysisDataHolders.get(a).attackPoints.indexOf(point));
                        np.count++;
                    }
                    input.network.resetOperabilities(a);
                }
            }
        }
    }

    private void  buildTree(Input input) {
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++) {
            if (input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.ATTACK)) {
                input.network.analysisDataHolders.get(a).attackTreeBuilder = new AttackTreeBuilder(
                        input.network.analysisDataHolders.get(a).attackPoints,
                        input.decisionTree,
                        DECISION_OPTION,
                        USE_BUDGET,
                        BUDGET,
                        INCLUDE_TEXT_STOP,
                        INCLUDE_MATH_STOP);
                input.network.analysisDataHolders.get(a).attackTreeBuilder.buildTree();
            }
        }
    }
}
