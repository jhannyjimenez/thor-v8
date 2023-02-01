package org.mitre.thor.network.attack;

import org.mitre.thor.analyses.target.TargetType;
import org.mitre.thor.input.Input;

import java.util.ArrayList;
import java.util.Random;

public class DecisionTree {
    private final ArrayList<Route> routes = new ArrayList<>();
    private final ArrayList<Decision> decisions = new ArrayList<>();

    private final Random RAND = new Random();

    private final boolean USE_BUDGET;
    private final double BUDGET;

    public DecisionTree(boolean useBudget, double budget){
        this.USE_BUDGET = useBudget;
        this.BUDGET = budget;
    }

    public ArrayList<AttackPoint> simulateAttackPoints(Input input, AttackChain startChain, int rollUpIndex, int maxPoints, TargetType targetType) {
        ArrayList<AttackPoint> attackPoints = new ArrayList<>();
        input.network.findNetworkOrder(input.network.startActivity);
        for(int r = 0; r < maxPoints; r++){
            // Choose a random decision to start with
            AttackChain chain;
            Decision start;
            if (startChain != null && !startChain.isEmpty()) {
                chain = new AttackChain(startChain);
                start = advanceDecision(chain, chain.getDecision(chain.getDecisionsSize() -  1));
            }else {
                chain = new AttackChain();
                // Get all the decisions that the decision tree can start with (decisions with no required outcomes)
                ArrayList<Decision> startDecisions = getStartDecisions();
                start = startDecisions.get(RAND.nextInt(startDecisions.size()));
            }
            if (start == null) {
                return attackPoints;
            }
            AttackChain randomChain =  createRandomAttackChain(chain, start);
            runAttackChain(input, randomChain, rollUpIndex, targetType);
            AttackPoint point = getAttackPoint(input, randomChain, rollUpIndex);

            if (!attackPoints.contains(point))
                attackPoints.add(point);
            else {
                attackPoints.get(attackPoints.indexOf(point)).count += 1;
            }
        }
        return attackPoints;
    }

    public static void runAttackChain(Input input, AttackChain chain, int rollUpIndex, TargetType targetType) {
        input.network.resetOperabilities(rollUpIndex);
        input.network.setAttackChainOperabilities(chain, rollUpIndex);
        input.network.oAlgorithmUsingOrder(input.iConfig.inputQueues.get(rollUpIndex).rollUpRule, rollUpIndex, targetType);
    }

    public static AttackPoint getAttackPoint(Input input, AttackChain chain, int rollUpI) {
        double impact = 100.0 - input.network.goalActivity.analysisDataHolders.get(rollUpI).operability;
        double cost = chain.getAccumulatedCost();
        return new AttackPoint(cost, impact, chain);
    }

    public AttackChain createRandomAttackChain(AttackChain chain, Decision start){

        continueAttackChain(chain, start);

        return chain;
    }

    private void continueAttackChain(AttackChain chain, Decision targetDecision) {
        // Check if adding the target decision goes over the budget
        if(!USE_BUDGET || chain.getAccumulatedCost() + targetDecision.getCost() <= BUDGET){
            chain.addDecision(targetDecision);
            Decision nextDecision = advanceDecision(chain, targetDecision);
            boolean shouldAttack = RAND.nextBoolean();
            if(nextDecision != null && shouldAttack){
                continueAttackChain(chain, nextDecision);
            }
        }
    }

    private Decision advanceDecision(AttackChain chain, Decision targetDecision) {
        Route chosenRoute = getNextRoute(targetDecision);
        chain.addRoute(chosenRoute);
        return getNextDecision(chain, chosenRoute);
    }

    public ArrayList<Decision> getStartDecisions(){
        ArrayList<Decision> out = new ArrayList<>();
        for(Decision decision : decisions){
            if(decision.getRequirement() == null || decision.getRequirement().isEmpty() || decision.getRequirement().isBlank()){
                out.add(decision);
            }
        }
        return out;
    }

    public ArrayList<Decision> getDecisionOptions(Route route){
        ArrayList<Decision> out = new ArrayList<>();
        for(Decision decision : decisions){
            if(decision.getRequirement() == null || decision.getRequirement().isEmpty() || decision.getRequirement().isBlank() || decision.getRequirement().equals(route.getFullId())){
                out.add(decision);
            }
        }
        return out;
    }

    public ArrayList<Route> getRouteOptions(int decisionId){
        ArrayList<Route> out = new ArrayList<>();
        for(Route route : routes){
            if(route.getDecisionId() == decisionId){
                out.add(route);
            }
        }
        return out;
    }

    public Decision getNextDecision(AttackChain chain, Route route){
        ArrayList<Decision> decisionOptions = getDecisionOptions(route);
        ArrayList<Decision> finalOptions = new ArrayList<>();
        for (Decision option : decisionOptions) {
            if (!chain.containsDecision(option)) {
                finalOptions.add(option);
            }
        }
        if(!finalOptions.isEmpty())
            return finalOptions.get(RAND.nextInt(finalOptions.size()));
        else
            return null;
    }

    public Route getNextRoute(Decision decision){
        ArrayList<Route> possibleRoutes = new ArrayList<>(getRouteOptions(decision.getId()));
        if(possibleRoutes.size() > 1){
            // Compute the total weight of all items together.
            // This can be skipped of course if sum is already 1.
            double totalWeight = 0.0;
            for (Route route : possibleRoutes) {
                totalWeight += route.getProbSuccess();
            }
            // Now choose a random item.
            int idx = 0;
            for (double r = Math.random() * totalWeight; idx < possibleRoutes.size() - 1; ++idx) {
                r -= possibleRoutes.get(idx).getProbSuccess();
                if (r <= 0.0) break;
            }
            return possibleRoutes.get(idx);
        }else if(possibleRoutes.size() == 1){
            return possibleRoutes.get(0);
        }else{
            return null;
        }
    }

    public void addRoute(Route route){
        for(Route a : routes){
            if(a.equals(route)){
                return;
            }
        }
        routes.add(route);
    }

    public void addDecision(Decision decision){
        for(Decision d : decisions){
            if(d.equals(decision)){
                return;
            }
        }
        decisions.add(decision);
    }

    public boolean containsDecision(int dId) {
        for (Decision d : decisions) {
            if (d.getId() == dId) {
                return true;
            }
        }
        return false;
    }

    public void clear(){
        routes.clear();
        decisions.clear();
    }
}
