package org.mitre.thor.network;

import org.mitre.thor.network.links.Link;
import org.mitre.thor.network.nodes.Activity;

import java.util.ArrayList;
import java.util.List;

public class Path implements Cloneable{
    public List<Activity> activities;

    public Path(ArrayList<Activity> activities){
        this.activities = new ArrayList<>(activities);
    }

    public Path(Path p){this.activities = new ArrayList<>(p.activities);}

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Object clone = super.clone();
        ArrayList<Activity> newList = new ArrayList<>(activities);
        return new Path(newList);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(activities.size()).append("] ");
        builder.append("{");
        for(Activity activity : activities){
             builder.append(activity.toString()).append(" -> ");
        }
        builder.delete(builder.toString().length() - 4, builder.toString().length());
        builder.append("}");
        return builder.toString();
    }

    public int getNumberOfUniqueNodes(){
        ArrayList<Activity> usedActivities = new ArrayList<>();
        int counter = 0;
        for(Activity activity : activities){
            if(!usedActivities.contains(activity)){
                counter++;
                usedActivities.add(activity);
            }
        }
        return counter;
    }

    public int getNumberOfDuplicateNodes(){
        ArrayList<Activity> usedActivities = new ArrayList<>();
        ArrayList<Activity> countedActivities = new ArrayList<>();
        int counter = 0;
        for(Activity activity : activities){
            if(usedActivities.contains(activity)){
                if(!countedActivities.contains(activity)){
                    counter++;
                    countedActivities.add(activity);
                }
            }else{
                usedActivities.add(activity);
            }
        }
        return counter;
    }

    public int getNumberOfLinkDuplicates(){
        ArrayList<Link> links = new ArrayList<>();
        int count = 0;
        for(int i = 0; i < activities.size() - 1; i++){
            Link link = new Link(activities.get(i + 1), activities.get(i));
            links.add(link);
        }
        for(int i = 0; i < links.size(); i++){
            for(int a = 0; a < links.size(); a++){
                if(a != i && links.get(i).parent.id == links.get(a).parent.id && links.get(i).child.id == links.get(a).child.id){
                    count++;
                }
            }
        }
        return count;
    }

    public int getNumberOfLinkDuplicatesRightAfterOneAnother(){
        ArrayList<Link> links = new ArrayList<>();
        int count = 0;
       for(int i = 0; i < activities.size() - 1; i++){
           Link link = new Link(activities.get(i + 1), activities.get(i));
           links.add(link);
       }
       for(int i = 0; i < links.size(); i++){
           for(int a = 0; a < links.size(); a++){
               if(a == i + 2 && links.get(i).parent.id == links.get(a).parent.id && links.get(i).child.id == links.get(a).child.id){
                   count++;
               }
           }
       }
       return count;
    }
}
