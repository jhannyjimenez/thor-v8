package org.mitre.thor.network.nodes;

public class Factor extends Activity{
    //NOTE: THE FACTOR HEALTH IS JUST THE OPERABILITY OF THE FACTOR
    //THIS CAN BE ACCESSED THROUGH node.analysisDataHolder.get(index).operability;
    //public double health = 100.0;

    public Factor(String name){
        super(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getIDTag() {
        return "f-";
    }
}
