package org.mitre.thor.analyses.crit;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.math.BigInteger;
import java.util.ArrayList;

class RandomPossibilities extends CalculationMethod{

    @Override
    protected boolean checkCustomCondition(PhiRunnable runnable, BigInteger index) {
        return true;
    }

    @Override
    protected void setNetworkStatus(Network network, ArrayList<Node> nodes, PhiRunnable phiRunnable, BigInteger index) {
        //50 percent chance that each node is turned on or off
        for(Node node : nodes){
            double choice = Math.random();
            //turn the node off if the choice is less than chance
            boolean less = choice <  node.offChance;
            node.setOn(!less);
        }
    }
}
