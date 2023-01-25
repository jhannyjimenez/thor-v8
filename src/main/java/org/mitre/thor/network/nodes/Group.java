package org.mitre.thor.network.nodes;

import org.mitre.thor.network.Network;

import java.util.ArrayList;

public class Group extends Activity{

    public ArrayList<Node> nodes = new ArrayList<>();
    public String tag = "";
    public String subTag = "";
    public boolean actAsActivity = false;

    public Group(String name) {
        super(name);
    }

    public Group(String name, ArrayList<Node> nodes){
        super(name);
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void setOperability(int dataHolderIndex, double op) {
        setConnectedNodesOperability(dataHolderIndex, op);
        if(actAsActivity){
            setGroupNodeOperability(dataHolderIndex, op);
        }
        if(Network.CAPTURE_RUN_MAP){
            Network.RUN_MAP.put(id, op);
        }
    }

    public void setConnectedNodesOperability(int dataHolderIndex, double op){
        for(Node node : nodes){
            node.setOperability(dataHolderIndex, op);
        }
        if(Network.CAPTURE_RUN_MAP){
            Network.RUN_MAP.put(id, op);
        }
    }

    public void setGroupNodeOperability(int dataHolderIndex, double op){
        super.setOperability(dataHolderIndex, op);
        if(Network.CAPTURE_RUN_MAP){
            Network.RUN_MAP.put(id, op);
        }
    }

    @Override
    public void setOn(boolean on) {
        for(Node node : nodes){
            node.setOn(on);
        }
        super.setOn(on);
    }

    @Override
    public String getIDTag() {
        return "g-";
    }
}
