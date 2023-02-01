package org.mitre.thor.network;

import org.mitre.thor.input.InputQueue;
import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.CriticalityAnalysis;
import org.mitre.thor.analyses.data_holders.NetworkAnalysisDataHolder;
import org.mitre.thor.analyses.data_holders.NetworkCriticalityTrial;
import org.mitre.thor.analyses.data_holders.NodeAnalysisDataHolder;
import org.mitre.thor.analyses.data_holders.NodeCriticalityTrial;
import org.mitre.thor.network.attack.AttackChain;
import org.mitre.thor.network.attack.Route;
import org.mitre.thor.network.links.ActivityLink;
import org.mitre.thor.network.links.FactorLink;
import org.mitre.thor.network.links.GroupLink;
import org.mitre.thor.network.links.Link;
import org.mitre.thor.network.nodes.*;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;
import org.mitre.thor.analyses.target.TargetType;

import java.text.DecimalFormat;
import java.util.*;

public class Network implements Cloneable{

    public static boolean CAPTURE_RUN_MAP = false;
    public final static HashMap<Integer, Double> RUN_MAP = new HashMap<>();

    public ArrayList<NetworkAnalysisDataHolder> analysisDataHolders = new ArrayList<>();
    public List<InputQueue> inputQueues = new ArrayList<>();
    private final List<Node> nodes = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
    public Activity startActivity = null;
    public Activity goalActivity = null;
    public boolean connectsToGoal = false;
    public final List<Activity> networkOrder = new ArrayList<>();

    private final DecimalFormat alphaDF = new DecimalFormat("###.##");

    public Network(){}

    public Network(List<InputQueue> inputQueues){
        this.inputQueues = inputQueues;
    }

    public Network(List<Node> nodes, List<Link> links, List<InputQueue> inputQueues){
        this.inputQueues = inputQueues;
        for(Node node : nodes){
            addNode(node);
        }
        this.links = links;
    }

    public void addNetworkAnalysisDataHolders(boolean addTrials){
        this.analysisDataHolders.clear();
        for(InputQueue queue : inputQueues){
            NetworkAnalysisDataHolder result = new NetworkAnalysisDataHolder(queue);
            if(queue.containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                CriticalityAnalysis analysis = (CriticalityAnalysis) queue.getAnalysis(AnalysesForm.CRITICALITY);
                result.networkCriticalityTrials = new NetworkCriticalityTrial[analysis.trials];
                result.trialNetworks = new Network[analysis.trials];
                if(addTrials){
                    for(int i = 0; i < analysis.trials; i++){
                        result.networkCriticalityTrials[i] = new NetworkCriticalityTrial();
                        result.trialNetworks[i] = getTrialNetwork();
                    }
                }
            }
            this.analysisDataHolders.add(result);
        }
    }

    /**
     * Creates a copy of the original network. Then, in that copy, each link has a % to be removed. Using that %, this function
     * removes random links which leaves the copy of the network to be different from the original
     *
     * @return modified version of the original network
     */
    private Network getTrialNetwork(){
        Network cNetwork = null;
        try {
            cNetwork = (Network) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        assert cNetwork != null;
        ArrayList<ActivityLink> links = cNetwork.getActivityLinks();
        for(int i = 0; i < links.size();){
            if(Math.random() > links.get(i).onChance){
                cNetwork.unlinkNodes(cNetwork.getLinks().get(i).parent, cNetwork.getLinks().get(i).child);
            }else{
                i++;
            }
        }

        for(Activity activity : cNetwork.getActivities()){
            activity.customActivityRule =  ((Activity)this.getNode(activity.id)).customActivityRule;
        }

        cNetwork.addNetworkAnalysisDataHolders(false);
        cNetwork.findStartAndEnd();
        return cNetwork;
    }

    /**
     * Creates an activity, but it does not add it to the list of nodes
     * @param name the name of the node
     * @param id the id of the node
     * @param checkDuplicates check if there is already a node with these properties
     * @return the activity created
     */
    public Activity createActivity(String name, int id, int decorativeID, boolean checkDuplicates){
        Activity tActivity = null;
        if(checkDuplicates){
            tActivity = (Activity) getNode(name, id);
        }
        if(tActivity == null){
            tActivity = new Activity(name);
            tActivity.id = id;
            tActivity.decorativeID = decorativeID;
        }
        return tActivity;
    }

    public SisterActivity createSisterActivity(String name, int id, int decorativeID, boolean checkDuplicates){
        SisterActivity tActivity = null;
        if(checkDuplicates){
            tActivity = (SisterActivity) getNode(name, id);
        }
        if(tActivity == null){
            tActivity = new SisterActivity(name);
            tActivity.id = id;
            tActivity.decorativeID = decorativeID;
        }
        return tActivity;
    }

    public Factor createFactor(String name, int id, int factorId, boolean checkDuplicates){
        Factor tFactor = null;
        if (checkDuplicates) {
            tFactor = (Factor) getNode(name, id);
        }
        if(tFactor == null){
            tFactor = new Factor(name);
            tFactor.id = id;
            tFactor.decorativeID = factorId;
            tFactor.attachDID = true;
        }
        return tFactor;
    }

    public int getRandomUnusedId(){
        int max = 99999999;
        int min = 1;
        int range = max - min + 1;
        boolean idUsed = true;
        double id;
        do {
            id = (Math.random() * range) + min;
            if(!isIdUsed(id)){
                idUsed = false;
            }
        } while (idUsed);
        return (int) id;
    }

    public boolean isIdUsed(double id){
        boolean isUsed = false;
        for(Node node : getNodes()){
            if(node.id == id){
                isUsed = true;
                break;
            }
        }
        return isUsed;
    }

    /**
     * Adds a node to the list of nodes in the network if it isn't already in there
     * @param node the nodes to be added
     */
    public void addNode(Node node){
        if(!nodes.contains(node)){
            nodes.add(node);
            addAnalysisDataHolder(node);
        }
    }

    public void addLink(Link link){
        if(!nodes.contains(link.child))
            addNode(link.child);
        if(!nodes.contains(link.parent))
            addNode(link.parent);
        if(!links.contains(link))
            links.add(link);
    }

    private void addAnalysisDataHolder(Node node){
        node.analysisDataHolders = new ArrayList<>();
        for(InputQueue inputQueue : inputQueues){
            NodeAnalysisDataHolder result = new NodeAnalysisDataHolder(inputQueue, inputQueue.targetAnalyses);
            if(inputQueue.containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                CriticalityAnalysis analysis = (CriticalityAnalysis) inputQueue.getAnalysis(AnalysesForm.CRITICALITY);
                result.nodeCriticalityTrials = new NodeCriticalityTrial[analysis.trials];
                for(int i = 0; i < analysis.trials; i++){
                    result.nodeCriticalityTrials[i] = new NodeCriticalityTrial();
                }
            }
            node.analysisDataHolders.add(result);
        }
    }

    /**
     * Removes a node from the network as well as any links associated with that node
     * @param node the node to be removed
     */
    public void removeNode(Node node){
        for(int i = 0; i < links.size();){
            if(links.get(i).parent == node || links.get(i).child == node){
                removeLink(links.get(i));
            }else{
                i++;
            }
        }
        node.analysisDataHolders = new ArrayList<>();
        nodes.remove(node);
    }

    /**
     * @return the list of nodes
     */
    public ArrayList<Node> getNodes(){
        return (ArrayList<Node>) nodes;
    }

    public ArrayList<Activity> getActivities(){
        ArrayList<Activity> activities = new ArrayList<>();
        for(Node node : nodes){
            if(node.getClass().equals(Activity.class)){
                activities.add((Activity) node);
            }
        }
        return activities;
    }

    public ArrayList<Factor> getFactors(){
        ArrayList<Factor> factors = new ArrayList<>();
        for(Node node : nodes){
            if(node.getClass().equals(Factor.class)){
                factors.add((Factor) node);
            }
        }
        return factors;
    }

    public ArrayList<Group> getGroups(String tag){
        ArrayList<Group> groups = new ArrayList<>();
        for(Node node : nodes){
            if(node.getClass().equals(Group.class)){
                if(tag.equals("") || ((Group) node).tag.equals(tag)){
                    groups.add((Group) node);
                }
            }
        }
        return groups;
    }

    public ArrayList<Group> getAllGroups(){
        ArrayList<Group> groups = new ArrayList<>();
        for(Node node : nodes){
            if(node.getClass().equals(Group.class)){
                groups.add((Group) node);
            }
        }
        return groups;
    }

    public void removeGroups(String tag){
        ArrayList<Group> groups = getGroups(tag);
        for(Group group : groups){
            removeNode(group);
        }
    }

    /**
     * Finds a Node by its id
     * @param id the id of the Activity one wishes to find
     * @return the node you're looking for. If none is found, the output will be null
     */
    public Node getNode(double id){
        Node out = null;
        for(Node node : nodes){
            if(node.id == id){
                out = node;
                break;
            }
        }
        return out;
    }

    public Node getNode(String name){
        Node out = null;
        for(Node node : nodes){
            if(node.name.equals(name)){
                out = node;
                break;
            }
        }
        return out;
    }

    /**
     * Get a Node by checking nodeName and id
     * @param nodeName the name of the node you're looking for
     * @param id the id of the node you're looking for
     * @return the node you're looking for. If none is found, the output will be null
     */
    public Node getNode(String nodeName, double id){
        Node out = null;
        for(Node node : nodes){
            if(node.name.equals(nodeName) && node.id == id){
                out = node;
                break;
            }
        }
        return out;
    }

    /**
     * Get the children of a particular node
     * @param node the node which children's you seek
     * @return the list of children
     */
    public ArrayList<Node> getNodeChildren(Node node){
        ArrayList<Node> children = new ArrayList<>();
        for(Link link : links){
            if(link.parent == node && link.child != null){
                children.add(link.child);
            }
        }
        return children;
    }

    /**
     * Get the parents of a particular node
     * @param node the node which parent's you seek
     * @return the list of parents
     */
    public ArrayList<Node> getNodeParents(Node node){
        ArrayList<Node> parents = new ArrayList<>();
        for(Link link : links){
            if(link.child == node && link.parent != null){
                parents.add(link.parent);
            }
        }
        return parents;
    }

    /**
     * Get a link by checking its parent and child nodes
     * @param parent the parent node of the link you want to get
     * @param child the child node of the link you want to get
     * @return the link you're looking for. If none is found, the output will be null
     */
    public Link getLink(Node parent, Node child){
        Link tLink = null;
        for(Link link : links){
            if(link.parent == parent && link.child == child){
                tLink = link;
                break;
            }
        }
        return tLink;
    }

    public ActivityLink getActivityLink(Node parent, Node child){
        ActivityLink tLink = null;
        for(ActivityLink link : getActivityLinks()){
            if(link.parent == parent && link.child == child){
                tLink = link;
                break;
            }
        }
        return tLink;
    }

    /**
     * @return the list of links
     */
    public ArrayList<Link> getLinks(){
        return (ArrayList<Link>) links;
    }

    public ArrayList<ActivityLink> getActivityLinks(){
        ArrayList<ActivityLink> links = new ArrayList<>();
        for(Link link : getLinks()){
            if(link instanceof ActivityLink){
                links.add((ActivityLink) link);
            }
        }
        return links;
    }

    public ArrayList<FactorLink> getFactorLinks(){
        ArrayList<FactorLink> links = new ArrayList<>();
        for(Link link : getLinks()){
            if(link instanceof FactorLink){
                links.add((FactorLink) link);
            }
        }
        return links;
    }

    public ArrayList<GroupLink> getGroupLinks(){
        ArrayList<GroupLink> links = new ArrayList<>();
        for(Link link : getLinks()){
            if(link instanceof GroupLink){
                links.add((GroupLink) link);
            }
        }
        return links;
    }

    public void linkNodes(Node child, Node parent, String mathematicaColor){
        if(!nodes.contains(child))
            addNode(child);
        if(!nodes.contains(parent))
            addNode(parent);

        if(getLink(child, parent) == null){
            Link link = new GroupLink(child, parent);
            link.mathematicaColor = mathematicaColor;
            links.add(link);
        }
    }

    /**
     * Link two nodes together
     * @param parent the parent of the link
     * @param child the child of the link
     * @param onChance the chance of the link being on (Range 0-1)
     * @param iod fdna/odinn value
     * @param sod fdna/odinn value
     * @param cod fdna/odinn value
     * @param mathematicaColor the color of the link when displayed in Mathematica
     */
    public void linkActivities(Activity child, Activity parent, double onChance, double iod, double sod, double cod, String mathematicaColor){
        if(!nodes.contains(parent))
            addNode(parent);
        if(!nodes.contains(child))
            addNode(child);

        if(getLink(parent, child) == null){
            ActivityLink link = new ActivityLink(child, parent);
            link.onChance = onChance;
            link.IOD = iod;
            link.SOD = sod;
            link.COD = cod;
            links.add(link);
            link.mathematicaColor = mathematicaColor;
        }
    }

    public void linkFactorToNode(Factor factor, Node node, double fvi, boolean binary, String mathematicaColor){
        if(!nodes.contains(node))
            addNode(node);
        if(!nodes.contains(factor))
            addNode(factor);

        if(getLink(node, factor) == null){
            FactorLink link = new FactorLink(factor, node);
            link.fvi = fvi;
            link.binary = binary;
            link.mathematicaColor = mathematicaColor;
            links.add(link);
        }
    }

    public void linkNodeToGroup(Node node, Group group, String mathematicaColor){
        if(!nodes.contains(node))
            addNode(node);
        if(!nodes.contains(group))
            addNode(group);

        if(getLink(node, group) == null){
            GroupLink link = new GroupLink(node, group);
            link.mathematicaColor = mathematicaColor;
            if(!group.nodes.contains(node)){
                group.nodes.add(node);
            }
            links.add(link);
        }
    }

    /**
     * Unlinks two nodes
     * @param parent the parent of link to be removed
     * @param child the child of the link to be removed
     */
    public void unlinkNodes(Node parent, Node child){
        Link link = getLink(parent, child);
        links.remove(link);
    }

    /**
     * Removes a link from the links list
     * @param link the link to be removed
     */
    public void removeLink(Link link)
    {
        links.remove(link);
    }

    public ArrayList<Node> filterReal(List<Node> n, boolean excludeStartEnd){
        ArrayList<Node> real = new ArrayList<>();
        for(Node node : n){
            if(node.isReal || (excludeStartEnd && (node == startActivity || node == goalActivity))){
                real.add(node);
            }
        }
        return real;
    }

    public ArrayList<Node> filterPhysical(List<Node> nodes){
        ArrayList<Node> physical = new ArrayList<>();
        for(Node node : nodes){
            if(node.isPhysical){
                physical.add(node);
            }
        }
        return physical;
    }

    /**
     * Get any real activities that don't have any real children
     * @return an arraylist of the real leaf activities
     */
    public ArrayList<Activity> getRealLeafActivities(){
        ArrayList<Activity> leafActivities = new ArrayList<>();
        ArrayList<Activity> activities = getActivities();
        for(Activity activity : activities){
            if(activity != null){
                ArrayList<Node> children = getNodeChildren(activity);
                if(children.size() == 0 && activity.isReal){
                    leafActivities.add(activity);
                }else{
                    boolean leaf = true;
                    for(Node child : children){
                        if(child instanceof Activity && child.isReal){
                            leaf = false;
                            break;
                        }
                    }
                    if(leaf){
                        leafActivities.add(activity);
                    }
                }
            }
        }
        return leafActivities;
    }

    /**
     * Get any Activities without parents that are also an Activity
     * @return a list of these activities
     */
    public ArrayList<Activity> getActivitiesWithoutActivityParents(){
        ArrayList<Activity> noParents = new ArrayList<>();
        ArrayList<Activity> activities = getActivities();
        for(Activity activity : activities){
            if(activity != null){
                ArrayList<Activity> parents = getActivitiesParents(activity);
                if(parents.isEmpty()){
                    noParents.add(activity);
                }
            }
        }
        return noParents;
    }

    /**
     * Get any Activities without children that are also an Activity
     * @return a list of these activities
     */
    public ArrayList<Activity> getActivitiesWithoutActivityChildren(){
        ArrayList<Activity> noChildren = new ArrayList<>();
        ArrayList<Activity> activities = getActivities();
        for(Activity activity : activities){
            if(activity != null){
                ArrayList<Activity> children = getActivitiesChildren(activity);
                if(children.isEmpty()){
                    noChildren.add(activity);
                }
            }
        }
        return noChildren;
    }

    public ArrayList<Group> getGroupChildren(Node node, String tag){
        ArrayList<Group> gChildren = new ArrayList<>();
        ArrayList<Node> children = getNodeChildren(node);
        for(Node n : children){
            if(n.getClass().equals(Group.class) && ((Group) n).tag.equals(tag)){
                gChildren.add((Group) n);
            }
        }
        return gChildren;
    }

    public ArrayList<Factor> getFactorChildren(Node node){
        ArrayList<Factor> fChildren = new ArrayList<>();
        ArrayList<Node> children = getNodeChildren(node);
        for(Node n : children){
            if(n.getClass().equals(Factor.class)){
                fChildren.add((Factor) n);
            }
        }
        return fChildren;
    }

    public ArrayList<Activity> getActivitiesChildren(Node node){
        ArrayList<Activity> aChildren = new ArrayList<>();
        ArrayList<Node> children = getNodeChildren(node);
        for(Node n : children){
            if(n.getClass().equals(Activity.class)){
                aChildren.add((Activity) n);
            }
        }
        return aChildren;
    }

    public ArrayList<Activity> getActivitiesParents(Activity activity){
        ArrayList<Activity> aParents = new ArrayList<>();
        ArrayList<Node> parents = getNodeParents(activity);
        for(Node n : parents){
            if(n.getClass().equals(Activity.class)){
                aParents.add((Activity) n);
            }
        }
        return aParents;
    }

    /**
     * Determine if all the children of an activity have an assigned operability
     * @param activity the parent of all the children
     * @param rollUpIndex the index of the analysis
     * @return a boolean value
     */
    public boolean childrenActivitiesScoresKnown(Activity activity, int rollUpIndex){
        for(Link link : links){
            if(link.parent == activity && link.child instanceof Activity){
                if(Double.isNaN(link.child.analysisDataHolders.get(rollUpIndex).operability)){
                    return false;
                }
            }
        }
        return true;
    }


    private final ArrayList<Node> iterated = new ArrayList<>();

    public void clearIteratedNodes(){
        iterated.clear();
    }

    /**
     * Determine if all the children of an activity have been iterated. Clear the iterated list before running
     * @return a boolean value
     */
    public boolean childrenActivitiesAreIterated(Activity activity){
        ArrayList<Activity> aChildren = getActivitiesChildren(activity);
        for(Node child : aChildren){
            if(!iterated.contains(child)){
                return false;
            }
        }
        return true;
    }

    public void generatePairs(String tag, TargetType targetType){
        ArrayList<Node> nodes = targetType.getTargetNodes(this);

        for(int i = 0; i < nodes.size() - 1; i++){
            for(int a = i + 1; a < nodes.size(); a++) {
                Group group = new Group(nodes.get(i).decorativeID + "-" +
                        nodes.get(a).decorativeID);
                group.tag = tag;
                group.subTag = "pair";
                group.id = getRandomUnusedId();
                group.decorativeID = -1;
                group.nodes.add(nodes.get(i));
                group.nodes.add(nodes.get(a));
                addNode(group);
            }
        }
    }

    public void generateTriples(String tag, TargetType targetType){
        ArrayList<Node> nodes = targetType.getTargetNodes(this);

        for(int i = 0; i < nodes.size() - 2; i++){
            for(int a = i + 1; a < nodes.size() - 1; a++){
                for(int j = a + 1; j < nodes.size(); j++){
                    Group group = new Group(nodes.get(i).decorativeID + "-" +
                            nodes.get(a).decorativeID + "-" +
                            nodes.get(j).decorativeID);
                    group.tag = tag;
                    group.subTag = "triple";
                    group.id = getRandomUnusedId();
                    group.decorativeID = -1;
                    group.nodes.add(nodes.get(i));
                    group.nodes.add(nodes.get(a));
                    group.nodes.add(nodes.get(j));
                    addNode(group);
                }
            }
        }
    }

    /**
     * Find and assign the start and end activities by looking at the activities without children and the activities
     * without parents
     */
    //TODO: make this return a boolean so I can stop the program if this fails
    public boolean findStartAndEnd(){
        List<Node> activitiesWithoutParents = filterPhysical(new ArrayList<>(getActivitiesWithoutActivityParents()));
        List<Node> activitiesWithoutChildren = filterPhysical(new ArrayList<>(getActivitiesWithoutActivityChildren()));

        //create a start node if not already present
        if(activitiesWithoutChildren.size() > 1){
            startActivity = this.createActivity("start", 5555, 5555,false);
            startActivity.isReal = false;
            startActivity.customActivityRule = RollUpEnum.OR;
            this.addNode(startActivity);
            for(Node activity : activitiesWithoutChildren){
                this.linkActivities(startActivity, (Activity) activity,1, 0, 0, 0, null);
            }
        }else if(activitiesWithoutChildren.size() == 1){
            startActivity = (Activity) activitiesWithoutChildren.get(0);
            startActivity.isReal = false;
        }else{
            System.out.println("Could not establish a start node. All nodes have children");
            return false;
        }


        startActivity.mathematicaColor = "Green";
        startActivity.mathematicaSize = "largeSize";

        //create a goal node if not already present
        if(activitiesWithoutParents.size() > 1){
            System.out.println("Could not establish a goal node. Please only have 1 top node");
            System.out.println("Top nodes are:");
            for(Node node : activitiesWithoutParents){
                System.out.println("\t" + node.name);
            }
            return false;
        }else if(activitiesWithoutParents.size() == 1){
            goalActivity = (Activity) activitiesWithoutParents.get(0);
            goalActivity.isReal = false;
        }else{
            System.out.println("Could not establish a goal node. All nodes have parents");
            return false;
        }
        goalActivity.mathematicaColor = "Blue";
        goalActivity.mathematicaSize = "largeSize";

        return true;
    }

    /**
     * Finds the order of Activities in which to run the Operability Tree Algorithm
     * @param targetActivity the starting Activity
     */
    public void findNetworkOrder(Activity targetActivity){
        networkOrder.clear();
        clearIteratedNodes();
        networkOrderHelper(targetActivity);
    }

    private void networkOrderHelper(Activity targetActivity) {
        if(childrenActivitiesAreIterated(targetActivity) && !networkOrder.contains(targetActivity)){
            networkOrder.add(targetActivity);
            iterated.add(targetActivity);
            ArrayList<Activity> aParents = getActivitiesParents(targetActivity);
            for(Activity parent : aParents){
                networkOrderHelper(parent);
            }
        }
    }

    /**
     * The operability algorithm used to find the operability of each Activity starting from the bottom start activity.
     * This method uses the network order list as a guide. Fakes nodes are always on.
     * @param rollUpRule the Roll up Rule to be used when calculation the operability
     * @param rollUpIndex the index of the analysis
     */
    public void oAlgorithmUsingOrder(RollUpEnum rollUpRule, int rollUpIndex, TargetType targetType){
        for(Activity activity : networkOrder){
            double operability = activity.analysisDataHolders.get(rollUpIndex).operability;

            if(Double.isNaN(operability) && activity != startActivity){

                if(targetType == TargetType.NODES){
                    activity.SE = activity.staticSE;
                }

                double scoreFromActivities = getScoreFromChildrenActivities(activity, rollUpRule, rollUpIndex);
                if(targetType == TargetType.FACTORS && (rollUpRule != RollUpEnum.ODINN && rollUpRule != RollUpEnum.ODINN_FTI)
                && (rollUpRule != RollUpEnum.CUSTOM || (activity.customActivityRule != RollUpEnum.ODINN && activity.customActivityRule != RollUpEnum.ODINN_FTI))){
                    double scoreFromFactors = getScoreFromFactors(activity, rollUpRule, rollUpIndex);
                    operability = Math.min(scoreFromActivities, scoreFromFactors);
                }else{
                    //WHEN TARGETING NODES OR WHEN TARGETING FACTORS WITH ODINN
                    operability = scoreFromActivities;
                }
            }else if(Double.isNaN(operability) && activity == startActivity){
                operability = 100.0;
            }

            activity.setOperability(rollUpIndex, operability);
        }
    }

    private double getScoreFromChildrenActivities(Activity activity, RollUpEnum rollUpRule, int rollUpIndex){
        RollUpEnum aRule = rollUpRule;
        if(rollUpRule == RollUpEnum.CUSTOM){
            aRule = activity.customActivityRule;
        }

        List<Node> aChildren = new ArrayList<>(getActivitiesChildren(activity));
        return  aRule.calculateNodeScoreFromChildren(activity, this, aChildren, rollUpIndex);
    }

    private double getScoreFromFactors(Activity activity, RollUpEnum rollUpRule, int rollUpIndex){
        //  GET ANY GROUPS THAT ARE PARENTS OF FACTORS
        List<Group> fGroups = getGroups("factors");
        List<Node> gChildren = new ArrayList<>();
        // GET THE GROUPS THAT ARE CHILDREN OF THE NODE 'node'
        for(Group group : fGroups){
            List<Node> parents = getNodeParents(group);
            if(parents.contains(activity)){
                gChildren.add(group);
            }
        }
        //  GET THE FACTORS THAT ARE CHILDREN OF THE NODE 'node'
        List<Node> fChildren = new ArrayList<>(getFactorChildren(activity));

        RollUpEnum gRule = rollUpRule;
        if(rollUpRule == RollUpEnum.CUSTOM){
            gRule = activity.customGroupRule;
        }

        RollUpEnum fRule = rollUpRule;
        if(rollUpRule == RollUpEnum.CUSTOM){
            fRule = activity.customFactorRule;
        }

        // CALCULATE THE OPERABILITY OF EACH GROUP
        for(Node child : gChildren){
            List<Node> gFChildren = new ArrayList<>(getFactorChildren(child));
            if(!gFChildren.isEmpty()){
                for(Node factor : gFChildren){ // IF A GROUP HAS FACTORS, USE THIS TO CALC OPERABILITY
                    if(Double.isNaN(factor.analysisDataHolders.get(rollUpIndex).operability)){
                        double operability = fRule.getFactorScore((Factor) factor);
                        factor.setOperability(rollUpIndex, operability);
                    }
                }
                double operability = fRule.calculateNodeScoreFromChildren(child, this, gFChildren, rollUpIndex);
                ((Group) child).setGroupNodeOperability(rollUpIndex, operability);
            }else{
                //   IF A GROUP DOES NOT HAVE FACTORS, SET THE GROUP OPERABILITY TO 100
                double operability = 100.;
                ((Group) child).setGroupNodeOperability(rollUpIndex, operability);
            }
        }

        //  CALCULATE THE OPERABILITY OF EACH FACTOR
        for(Node factor : fChildren){
            if(Double.isNaN(factor.analysisDataHolders.get(rollUpIndex).operability)){
                double operability = fRule.getFactorScore((Factor) factor);
                factor.setOperability(rollUpIndex, operability);
            }
        }

        ArrayList<Node> children = new ArrayList<>();
        RollUpEnum finalRule = null;
        if(!fChildren.isEmpty()){
            children.addAll(fChildren);
            finalRule = fRule;
        }
        // NOTE THAT IF AN ACTIVITY IS LINKED TO BOTH (FACTORS GROUPS) AND (FACTORS) THAN THE FINAL ROLL UP RULE USED
        // WILL BE THE NODE -> GROUPS RULE, SO THE INDIVIDUAL FACTORS WILL BE TREATED AS A GROUP
        if(!gChildren.isEmpty()){
            children.addAll(gChildren);
            finalRule = gRule;
        }

        if(finalRule != null){
            return gRule.calculateNodeScoreFromChildren(activity, this, children, rollUpIndex);
        }else{
            return 100.0;
        }
    }

    /**
     * @param color determines if the loop links are colored brown or not
     * @return an array of nodes that are part of a loop. If list is empty, there are no loops.
     */
    public List<Activity> getLoops(boolean color, Network colorNetwork){
        //Creates a fake copy of the network
        Network nCopy = null;
        try{
            nCopy = (Network) this.clone();
        }catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        //Keep removing nodes without parents until there are no nodes missing a parent
        while(true){
            assert nCopy != null;
            ArrayList<Activity> noParents = nCopy.getActivitiesWithoutActivityParents();
            if(noParents.isEmpty()){
                break;
            }
            for(Activity activity : noParents){
                nCopy.removeNode(activity);
            }
        }
        //Keep removing nodes without children until there are no nodes missing a child
        while(true){
            ArrayList<Activity> noChildren = nCopy.getActivitiesWithoutActivityChildren();
            if(noChildren.isEmpty()){
                break;
            }
            for(Activity activity : noChildren){
                nCopy.removeNode(activity);
            }
        }
        //Use the fake loop to identify the real loop in the real network by comparing similar ids
        ArrayList<Activity> loopActivities = new ArrayList<>();
        for(Activity fakeActivity : nCopy.getActivities()){
            for(Activity activity : getActivities()){
                if(activity.id == fakeActivity.id){
                    loopActivities.add(activity);
                    break;
                }
            }
        }

        if(color) {
            for (Link link : colorNetwork.getLinks()) {
                boolean hasParent = false;
                boolean hasChild = false;

                for (Activity a : loopActivities) {
                    if (a.id == link.parent.id) {
                        hasParent = true;
                    } else if (a.id == link.child.id) {
                        hasChild = true;
                    }
                }

                if(hasChild && hasParent){
                    link.mathematicaColor = "Brown";
                    link.child.mathematicaColor = "Brown";
                    link.parent.mathematicaColor = "Brown";
                }
            }
        }
        return loopActivities;
    }

    /**
     * Creates a new network from a collection of paths
     * @param paths a collection of paths
     * @return a completely new network
     */
    public Network createCopyNetworkFromPaths(PathCollection paths){
        Network network = new Network();
        network.inputQueues = this.inputQueues;
        network.addNetworkAnalysisDataHolders(true);
        for(Path path : paths.getPaths()) {
            for (int i = 0; i < path.activities.size() - 1; i++) {
                Activity pActivity = path.activities.get(i + 1) instanceof SisterActivity ? ((SisterActivity) path.activities.get(i + 1)).origin : path.activities.get(i + 1);
                Activity cActivity = path.activities.get(i) instanceof SisterActivity ? ((SisterActivity) path.activities.get(i)).origin : path.activities.get(i);
                Link link = this.getLink(pActivity, cActivity);
                double onChance = 1;
                double iod = 0.0;
                double sod = 0.0;
                double cod = 0.0;
                String mathe = null;
                if (link instanceof ActivityLink) {
                    sod = ((ActivityLink) link).SOD;
                    cod = ((ActivityLink) link).COD;
                    iod = ((ActivityLink) link).IOD;
                    onChance = ((ActivityLink) link).onChance;
                    mathe = link.mathematicaColor;
                }
                Activity iParent = (Activity) network.getNode(path.activities.get(i + 1).id);
                if(iParent == null){
                    iParent = createActivity(path.activities.get(i + 1).name, path.activities.get(i + 1).id, path.activities.get(i + 1).decorativeID,false);
                    iParent.A = path.activities.get(i + 1).A;
                    iParent.B = path.activities.get(i + 1).B;
                    iParent.customActivityRule = path.activities.get(i+1).customActivityRule;
                    iParent.mathematicaColor = path.activities.get(i + 1).mathematicaColor;
                    iParent.mathematicaSize = path.activities.get(i + 1).mathematicaSize;
                }
                if(path.activities.get(i+1).customActivityRule == null){
                    iParent.customActivityRule = RollUpEnum.OR;
                }
                Activity iChild = (Activity) network.getNode(path.activities.get(i).id);
                if(iChild == null){
                    iChild = createActivity(path.activities.get(i).name, path.activities.get(i).id, path.activities.get(i).decorativeID,false);
                    iChild.A = path.activities.get(i).A;
                    iChild.B = path.activities.get(i).B;
                    iChild.customActivityRule = path.activities.get(i).customActivityRule;
                    iChild.mathematicaColor = path.activities.get(i).mathematicaColor;
                    iChild.mathematicaSize = path.activities.get(i).mathematicaSize;
                }
                if(path.activities.get(i).customActivityRule == null){
                    iChild.customActivityRule = RollUpEnum.OR;
                }
                network.linkActivities(iChild, iParent, onChance, iod, sod, cod, mathe);


                if(path.activities.get(i) == startActivity){
                    network.startActivity = iChild;
                }

                if(path.activities.get(i + 1) == goalActivity){
                    network.goalActivity = iParent;
                }

                iChild.loopLevel = path.activities.get(i).loopLevel;
                iParent.loopLevel = path.activities.get(i + 1).loopLevel;
                iChild.isReal = path.activities.get(i).isReal;
                iParent.isReal = path.activities.get(i + 1).isReal;
            }
        }
        return network;
    }

    /**
     * @param start the starting index
     * @param targetAmount the target amount of critical nodes
     * @param rollUpIndex the index of the analysis
     * @return n ordered array of nodes based on each node's alpha value. Where, the higher the alpha value, the more important the node
     */
    public ArrayList<Node> getTopNodes(int start, int targetAmount, AnalysesForm analysis,  int rollUpIndex, TargetType targetType){
        ArrayList<Node> temp = targetType.getTargetNodes(this);
        temp.sort((o1, o2) -> {
            double a1 = Double.NaN;
            double a2 = Double.NaN;
            if(analysis == AnalysesForm.CRITICALITY){
                a1 = o1.analysisDataHolders.get(rollUpIndex).colorScore;
                a2 = o2.analysisDataHolders.get(rollUpIndex).colorScore;
            }else if(analysis == AnalysesForm.CG){
                a1 = o1.analysisDataHolders.get(rollUpIndex).cgSCore;
                a2 = o2.analysisDataHolders.get(rollUpIndex).cgSCore;
            }
            if(Double.isNaN(a2) || a1 > a2) return -1;
            if(Double.isNaN(a1) || a2 > a1) return 1;
            return 0;
        });
        ArrayList<Node> iNodes = new ArrayList<>();
        for(int i = 0; i < temp.size(); i++){
            double thisNode = -1;
            double lastNode = 1;
            if(i != 0 && iNodes.size() >= 1 && analysis == AnalysesForm.CRITICALITY){
                thisNode = temp.get(i).analysisDataHolders.get(rollUpIndex).colorScore;
                lastNode = iNodes.get(iNodes.size() - 1).analysisDataHolders.get(rollUpIndex).colorScore;
            }else if(i != 0 && iNodes.size() >= 1 && analysis == AnalysesForm.CG){
                thisNode = roundAlpha(temp.get(i).analysisDataHolders.get(rollUpIndex).cgSCore);
                lastNode = roundAlpha(iNodes.get(iNodes.size() - 1).analysisDataHolders.get(rollUpIndex).cgSCore);
            }

            if(i >= start &&
                    (iNodes.size() <= targetAmount ||
                            (iNodes.size() > 0 &&
                                    lastNode == thisNode))){
                iNodes.add(temp.get(i));
            }
        }
        return iNodes;
    }

    public void resetOperabilities(int rollUpIndex){
        for(Node n : getNodes()){
            n.analysisDataHolders.get(rollUpIndex).operability = Double.NaN;
        }
        for(Activity a : getActivities()){
            a.SE = Double.NaN;
        }
    }

    public void setAttackChainOperabilities(AttackChain chain, int rollUpIndex) {
        for (int i = 0; i < chain.getRoutesSize(); i++) {
            if (chain.getRoute(i).getTargetNode() != null) {
                chain.getRoute(i).getTargetNode().setOperability(rollUpIndex, chain.getRoute(i).getOperability());
            }
        }
    }

    private double roundAlpha(double alpha){
        return Double.parseDouble(alphaDF.format(alpha));
    }

    /**
     * finds the paths between two nodes where each node can only appear in that path 'x' amount of times
     * @param currentActivity the starting activity
     * @param endActivity the goal activity
     * @param lPath the local path (just create a new empty arraylist for this don't worry about storing it)
     * @param paths the output collection of paths (this will be updated as the function is called)
     * @param maxOccurrences the max amount of times that a node can appear in a single path
     */
    public void getPaths(Activity currentActivity, Activity endActivity, Path lPath, PathCollection paths, int maxOccurrences){
        int occurrences = Collections.frequency(lPath.activities, currentActivity);
        if(currentActivity != endActivity && occurrences < maxOccurrences){
            Path localPath = new Path(lPath);
            localPath.activities.add(currentActivity);
            ArrayList<Activity> parents = this.getActivitiesParents(currentActivity);
            for(Activity parent : parents){
                getPaths(parent, endActivity, localPath, paths, maxOccurrences);
            }
        }else if(currentActivity == endActivity){
            Path localPath = new Path(lPath);
            localPath.activities.add(currentActivity);
            paths.addPath(localPath);
        }
    }

    public void colorNodesBasedOnPhiTable(int rollUpIndex, int tableRow, TargetType targetType){
        ArrayList<Node> nodes = targetType.getTargetNodes(this);
        nodes.addAll(this.getGroups("crit"));
        for(int i = 0; i < this.analysisDataHolders.get(rollUpIndex).possibilitiesTable.get(tableRow).size(); i++){
            int bin = analysisDataHolders.get(rollUpIndex).possibilitiesTable.get(tableRow).get(i);
            if(bin == 1){
                nodes.get(i).mathematicaColor = "Green";
            }else{
                nodes.get(i).mathematicaColor = "Red";
            }
        }
        double end = this.analysisDataHolders.get(rollUpIndex).endResults.get(tableRow);
        goalActivity.mathematicaColor = "RGBColor[0," + 255 * end + ",0]";
    }

    public String getMathematicaString(boolean useID){

        double root = Math.sqrt(getNodes().size());
        double multiplier = useID ? 1.5 : 1.0;
        double defaultSize = multiplier * ((Math.log10(root) / 10) * root);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("data = {");

        ArrayList<Node> usedNodes = new ArrayList<>();
        ArrayList<Link> usedLinks = new ArrayList<>();

        for(Link link : getLinks()){
            String parentNodeLabel = useID ? link.parent.getIDTag() + link.parent.decorativeID : link.parent.getDecorativeName();
            String childNodeLabel = useID ?  link.child.getIDTag() + link.child.decorativeID : link.child.getDecorativeName();
            stringBuilder.append("\"").append(childNodeLabel).append("\" -> \"").append(parentNodeLabel).append("\", ");

            if(!usedLinks.contains(link)){
                usedLinks.add(link);
            }

            if(!usedNodes.contains(link.child)){
                usedNodes.add(link.child);
            }
            if(!usedNodes.contains(link.parent)){
                usedNodes.add(link.parent);
            }
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1).append("}; \n");

        stringBuilder.append("scale = ").append(defaultSize).append(";\n")
                .append("medSize = scale * 1.3;\n")
                .append("largeSize = scale * 1.6;\n")
                .append("g = LayeredGraphPlot[data,\n ");

        String vertexLabel = useID ? "Placed[\"Name\", Center]" : "\"Name\"";
        String graphLayout = "GraphLayout -> {\"LayeredDigraphEmbedding\", \"Orientation\" -> Left},\n ";
        stringBuilder
                .append(graphLayout)
                .append("VertexLabels -> ")
                .append(vertexLabel)
                .append(",\n VertexStyle -> {");
        for(Node node : usedNodes){
            if(node.mathematicaColor != null){
                String iNodeLabel = useID ?  node.getIDTag() + node.decorativeID : node.getDecorativeName();
                stringBuilder.append("\"").append(iNodeLabel).append("\" -> ").append(node.mathematicaColor).append(", ");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(", White}")
                .append(",\n VertexSize -> {");
        for(Node node : usedNodes){
            if(node.mathematicaSize != null){
                String iNodeLabel = useID ?  node.getIDTag() + node.decorativeID : node.getDecorativeName();
                stringBuilder.append("\"").append(iNodeLabel).append("\" -> ").append(node.mathematicaSize).append(", ");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1)
                .append(", scale},\n ");

        StringBuilder noEdgesColor = new StringBuilder(stringBuilder);

        boolean colorLink = false;
        stringBuilder.append("EdgeStyle -> {");
        for(Link link : usedLinks){
            if(link.mathematicaColor != null){
                String parentNodeLabel = useID ? link.parent.getIDTag() + link.parent.decorativeID : link.parent.getDecorativeName();
                String childNodeLabel = useID ?  link.child.getIDTag() + link.child.decorativeID : link.child.getDecorativeName();
                stringBuilder.append("(\"")
                        .append(childNodeLabel)
                        .append("\" -> \"")
                        .append(parentNodeLabel)
                        .append("\") -> ")
                        .append(link.mathematicaColor)
                        .append(", ");
                colorLink = true;
            }
        }

        if(!colorLink){
            stringBuilder = noEdgesColor;
            stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append("]");
        }else{
            stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1)
                    .append("}]");
        }

        return stringBuilder.toString();
    }

    public String getPt1DaggerCode(int rollUpIndex){
        RollUpEnum rule = this.analysisDataHolders.get(rollUpIndex).queue.rollUpRule;
        ArrayList<Node> includedNodes = new ArrayList<>(getActivities());
        ArrayList<Factor> factors = getFactors();
        includedNodes.addAll(factors);
        ArrayList<Group> fGroups = getGroups("factors");
        includedNodes.addAll(fGroups);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<!-- ***** CLASSIFICATION: UNCLASSIFIED ***** -->\n");
        sb.append("<dagger_model schemaVersion=\"4.5\">\n\t<items>\n");
        for(Node node : includedNodes){
            sb.append("\t\t<item name=\"").append(node.getDecorativeName()).append("\" id=\"").append(node.id).append("\"/>\n");
        }
        sb.append("\t</items>\n");
        sb.append("\n\t<layers>\n");

        sb.append("\t\t<layer name=\"Mission\" id=\"Mission\">\n");
        sb.append("\t\t\t<item>").append(goalActivity.id).append("</item>\n");
        sb.append("\t\t</layer>\n");

        sb.append("\t\t<layer name=\"Activities\" id=\"Activities\">\n");
        for(Activity activity : getActivities()){
            if(activity != this.goalActivity){
                sb.append("\t\t\t<item>").append(activity.id).append("</item>\n");
            }
        }
        sb.append("\t\t</layer>\n");


        if(fGroups.size() > 0){
            sb.append("\t\t<layer name=\"Groups\" id=\"Groups\">\n");
            for(Group group : fGroups){
                sb.append("\t\t\t<item>").append(group.id).append("</item>\n");
            }
            sb.append("\t\t</layer>\n");
        }

        if(factors.size() > 0){
            sb.append("\t\t<layer name=\"Factors\" id=\"Factors\">\n");
            for(Factor factor : factors){
                sb.append("\t\t\t<item>").append(factor.id).append("</item>\n");
            }
            sb.append("\t\t</layer>\n");
        }

        sb.append("\t</layers>\n");
        sb.append("\n\t<shards>\n\t\t<shard name=\"Default\" id=\"Default\">\n\t\t\t<program>\n\t\t\t\t<dependencies>\n");
        for(Link link : getLinks()){
            if(includedNodes.contains(link.child) || includedNodes.contains(link.parent)){
                sb.append("\t\t\t\t\t<dependency item=\"").append(link.parent.id).append("\">\n");
                sb.append("\t\t\t\t\t\t<depends_on>").append(link.child.id).append("</depends_on>\n");
                sb.append("\t\t\t\t\t</dependency>\n");
            }
        }
        sb.append("\t\t\t\t</dependencies>\n");

        return sb.toString();
    }

    public String getCGDaggerCode(int rollUpIndex, int cgMapKey, int atOp){
        ArrayList<Node> includedNodes = new ArrayList<>(getActivities());
        ArrayList<Factor> factors = getFactors();
        includedNodes.addAll(factors);
        ArrayList<Group> fGroups = getGroups("factors");
        includedNodes.addAll(fGroups);

        StringBuilder sb = new StringBuilder();
        sb.append("\t\t\t\t<indicators>\n");

        for(Node node : includedNodes){
            HashMap<Integer, Double>[] map = this.analysisDataHolders.get(rollUpIndex).cgRunMap.get(cgMapKey);
            double status = map[atOp].get(node.id) != null ? map[atOp].get(node.id)/100.0 : 1.0;
            sb.append("\t\t\t\t\t<indicator item=\"").append(node.id).append("\" key=\"General status\" valueType=\"STATUS\" implicitRefsAllowed=\"false\">\n");
            sb.append("\t\t\t\t\t\t<expression>result = status(").append(status).append(")</expression>\n");
            sb.append("\t\t\t\t\t</indicator>\n");
        }
        sb.append("\t\t\t\t</indicators>\n");
        sb.append("\t\t\t</program>\n");
        sb.append("\t\t\t<status_overlays>\n\t\t\t\t<status_overlay name=\"General status\" id=\"General status\"/>\n\t\t\t</status_overlays>\n");
        sb.append("\t\t\t<dagger_alerts/>\n");
        sb.append("\t\t</shard>\n\t</shards>\n");;
        sb.append("\t<scenarios/>\n\t<groups/>\n\t<views/>\n\t<palettes/>\n\t<metadata name=\"default\" id=\"default\">\n");
        sb.append("\t\t<createdBy>THOR</createdBy>\n");
        sb.append("\t\t<modifiedBy>THOR</modifiedBy>\n");
        sb.append("\t\t<name>default</name>\n");
        sb.append("\t\t<namespace>default</namespace>\n");
        sb.append("\t\t<properties/>\n");
        sb.append("\t\t<version>1.0</version>\n");
        sb.append("\t</metadata>");
        sb.append("</dagger_model>");
        sb.append("<!-- ***** CLASSIFICATION: UNCLASSIFIED ***** -->");

        return sb.toString();
    }

    public String geCritGDaggerCode(int rollUpIndex, ArrayList<Node> critNodes){
        ArrayList<Node> includedNodes = new ArrayList<>(getActivities());
        ArrayList<Factor> factors = getFactors();
        includedNodes.addAll(factors);
        ArrayList<Group> fGroups = getGroups("factors");
        includedNodes.addAll(fGroups);

        StringBuilder sb = new StringBuilder();
        sb.append("\t\t\t\t<indicators>\n");

        for(Node node : includedNodes){
            double status = critNodes.contains(node) ? (100. - node.analysisDataHolders.get(rollUpIndex).colorScore) / 100. : 1.;
            sb.append("\t\t\t\t\t<indicator item=\"").append(node.id).append("\" key=\"General status\" valueType=\"STATUS\" implicitRefsAllowed=\"false\">\n");
            sb.append("\t\t\t\t\t\t<expression>result = status(").append(status).append(")</expression>\n");
            sb.append("\t\t\t\t\t</indicator>\n");
        }
        sb.append("\t\t\t\t</indicators>\n");
        sb.append("\t\t\t</program>\n");
        sb.append("\t\t\t<status_overlays>\n\t\t\t\t<status_overlay name=\"General status\" id=\"General status\"/>\n\t\t\t</status_overlays>\n");
        sb.append("\t\t\t<dagger_alerts/>\n");
        sb.append("\t\t</shard>\n\t</shards>\n");;
        sb.append("\t<scenarios/>\n\t<groups/>\n\t<views/>\n\t<palettes/>\n\t<metadata name=\"default\" id=\"default\">\n");
        sb.append("\t\t<createdBy>THOR</createdBy>\n");
        sb.append("\t\t<modifiedBy>THOR</modifiedBy>\n");
        sb.append("\t\t<name>default</name>\n");
        sb.append("\t\t<namespace>default</namespace>\n");
        sb.append("\t\t<properties/>\n");
        sb.append("\t\t<version>1.0</version>\n");
        sb.append("\t</metadata>");
        sb.append("</dagger_model>");
        sb.append("<!-- ***** CLASSIFICATION: UNCLASSIFIED ***** -->");

        return sb.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();

        for(Node node : this.getNodes()){
            nodes.add(node.clone());
        }

        for(int i = 0; i < nodes.size(); i++){
            nodes.get(i).analysisDataHolders = new ArrayList<>();
            for(int a = 0; a < this.nodes.get(i).analysisDataHolders.size(); a++){
                NodeAnalysisDataHolder result = (NodeAnalysisDataHolder) this.nodes.get(i).analysisDataHolders.get(a).clone();
                nodes.get(i).analysisDataHolders.add(result);
            }
        }

        if(!this.links.isEmpty()){
            for(ActivityLink value : this.getActivityLinks()) {
                Activity child = (Activity) nodes.get(this.nodes.indexOf(value.child));
                Activity parent = (Activity) nodes.get(this.nodes.indexOf(value.parent));
                links.add(new ActivityLink(child, parent, value.IOD, value.SOD, value.COD, value.onChance, value.mathematicaColor));
            }
            for(FactorLink value : this.getFactorLinks()) {
                Factor child = (Factor) nodes.get(this.nodes.indexOf(value.child));
                Node parent = nodes.get(this.nodes.indexOf(value.parent));
                links.add(new FactorLink(child, parent, value.fvi, value.binary, value.mathematicaColor));
            }
            for(GroupLink value : this.getGroupLinks()){
                Node child = nodes.get(this.nodes.indexOf(value.child));
                Node parent = nodes.get(this.nodes.indexOf(value.parent));
                links.add(new GroupLink(child, parent));
            }
        }

        Network n = new Network(nodes, links, this.inputQueues);

        //Properly clone groups
        ArrayList<Group> groups = n.getAllGroups();
        for(Group g : groups){
            ArrayList<Node> newNodes = new ArrayList<>();
            Group oG = (Group) this.getNode(g.id);
            for(Node node : oG.nodes){
                newNodes.add(n.getNode(node.id));
            }
            g.nodes = newNodes;
        }

        return n;
    }
}
