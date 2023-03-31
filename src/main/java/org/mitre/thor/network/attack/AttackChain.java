package org.mitre.thor.network.attack;

import java.util.ArrayList;

/**
 * A list/path of decisions and routes that represents the choices made
 */
public class AttackChain {
    private int accumulatedCost = 0;
    private final ArrayList<Decision> decisions = new ArrayList<>();
    private final ArrayList<Route> routes = new ArrayList<>();

    public AttackChain(){}

    public AttackChain(AttackChain attackChain){
        for(Decision decision : attackChain.decisions){
            addDecision(decision);
        }
        for(Route route : attackChain.routes){
            addRoute(route);
        }
    }

    public void addDecision(Decision decision){
        this.decisions.add(decision);
        this.accumulatedCost += decision.getCost();
    }

    public Decision getDecision(int index){
        return this.decisions.get(index);
    }

    public void removeDecision(Decision decision) {decisions.remove(decision);}

    public int getAccumulatedCost(){
        return this.accumulatedCost;
    }

    public void addRoute(Route route) { this.routes.add(route); }

    public Route getRoute(int index) { return this.routes.get(index); }

    public void clearDecisions(){
        decisions.clear();
        accumulatedCost = 0;
    }

    public void clearRoutes(){
        routes.clear();
    }

    public int getDecisionsSize(){
        return decisions.size();
    }

    public int getRoutesSize(){
        return routes.size();
    }

    public boolean containsDecision(Decision d){
        return decisions.contains(d);
    }

    public boolean containsRoute(Route r){
        return routes.contains(r);
    }

    public boolean containsRouteFullID(String rID) {
        for (Route r : routes) {
            if (r.getFullId().equals(rID)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsChain(AttackChain chain){
        if (this.getDecisionsSize() < chain.getDecisionsSize() || this.getRoutesSize() < chain.getRoutesSize())
            return false;
        return chainHasMatchingDecisionAndRoutes(chain);
    }

    public boolean containsChain2(AttackChain chain) {
        for (int i = 0; i < chain.getDecisionsSize(); i++) {
            if (!this.containsDecision(chain.getDecision(i))) {
                return false;
            }
        }

        for (int i = 0; i < chain.getRoutesSize(); i++) {
            if (!this.containsRoute(chain.getRoute(i))) {
                return false;
            }
        }

        return true;
    }

    public boolean isEmpty() {
        return decisions.isEmpty() && routes.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AttackChain chain){
            if(this.getDecisionsSize() != chain.getDecisionsSize() || this.getRoutesSize() != chain.getRoutesSize())
                return false;
            return chainHasMatchingDecisionAndRoutes(chain);
        }
        return super.equals(obj);
    }

    private boolean chainHasMatchingDecisionAndRoutes(AttackChain chain){
        boolean matchingDecisions = true;
        for(int i = 0; i < chain.getDecisionsSize(); i++){
            if(!this.getDecision(i).equals(chain.getDecision(i))){
                matchingDecisions = false;
                break;
            }
        }
        if(matchingDecisions){
            boolean matchingRoutes = true;
            for(int i = 0; i < chain.getRoutesSize(); i++){
                if(!this.getRoute(i).equals(chain.getRoute(i))){
                    return false;
                }
            }
            return matchingRoutes;
        }else{
            return false;
        }
    }

    @Override
    public String toString() {
        if(getDecisionsSize() != 0){
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < getDecisionsSize(); i++){
                sb.append(getDecision(i).toString());
                sb.append(" -> ");
                if(i <= getRoutesSize() - 1) {
                    sb.append(getRoute(i).toString());
                    sb.append(" -> ");
                }else {
                    break;
                }
            }
            int lastSpace = sb.lastIndexOf(" ");
            sb.delete(lastSpace - 3, lastSpace + 1);
            return sb.toString();
        }else{
            return "";
        }
    }

    public AttackChain clone(){
        AttackChain newPath = new AttackChain();
        for(Decision d : decisions){
            newPath.addDecision(d);
        }
        for(Route r : routes){
            newPath.addRoute(r);
        }
        return newPath;
    }
}
