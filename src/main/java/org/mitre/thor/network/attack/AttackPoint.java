package org.mitre.thor.network.attack;

public class AttackPoint {
    public double cost;
    public double impact;
    public AttackChain path;
    public int count = 1;

    public AttackPoint(double cost, double impact, AttackChain path){
        this.cost = cost;
        this.impact = impact;
        this.path = path;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AttackPoint)
            return cost == ((AttackPoint) obj).cost && path.equals(((AttackPoint) obj).path);
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "I: " + impact + " | " + "C: " + cost + " | " + path.toString();
    }
}
