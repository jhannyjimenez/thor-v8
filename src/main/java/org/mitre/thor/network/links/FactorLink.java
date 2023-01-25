package org.mitre.thor.network.links;

import org.mitre.thor.network.nodes.Factor;
import org.mitre.thor.network.nodes.Node;

public class FactorLink extends Link{

    public double fvi;
    public boolean binary;

    public FactorLink(Factor child, Node parent) {
        super(child, parent);
    }

    public FactorLink(Factor child, Node parent, double fvi, boolean binary, String mathe){
        super(child, parent);
        this.fvi = fvi;
        this.binary = binary;
        super.mathematicaColor = mathe;
    }
}
