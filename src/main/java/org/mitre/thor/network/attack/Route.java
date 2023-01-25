package org.mitre.thor.network.attack;

import org.mitre.thor.network.nodes.Node;

public class Route {
    private final int decisionId;
    private final int id;
    private final double probSuccess;
    private final Node targetNode;
    private final double operability;
    private final String comment;

    public Route(int decisionId, int id, double probSuccess, Node targetNode, double operability, String comment){
        this.decisionId = decisionId;
        this.id = id;
        this.probSuccess = probSuccess;
        this.targetNode = targetNode;
        this.operability = operability;
        this.comment = comment;
    }

    public int getId(){
        return id;
    }

    public int getDecisionId(){
        return decisionId;
    }

    public String getFullId() {
        return getDecisionId() + "-" + getId();
    }

    public double getProbSuccess() {
        return probSuccess;
    }

    public Node getTargetNode(){
        return targetNode;
    }

    public double getOperability() {
        return operability;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Route){
            Route a = (Route) obj;
            return this.decisionId == a.decisionId && this.id == a.id && this.operability == a.operability;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return targetNode != null ? id + "[" + targetNode + " : " + operability + "]" : String.valueOf(id);
    }
}
