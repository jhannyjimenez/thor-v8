package org.mitre.thor.network.attack;

import org.mitre.thor.network.nodes.Node;

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

    public AttackChain createRandomAttackChain(int rollUpIndex, Node goalNode){
        // Get all the decisions that the decision tree can start with (decisions with no required outcomes)
        ArrayList<Decision> startDecisions = getStartDecisions();
        // Choose a random decision to start with
        Decision start = startDecisions.get(RAND.nextInt(startDecisions.size()));
        // Start the attack chain process
        AttackChain chain = new AttackChain();
        continueAttackChain(chain, start, rollUpIndex, goalNode);

        return chain;
    }

    private void continueAttackChain(AttackChain chain, Decision targetDecision, int rollUpIndex, Node goalNode) {
        // Check if adding the target decision goes over the budget
        if(!USE_BUDGET || chain.getAccumulatedCost() + targetDecision.getCost() <= BUDGET){
            chain.addDecision(targetDecision);
            Route chosenRoute = getNextRoute(targetDecision);
            chain.addRoute(chosenRoute);
            if(chosenRoute.getTargetNode() != null){
                chosenRoute.getTargetNode().setOperability(rollUpIndex, chosenRoute.getOperability());
            }
            Decision nextDecision = getNextDecision(chain, chosenRoute);
            boolean shouldAttack = RAND.nextBoolean();
            if(nextDecision != null && shouldAttack){
                continueAttackChain(chain, nextDecision, rollUpIndex, goalNode);
            }
        }
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

    public void clear(){
        routes.clear();
        decisions.clear();
    }
}
