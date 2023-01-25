package org.mitre.thor.analyses.crit;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

class FDNAPossibilities extends CalculationMethod{

    @Override
    protected boolean checkCustomCondition(PhiRunnable runnable, BigInteger index) {
        return true;
    }

    @Override
    protected void setNetworkStatus(Network network, ArrayList<Node> nodes, PhiRunnable phiRunnable, BigInteger index) {
        Random rand = new Random();
        //50 percent chance to turn on a node and set the node operability a random # between 0-101
        for(Node node : nodes){
            double choice = Math.random();
            double chance = .5;
            //turn the node on if the choice is less than chance
            node.setOn(choice < chance);
            for(int i = 0; i < node.analysisDataHolders.size(); i++){
                node.fdna2Value = (int) (rand.nextDouble() * 100);
            }
        }
    }
}
