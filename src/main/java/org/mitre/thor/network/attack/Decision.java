package org.mitre.thor.network.attack;

import java.util.List;

public class Decision {
    private final int id;
    private final List<String> requirements;
    private final RequirementsRule reqRule;
    private final double cost;
    private final String description;


    public Decision(int id, List<String> requirements, RequirementsRule reqRule, double cost, String description){
        this.id = id;
        this.requirements = requirements;
        this.reqRule = reqRule;
        this.cost = cost;
        this.description = description;
    }

    public int getId(){
        return id;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public String getDescription(){
        return description;
    }

    public double getCost(){
        return cost;
    }

    public RequirementsRule getReqRule() { return reqRule;}

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Decision){
            if (requirements.size() != ((Decision) obj).requirements.size()) return false;
            for (int i = 0; i < requirements.size(); i++) {
                if (!requirements.get(i).equals(((Decision) obj).requirements.get(i))) return false;
            }
            return id == ((Decision) obj).id && cost == ((Decision) obj).cost && reqRule == ((Decision) obj).reqRule;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return String.valueOf(this.id);
    }
}

