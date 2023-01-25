package org.mitre.thor.analyses.crit;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Node;

import java.math.BigInteger;
import java.util.ArrayList;

class AllPossibilities extends CalculationMethod{

    @Override
    protected boolean checkCustomCondition(PhiRunnable runnable, BigInteger index) {
        return index.compareTo(runnable.endIndex) < 0;
    }

    @Override
    protected void setNetworkStatus(Network network, ArrayList<Node> nodes, PhiRunnable runnable, BigInteger index) {
        //converts index into base with the total characters being equal to the number of nodes
        String binary = String.format("%" + nodes.size() + "s", index.toString(2)).replace(' ', '0');
        for(int a = 0; a < nodes.size(); a++) {
            //turn node on if the binary letter is '1'
            nodes.get(a).setOn(binary.charAt(a) == '1');
        }
    }

}
