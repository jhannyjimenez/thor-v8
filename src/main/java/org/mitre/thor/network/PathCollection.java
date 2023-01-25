package org.mitre.thor.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PathCollection {
    private final ArrayList<Path> paths = new ArrayList<>();
    public final ArrayList<String> pathsUsed = new ArrayList<>();

    public ArrayList<Path> getPaths(){
        return paths;
    }

    public void addPath(Path path){
        String pString = path.toString();
        if(!pathsUsed.contains(pString)){
            pathsUsed.add(pString);
            paths.add(path);
        }
    }

    public void sortByLongest(){
        paths.sort((o1, o2) -> Integer.compare(o2.activities.size(), o1.activities.size()));
    }

    public void sortByShortest(){
        paths.sort(Comparator.comparingInt(o -> o.activities.size()));
    }

    public void shuffle(){
        Collections.shuffle(paths);
    }

}
