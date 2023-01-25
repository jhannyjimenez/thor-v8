package org.mitre.thor.network.loops;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.Path;
import org.mitre.thor.network.PathCollection;
import org.mitre.thor.network.links.ActivityLink;
import org.mitre.thor.network.nodes.Activity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TDeLoop {

    private final Network ogNet;
    private final Network GRAPH;

    private ArrayList<Activity> newPath;
    private Activity currentNode;
    private String pathLabel;
    private int pCounter;

    public static Network createDeLoopedGraph(Network network){
        TDeLoop deLoop = new TDeLoop(network);
        return deLoop.GRAPH;
    }

    private TDeLoop(Network ogNet) {
        this.ogNet = ogNet;
        GRAPH = new Network();
        GRAPH.inputQueues = ogNet.inputQueues;
        PathCollection paths = new PathCollection();
        ogNet.getPaths(ogNet.startActivity, ogNet.goalActivity, new Path(new ArrayList<>()), paths, 2);
        initGraph(paths.getPaths().get(0), ogNet);
        paths.getPaths().remove(0);
        deLoop(paths);
        renameNodes();
    }

    private void initGraph(Path path, Network og) {
        Activity lastNode = null;
        Activity thisNode;
        for (int i = 0; i < path.activities.size(); i++) {
            thisNode = (Activity) path.activities.get(i).clone();
            if (i == 0) {
                GRAPH.startActivity = lastNode = thisNode;
                GRAPH.addNode(GRAPH.startActivity);
            }else{
                if(i == path.activities.size() - 1) {
                    GRAPH.goalActivity = thisNode;
                }
                GRAPH.addNode(thisNode);
                ActivityLink link = og.getActivityLink(path.activities.get(i), path.activities.get(i - 1));
                GRAPH.linkActivities(lastNode, thisNode, link.onChance, link.IOD, link.SOD, link.COD, link.mathematicaColor);
                lastNode = thisNode;
            }
        }
    }

    private void deLoop(PathCollection paths){
        for(Path path : paths.getPaths()){
            addPath(path);
        }
    }

    private void renameNodes(){
        PathCollection paths = new PathCollection();
        GRAPH.getPaths(GRAPH.startActivity, GRAPH.goalActivity, new Path(new ArrayList<>()), paths, 1);
        for(Path path : paths.getPaths()){
            ArrayList<String> usedNames = new ArrayList<>();
            for(Activity node : path.activities){
                if(usedNames.contains(node.name)){
                    node.isReal = false;
                    node.isPhysical = false;
                }else{
                    usedNames.add(node.name);
                }
            }
        }

        ArrayList<String> usedNames = new ArrayList<>();
        int cnt = 1;
        for(Activity node : GRAPH.getActivities()){
            if(usedNames.contains(node.name)){
                node.name = node.name + "@" + cnt;
                node.id = GRAPH.getRandomUnusedId();
                node.decorativeID = node.id;
                node.mathematicaColor = "Gray";
                cnt++;
            }else{
                usedNames.add(node.name);
            }
        }
    }

    private void addPath(Path path){
        newPath = new ArrayList<>();
        prefixMinimization(path);
        pathInclusion(path);
        suffixMinimization(new Path(newPath));
    }

    private String forwardPathLabel(Path path){
        if(pCounter < path.activities.size() - 1){
            pCounter++;
        }
        return path.activities.get(pCounter).name;
    }

    private String backwardPathLabel(Path path){
        if(pCounter > 0){
            pCounter--;
        }
        return path.activities.get(pCounter).name;
    }

    private void prefixMinimization(Path path){
        pCounter = 1;
        currentNode = GRAPH.startActivity;
        newPath.add(currentNode);
        pathLabel = path.activities.get(1).name;
        while (true){
            if(pathLabel.equals(GRAPH.goalActivity.name)){
                break;
            }
            Activity matchOutNeighbor = findMatchPrefixOutNeighbor(currentNode, pathLabel);
            if(matchOutNeighbor != null){
                if(inDegree(matchOutNeighbor) == 1){
                    currentNode = matchOutNeighbor;
                    newPath.add(currentNode);
                }else{
                    Activity neighborCopy = (Activity) matchOutNeighbor.clone();
                    neighborCopy.id = GRAPH.getRandomUnusedId();
                    giveOutNeighbors(matchOutNeighbor, neighborCopy);
                    ActivityLink link = (ActivityLink) GRAPH.getLink(matchOutNeighbor, currentNode);
                    GRAPH.linkActivities(currentNode, neighborCopy, link.onChance, link.IOD, link.SOD, link.COD, link.mathematicaColor);
                    GRAPH.unlinkNodes(matchOutNeighbor, currentNode);
                    currentNode = neighborCopy;
                    newPath.add(currentNode);
                }
                pathLabel = forwardPathLabel(path);
            }else{
                break;
            }
        }
    }

    private Activity findMatchPrefixOutNeighbor(Activity currentNode, String pathLabel){
        ArrayList<Activity> outNeighbors = GRAPH.getActivitiesParents(currentNode);
        for(Activity outNeighbor : outNeighbors){
            if(outNeighbor.name.equals(pathLabel)){
                return outNeighbor;
            }
        }
        return null;
    }

    private int inDegree(Activity activity){
        ArrayList<Activity> children = GRAPH.getActivitiesChildren(activity);
        return children.size();
    }

    private void giveOutNeighbors(Activity source, Activity destination){
        ArrayList<Activity> sourceOutNeighbors = GRAPH.getActivitiesParents(source);
        for(Activity outNeighbor : sourceOutNeighbors){
            ActivityLink ogLink = GRAPH.getActivityLink(outNeighbor, source);
            GRAPH.linkActivities(destination, outNeighbor, ogLink.onChance, ogLink.IOD, ogLink.SOD, ogLink.COD, ogLink.mathematicaColor);
        }
    }

    private void pathInclusion(Path path){
        while(true){
            if(pathLabel.equals(GRAPH.goalActivity.name)){
                ActivityLink link = ogNet.getActivityLink(ogNet.goalActivity, ogNet.getNode(currentNode.name));
                if(link != null)
                    GRAPH.linkActivities(currentNode, GRAPH.goalActivity, link.onChance, link.IOD, link.SOD, link.COD, link.mathematicaColor);
                else
                    GRAPH.linkActivities(currentNode, GRAPH.goalActivity, 1., 1., 1., 1., "");
                newPath.add(GRAPH.goalActivity);
                break;
            }else{
                Activity pathNode = path.activities.get(pCounter);
                Activity newNode = (Activity) pathNode.clone();
                newNode.id = GRAPH.getRandomUnusedId();
                ActivityLink link = ogNet.getActivityLink(pathNode, ogNet.getNode(currentNode.name));
                if(link != null)
                    GRAPH.linkActivities(currentNode, newNode, link.onChance, link.IOD, link.SOD, link.COD, link.mathematicaColor);
                else
                    GRAPH.linkActivities(currentNode, newNode, 1., 1., 1., 1., "");
                currentNode = newNode;
                newPath.add(currentNode);
                pathLabel = forwardPathLabel(path);
            }
        }
    }

    private void suffixMinimization(Path path) {
        pCounter = path.activities.size() - 1;
        currentNode = GRAPH.goalActivity;
        pathLabel = backwardPathLabel(path);
        while (true){
            Activity pathNode = path.activities.get(pCounter);
            if(pathNode != GRAPH.startActivity){
                Activity matchInNeighbor = findMatchSuffixInNeighbor(currentNode, pathNode);
                if(matchInNeighbor != null /*&& GRAPH.getActivityLink(path.activities.get(pCounter + 1), matchInNeighbor) == null*/){
                    ActivityLink link = GRAPH.getActivityLink(currentNode, pathNode);
                    GRAPH.unlinkNodes(currentNode, pathNode);
                    backwardPathLabel(path);
                    Activity newPathNode = path.activities.get(pCounter);
                    GRAPH.linkActivities(newPathNode, matchInNeighbor, link.onChance, link.IOD, link.SOD, link.COD, link.mathematicaColor);
                    currentNode = matchInNeighbor;
                    GRAPH.removeNode(pathNode);
                }else{
                    break;
                }
            }else{
                break;
            }
        }
    }

    private Activity findMatchSuffixInNeighbor(Activity currentNode, Activity pathNode){
        ArrayList<Activity> inNeighbors = GRAPH.getActivitiesChildren(currentNode);
        for(Activity inNeighbor : inNeighbors){
            if(pathNode != inNeighbor && nodesAreSimilar(pathNode, inNeighbor)){
                return inNeighbor;
            }
        }
        return null;
    }

    private boolean nodesAreSimilar(Activity pathNode, Activity inNeighbor) {
        if(pathNode.name.equals(inNeighbor.name)){
            Set<Activity> set1 = new HashSet<>(GRAPH.getActivitiesParents(pathNode));
            Set<Activity> set2 = new HashSet<>(GRAPH.getActivitiesParents(inNeighbor));
            return set1.equals(set2);
        }
        return false;
    }
}