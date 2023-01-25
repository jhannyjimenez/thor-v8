package org.mitre.thor.network.attack;

public class Decision {
    private final int id;
    private final String requirement;
    private final double cost;
    private final String description;


    public Decision(int id, String requirement, double cost, String description){
        this.id = id;
        this.requirement = requirement;
        this.cost = cost;
        this.description = description;
    }

    public int getId(){
        return id;
    }

    public String getRequirement() {
        return requirement;
    }

    public String getDescription(){
        return description;
    }

    public double getCost(){
        return cost;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Decision){
            return id == ((Decision) obj).id && cost == ((Decision) obj).cost && requirement.equals(((Decision) obj).requirement);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return String.valueOf(this.id);
    }
}

