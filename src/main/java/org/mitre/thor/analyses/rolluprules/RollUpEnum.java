package org.mitre.thor.analyses.rolluprules;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Factor;
import org.mitre.thor.network.nodes.Node;

import java.util.List;

public enum RollUpEnum {
    OR(new LogicalOR(), true, true),
    AND(new LogicalAND(), true, true),
    FDNA(new FDNA(), true, false),
    FDNA_OR(new FDNA_OR(), true, false),
    FDNA2(new FDNA2(), true, false),
    ODINN_FTI(new ODINN_FTI(), true, false),
    ODINN(new ODINN_CLASSIC(), true, false),
    AVERAGE(new Average(), true, true),
    THRESHOLD(new Threshold(), true, true),
    CUSTOM(new NodeSpecific(), true, true);

    private final IARollRule function;
    public final boolean isVisible;
    public final boolean factorsCompatible;

    RollUpEnum(IARollRule function, boolean isVisible, boolean factorsCompatible){
        this.function = function;
        this.isVisible = isVisible;
        this.factorsCompatible = factorsCompatible;
    }

    public double calculateNodeScoreFromChildren(Node node, Network network, List<Node> children, int rollUpIndex){
        if(!node.isOn){
            return 0.0;
        }
        return function.calculateNodeScoreFromChildren(node, network, children, rollUpIndex);
    }

    public double getFactorScore(Factor factor) {
        return function.getFactorScore(factor);
    }
}

