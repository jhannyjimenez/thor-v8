package org.mitre.thor.network.attack;

import java.util.ArrayList;

public class TextTree {
    private static final String NODE_SEPARATOR = "   ";

    private final ArrayList<Object> objects = new ArrayList<>();
    private final ArrayList<Integer> tabs = new ArrayList<>();
    private int lineCount = 0;

    public void addLine(Object object, int numTabs){
        objects.add(object);
        tabs.add(numTabs);
        lineCount++;
    }

    public void deleteObject(Object obj){
        if(objects.contains(obj)){
            objects.remove(obj);
            lineCount--;
        }
    }

    public Object getObjectAt(int line){
        return objects.get(line);
    }

    public int getLineCount(){
        return this.lineCount;
    }

    public void clear(){
        objects.clear();
        tabs.clear();
        lineCount = 0;
    }

    @Override
    public String toString() {
        StringBuilder tree = new StringBuilder();
        for(int i = 0; i < lineCount; i++){
            tree.append(NODE_SEPARATOR.repeat(tabs.get(i)));
            Object obj = objects.get(i);
            if(obj instanceof DecisionNode){
                DecisionNode node = (DecisionNode) obj;
                tree.append("Do '").append(node.rdComment).append("' ").append(node.rdLabel).append(",\n");
            }else if(obj instanceof DecisionLink){
                DecisionLink link = (DecisionLink) obj;
                tree.append("If '").append(link.comment).append("' ").append(link.name).append(",\n");
            }
        }
        return tree.toString();
    }
}
