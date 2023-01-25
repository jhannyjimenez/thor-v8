package org.mitre.thor.network.links;

import org.mitre.thor.network.nodes.Activity;

public class ActivityLink extends Link{

    public double SOD;
    public double COD;
    public double IOD;
    public double onChance = 1.0;

    public ActivityLink(Activity child, Activity parent) {
        super(child, parent);
    }

    public ActivityLink(Activity child, Activity parent, double IOD, double COD, double SOD, double onChance, String matheColor){
        super(child, parent);
        this.IOD = IOD;
        this.COD = COD;
        this.SOD = SOD;
        super.mathematicaColor = matheColor;
    }
}
