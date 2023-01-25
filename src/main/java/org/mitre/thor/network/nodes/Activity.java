package org.mitre.thor.network.nodes;

import org.mitre.thor.analyses.rolluprules.RollUpEnum;

public class Activity extends Node {

    public RollUpEnum customActivityRule = null;
    public RollUpEnum customGroupRule = null;
    public RollUpEnum customFactorRule = null;

    //Criticality analysis default  values

    //Level used for anti-loops system
    public int loopLevel = 0;

    public Activity(String activityName){
        super(activityName);
    }

    @Override
    public String getIDTag() {
        return "n-";
    }
}
