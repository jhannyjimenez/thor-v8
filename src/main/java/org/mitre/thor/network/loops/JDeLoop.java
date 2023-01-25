package org.mitre.thor.network.loops;

import org.mitre.thor.network.Network;
import org.mitre.thor.network.Path;
import org.mitre.thor.network.PathCollection;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Node;
import org.mitre.thor.network.nodes.SisterActivity;

import java.util.ArrayList;
import java.util.Collections;

public class JDeLoop {

    private final Network inNetwork;
    private final Network outNetwork;

    private JDeLoop(Network network){
        this.inNetwork = network;

        PathCollection paths = new PathCollection();
        //Simple Paths
        inNetwork.getPaths(inNetwork.startActivity, inNetwork.goalActivity, new Path(new ArrayList<>()), paths, 2);
        PathCollection finalPaths = loopLeveler(paths);
        outNetwork = inNetwork.createCopyNetworkFromPaths(finalPaths);
    }

    /**
     * small subset function that helps deloop networks
     * @param paths the collection of paths to deloop
     * @return a fixed collection of paths where each node does not go below their level
     */
    private PathCollection loopLeveler(PathCollection paths){
        ArrayList<String> usedPaths = new ArrayList<>();
        for(Activity activity : inNetwork.getActivities()){
            activity.loopLevel = 0;
        }

        paths.getPaths().sort((o1, o2) -> {
            //Put subsets last
            int o1L = o1.getNumberOfLinkDuplicatesRightAfterOneAnother();
            int o2L = o2.getNumberOfLinkDuplicatesRightAfterOneAnother();

            int o1D = o1.getNumberOfDuplicateNodes();
            int o2D = o2.getNumberOfDuplicateNodes();
            int o1U = o1.getNumberOfUniqueNodes();
            int o2U = o2.getNumberOfUniqueNodes();

            if (o1L != o2L) {
                if (o1L < o2L) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (o1D != o2D) {
                if (o1D > o2D) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (o1U != o2U) {
                if (o1U > o2U) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        });

        PathCollection newList = new PathCollection();
        for (Path path : paths.getPaths()) {
            ArrayList<Activity> newPathActivities = new ArrayList<>();
            int prevScore = 0;
            for (int i = 0; i < path.activities.size(); i++) {
                Activity cActivity = path.activities.get(i);
                if (cActivity.loopLevel == 0) {
                    cActivity.loopLevel = i;
                }
                if (cActivity != inNetwork.goalActivity && cActivity != inNetwork.startActivity && cActivity.loopLevel <= prevScore) {
                    Node n = inNetwork.getNode(cActivity.name + ": " + (prevScore + 1));
                    if(n != null){
                        newPathActivities.add((Activity) n);
                    }else{
                        int id = (int) inNetwork.getRandomUnusedId();
                        SisterActivity copy = inNetwork.createSisterActivity(cActivity.name + ": " + (prevScore + 1), id, id, true);
                        copy.customActivityRule = cActivity.customActivityRule;
                        copy.isReal = false;
                        copy.origin = cActivity;
                        copy.mathematicaColor = null;
                        copy.mathematicaSize = cActivity.mathematicaSize;
                        inNetwork.addNode(copy);
                        newPathActivities.add(copy);
                    }
                    prevScore = prevScore + 1;
                } else {
                    prevScore = cActivity.loopLevel;
                    newPathActivities.add(cActivity);
                }
            }
            Path newPath = new Path(newPathActivities);
            String newPathString = newPath.toString();
            if(!usedPaths.contains(newPathString)){
                newList.addPath(newPath);
                usedPaths.add(newPathString);
            }
        }
        return newList;
    }

    public static Network createDeLoopedGraph(Network network){
        return (new JDeLoop(network)).outNetwork;
    }

}
