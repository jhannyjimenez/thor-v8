package org.mitre.thor.network.nodes;

import org.mitre.thor.analyses.data_holders.NodeAnalysisDataHolder;
import org.mitre.thor.network.Network;

import java.util.ArrayList;

public class Node implements Cloneable{

    public static int nodeCount = 0;

    public String name;
    // The ID used in this system to identify nodes
    public int id = -1;
    public boolean isOn =  true;
    //Determines if the node is used in calculations and displayed in outputs
    public boolean isReal = true;
    public boolean isPhysical = true;
    public ArrayList<NodeAnalysisDataHolder> analysisDataHolders = new ArrayList<>();

    //Value used for factors analysis
    public double SE = Double.NaN;
    public double staticSE = Double.NaN;

    public double fdna2Value = Double.NaN;

    public String mathematicaColor = null;
    public String mathematicaSize = null;
    public int decorativeID = -1;
    public boolean attachDID = false;

    public double A = 1;
    public double B = -.5;

    public Node(String name){
        this.name = name;
        nodeCount++;
    }

    public void setOperability(int dataHolderIndex, double op) {
        if(Network.CAPTURE_RUN_MAP){
            Network.RUN_MAP.put(id, op);
        }
        analysisDataHolders.get(dataHolderIndex).operability = op;
    }

    public void setOn(boolean on){
        this.isOn = on;
    }

    @Override
    public Node clone() {
        try {
            return (Node) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String getDecorativeName(){
        String n = name;
        if(attachDID){
            n += "-" + decorativeID;
        }
        return n;
    }

    public String getIDTag(){
        return "";
    }

    @Override
    public String toString() {
        return name;
    }
}
