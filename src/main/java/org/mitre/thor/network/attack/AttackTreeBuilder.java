package org.mitre.thor.network.attack;

import javafx.util.Pair;
import org.mitre.thor.analyses.target.TargetType;
import org.mitre.thor.input.Input;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import static org.mitre.thor.network.attack.AttackTreeBuilder.NODE_DF;

public class AttackTreeBuilder {

    public final static DecimalFormat NODE_DF = new DecimalFormat("0.00");
    private final DecisionTree decisionTree;

    private final ArrayList<DecisionNode> nodes = new ArrayList<>();
    private final ArrayList<DecisionLink> links = new ArrayList<>();
    private int tabCount;
    private boolean treeStopped;

    private final DecisionOption decisionOption;
    private final boolean CAP_BUDGET;
    private final double BUDGET;
    private final boolean INCLUDE_TEXT_STOP;
    private final boolean INCLUDE_MATH_STOP;
    private final ArrayList<DecisionNode> stopNodes = new ArrayList<>();

    private final TextTree textTree = new TextTree();
    private String textTreeString = "";

    private final Input input;
    private final int maxPoints;
    private final TargetType targetType;

    public AttackTreeBuilder(DecisionTree decisionTree, DecisionOption option,
                             boolean useBudget, double budget, boolean includeTextStop, boolean includeMathStop, Input input,
                             int maxPoints, TargetType targetType){
        this.decisionTree = decisionTree;
        this.decisionOption = option;
        this.CAP_BUDGET = useBudget;
        this.BUDGET = budget;
        this.INCLUDE_TEXT_STOP = includeTextStop;
        this.INCLUDE_MATH_STOP = includeMathStop;
        this.input = input;
        this.maxPoints = maxPoints;
        this.targetType = targetType;

        //this.attackPoints.sort(Comparator.comparingDouble(this::getAttackPointValue));
        //Collections.reverse(this.attackPoints);
        //System.out.println("Test at AttackDecisionTree Constructor");
    }

    public DecisionNode addNode(double ac, double emo, double cost, String rdLabel, String rdComment, Decision decision){
        int id = getUnusedNodeId();
        DecisionNode node = new DecisionNode(ac, emo, cost, rdLabel, rdComment, id);
        node.decision = decision;
        nodes.add(node);
        return node;
    }

    public void removeNodeRecursively(DecisionNode node, boolean removeFromTextTree){
        ArrayList<DecisionLink> linksToRemove = new ArrayList<>();
        ArrayList<DecisionNode> nodesToRemove = new ArrayList<>();
        for(DecisionLink link : links){
            if (node == link.childNode || node == link.parentNode) {
                linksToRemove.add(link);
                if(node == link.childNode)
                    nodesToRemove.add(link.parentNode);
            }
        }
        for(DecisionLink link : linksToRemove){
            links.remove(link);
            if(removeFromTextTree){
                textTree.deleteObject(link);
            }
        }

        nodes.remove(node);
        stopNodes.remove(node);
        if(removeFromTextTree){
            textTree.deleteObject(node);
        }
        for(DecisionNode n : nodesToRemove){
            removeNodeRecursively(n, removeFromTextTree);
        }
    }

    private int getUnusedNodeId(){
        Random r = new Random();
        while(true){
            int id = r.nextInt();
            boolean used = false;
            for(DecisionNode node : nodes){
                if(node.uid == id){
                    used = true;
                    break;
                }
            }
            if(!used){
                return id;
            }
        }
    }

    public DecisionLink addLink(String name, String comment, DecisionNode childNode, DecisionNode parentNode, double probability, double routeAMI, Route route){
        DecisionLink link =  new DecisionLink(name, comment, childNode, parentNode, probability, routeAMI);
        link.route = route;
        links.add(link);
        return link;
    }

    public String getMathematicaCode(boolean full){
        double root = Math.sqrt(nodes.size());
        double multiplier = 1.0;
        double defaultSize = multiplier * ((Math.log10(root) / 10) * root);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("data = {");

        ArrayList<DecisionNode> usedNodes = new ArrayList<>();
        ArrayList<DecisionLink> usedLinks = new ArrayList<>();

        for(DecisionLink link : links){
            if(INCLUDE_MATH_STOP || (!stopNodes.contains(link.childNode) && !stopNodes.contains(link.parentNode))){
                stringBuilder.append("\"").append(link.childNode.uid).append("\" -> \"").append(link.parentNode.uid).append("\", ");
                if(!usedLinks.contains(link)){
                    usedLinks.add(link);
                }
                if(!usedNodes.contains(link.childNode)){
                    usedNodes.add(link.childNode);
                }
                if(!usedNodes.contains(link.parentNode)){
                    usedNodes.add(link.parentNode);
                }
            }
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1).append("}; \n");
        stringBuilder.append("scale = ").append(defaultSize).append(";\n")
                .append("medSize = scale * 1.3;\n")
                .append("largeSize = scale * 1.6;\n")
                .append("g = LayeredGraphPlot[data,\n ");

        String graphLayout = "GraphLayout -> {\"LayeredDigraphEmbedding\", \"Orientation\" -> Left},\n ";
        stringBuilder.append(graphLayout)
                .append("VertexLabels -> {");
        for(DecisionNode node : usedNodes){
            if (full) {
                stringBuilder.append("\"")
                        .append(node.uid)
                        .append("\" -> Placed[\"")
                        .append(node)
                        .append("\",Tooltip], ");
            }else {
                stringBuilder.append("\"")
                        .append(node.uid)
                        .append("\" -> \"")
                        .append(node.rdLabel)
                        .append("\", ");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append("},\n VertexStyle -> {")
                .append("White}")
                .append(",\n VertexSize -> {")
                .append("scale},\n ");

        stringBuilder.append("EdgeLabels -> {");
        for(DecisionLink link : usedLinks){
            if (full) {
                stringBuilder.append("(\"")
                        .append(link.childNode.uid)
                        .append("\" -> \"")
                        .append(link.parentNode.uid)
                        .append("\") -> Placed[\"")
                        .append(link)
                        .append("\",Tooltip], ");
            } else {
                stringBuilder.append("(\"")
                        .append(link.childNode.uid)
                        .append("\" -> \"")
                        .append(link.parentNode.uid)
                        .append("\") -> \"")
                        .append(link.name)
                        .append("\", ");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1).deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append("}]");

        return stringBuilder.toString();
    }

    public double getDecisionTreeValue() {
        double totalValue = 0.0;
        for (DecisionNode stopNode : stopNodes) {
            totalValue += getDecisionNodeChance(stopNode) * stopNode.ami;
        }
        return totalValue;
    }

    public double calculateRealImpacts() {
        DecisionNode start = getStartDNode();
        realImpactHelper(start);
        return start.rmi;
    }

    public void realImpactHelper(DecisionNode target) {
        ArrayList<DecisionLink> childLinks = getChildLinks(target);
        double totalValue = 0.0;
        for (DecisionLink link : childLinks) {
            if (Double.isNaN(link.parentNode.rmi)) {
                realImpactHelper(link.parentNode);
            }
            totalValue += link.parentNode.rmi * link.probability;
        }
        target.rmi = totalValue;
    }

    public double getDecisionNodeChance(DecisionNode node) {
        double chance = 1.0;
        DecisionNode target = node;
        while (true) {
            ArrayList<DecisionLink> parentLinks = getParentLinks(target);
            if (parentLinks.isEmpty()) {
                break;
            }
            chance *= parentLinks.get(0).probability;
            target = parentLinks.get(0).childNode;
        }
        return chance;
    }

    private ArrayList<DecisionLink> getParentLinks(DecisionNode node) {
        ArrayList<DecisionLink> parentLinks = new ArrayList<>();
        for (DecisionLink link : links) {
            if (link.parentNode == node) {
                parentLinks.add(link);
            }
        }
        return parentLinks;
    }

    private ArrayList<DecisionLink> getChildLinks(DecisionNode node) {
        ArrayList<DecisionLink> childLinks = new ArrayList<>();
        for (DecisionLink link : links) {
            if (link.childNode == node) {
                childLinks.add(link);
            }
        }
        return childLinks;
    }

    private DecisionNode getStartDNode() {
        ArrayList<DecisionNode> starts = new ArrayList<>(nodes);
        for (DecisionLink link : links) {
            starts.remove(link.parentNode);
        }
        return starts.get(0);
    }

    public void buildTree(int rollUpI){
        textTree.clear(); stopNodes.clear(); nodes.clear(); links.clear(); tabCount = 0;
        buildTreeLayer(decisionTree.getStartDecisions(), addNode( 0.0, Double.NaN, 0.0 , "", "", null), new AttackChain(), 0, rollUpI, new ArrayList<>(), 0.0);
        //trimStops();
        calculateRealImpacts();
        this.textTreeString = formTextTree();
    }

    private void trimStops() {
        DecisionNode start = getStartDNode();
        trimHelper(start);
    }

    private void trimHelper(DecisionNode target) {
        for (DecisionLink childLink: getChildLinks(target)) {
            if (!nodeReachesSuccessfulEnd(childLink.parentNode)) {
                removeNodeRecursively(childLink.parentNode, true);
                DecisionNode newStop = addNode(target.ac, 0.0, 0.0, "end", "Stop Tree", null);
                stopNodes.add(newStop);
                addLink(childLink.name, childLink.comment, target, newStop, childLink.probability, childLink.ami, childLink.route);
            } else {
                trimHelper(childLink.parentNode);
            }
        }
    }

    private boolean nodeReachesSuccessfulEnd(DecisionNode target) {
        if (stopNodes.contains(target) && target.ami > 0) {
            return true;
        } else {
            for (DecisionLink cLink: getChildLinks(target)) {
                if (nodeReachesSuccessfulEnd(cLink.parentNode)) {
                    return true;
                }
            }
            return false;
        }
    }

    public String getTextTreeString(){
        return this.textTreeString;
    }

    private void buildTreeLayer(ArrayList<Decision> options, DecisionNode node, AttackChain currentPath, int depth, int rollUpI, ArrayList<AttackPoint> attackPoints, double prevBestDecisionVal){
        if (prevBestDecisionVal == 100.0) {
            addStopNode(node, currentPath, attackPoints, rollUpI, depth);
            return;
        }

        ArrayList<AttackPoint> newSimulations = decisionTree.simulateAttackPoints(input, currentPath, rollUpI, maxPoints / (depth + 1), targetType);
        for (AttackPoint point : newSimulations) {
            if (attackPoints.contains(point)) {
                attackPoints.get(attackPoints.indexOf(point)).count += 1;
            } else {
                attackPoints.add(point);
            }
        }
        // If there are still options left, get the best decision and the impact it has on the goal
        Pair<Decision, Double> bestDecision = !options.isEmpty() ? getBestDecision(options, currentPath, attackPoints) : null;
        // Only continue if there is valid possible decision
        if(bestDecision != null && bestDecision.getKey() != null){
            // Calculate the new accumulated cost
            double nAC = currentPath.getAccumulatedCost() + bestDecision.getKey().getCost();
            // Break up the decision and impact into two separate variables
            Decision chosenDecision = bestDecision.getKey(); double emo = bestDecision.getValue();
            // Get all the possible routes that the tree can take from the chosen decision
            ArrayList<Route> routeOptions = decisionTree.getRouteOptions(chosenDecision.getId());
            // Sort the routes based on how much they impact the goal. Where the most impactful routes will be added
            // first

            currentPath.addDecision(chosenDecision);
            ArrayList<ValuedRoute> valuedRoutes = new ArrayList<>();
            for(Route route : routeOptions){
                valuedRoutes.add(new ValuedRoute(route, getAvgRouteValue(route, currentPath, attackPoints)));
            }
            valuedRoutes.sort(Comparator.comparingDouble(ValuedRoute::getValue).reversed());
            for(ValuedRoute route : valuedRoutes){
                // Continue to the next layer
                DecisionNode newNode = graphTree(route.getRoute(), chosenDecision, node, nAC, emo, depth, route.getValue());
                continueBuildingTree(route.getRoute(), newNode, currentPath, depth, rollUpI, attackPoints, bestDecision.getValue());
            }
        }else{
            addStopNode(node, currentPath, attackPoints, rollUpI, depth);
        }
    }

    private void addStopNode(DecisionNode node, AttackChain currentPath, ArrayList<AttackPoint> attackPoints, int rollUpI, int depth){
        // Configure the text tree to stop
        treeStopped = true;
        stopNodes.add(node);
        node.rdLabel = "end";
        node.rdComment = "Stop Tree";
        node.ac = currentPath.getAccumulatedCost();
        node.cost = 0.0;
        double mi = getExactChainValue(currentPath, attackPoints, rollUpI);
        node.rmi = mi;
        node.ami = mi;

        textTree.addLine(node ,depth + tabCount);
    }

    private void continueBuildingTree(Route route, DecisionNode newNode, AttackChain currentPath, int depth, int rollUpI, ArrayList<AttackPoint> attackPoints, double prevBDV){
        AttackChain newPath = currentPath.clone();
        newPath.addRoute(route);
        int nDepth = depth + this.tabCount; this.tabCount = 0;

        ArrayList<Decision> options = decisionTree.getDecisionOptions(newPath);
        ArrayList<Decision> finalOptions = new ArrayList<>();
        for (Decision option : options) {
            if (!newPath.containsDecision(option)){
                finalOptions.add(option);
            }
        }

        buildTreeLayer(finalOptions, newNode, newPath, nDepth, rollUpI, attackPoints, prevBDV);
    }

    private DecisionNode graphTree(Route route, Decision decision, DecisionNode preNode, double ac, double ami, int depth, double routeAMI){
        DecisionNode newNode = addNode(ac, ami, decision.getCost(),"STOP", "STOP" ,null);
        preNode.rdLabel = String.valueOf(decision.getId());
        preNode.rdComment = decision.getDescription();
        preNode.ac = ac;
        preNode.ami = ami;
        preNode.cost = decision.getCost();
        preNode.decision = decision;
        DecisionLink newLink = addLink(String.valueOf(route.getFullId()), route.getComment(), preNode, newNode, route.getProbSuccess(), routeAMI, route);

        //Text tree
        if(!treeStopped){
            textTree.addLine(preNode, depth);
        }else{
            treeStopped = false;
        }
        this.tabCount++;

        if(!newLink.comment.isEmpty() && !newLink.comment.isBlank()){
            textTree.addLine(newLink, depth + 1);
            this.tabCount++;
        }else{
            this.tabCount--;
        }
        return newNode;
    }

    private ArrayList<DecisionNode> getParentNodes(DecisionNode child){
        ArrayList<DecisionNode> parents = new ArrayList<>();
        for(DecisionLink link : links){
            if(link.childNode.equals(child)){
                parents.add(link.parentNode);
            }
        }
        return parents;
    }

    private String formTextTree(){
        String[] preLines = textTree.toString().split("\\r?\\n");
        String[] lines = new String[preLines.length + 1];
        lines[0] = "START:";
        System.arraycopy(preLines, 0, lines, 1, preLines.length);

        int maxChars = 0;
        int maxCostLength = 0;
        int maxACLength = 0;
        int maxAMILength = 0;
        int maxRMILength = 0;
        int maxProbLength = 0;
        for(String line : lines){
            maxChars = Math.max(line.length(), maxChars);
        }
        for(int i = 0; i < textTree.getLineCount(); i++){
            Object object = textTree.getObjectAt(i);
            if(object instanceof DecisionNode){
                DecisionNode node = (DecisionNode) object;
                maxCostLength = Math.max(String.valueOf((int) node.cost).length(), maxCostLength);
                maxACLength = Math.max(String.valueOf((int) node.ac).length(), maxACLength);
                maxAMILength = Math.max(NODE_DF.format(node.ami).length(), maxAMILength);
                maxRMILength = Math.max(NODE_DF.format(node.rmi).length(), maxRMILength);
            }else if(object instanceof DecisionLink){
                DecisionLink link = (DecisionLink) object;
                maxAMILength = Math.max(NODE_DF.format(link.ami).length(), maxAMILength);
                maxProbLength = Math.max(String.valueOf(Math.round(link.probability * 100.0)).length(), maxProbLength);
            }
        }
        int padding = 3;
        int leftBorderIndex = maxChars + padding;
        // Right borderse
        int costBorderIndex =  leftBorderIndex + padding;
        int acBorderIndex = costBorderIndex + padding;
        int rmiBorderIndex = acBorderIndex + padding;
        int amiBorderIndex = rmiBorderIndex + padding;
        int probBorderIndex = amiBorderIndex + padding;
        char borderChar = '|';

        int historyIndex = -1;
        int firstIndex = leftBorderIndex + 2;
        int secondIndex = firstIndex + padding + maxCostLength + 1; //The additional 1 are for symbols like $ or %
        int thirdIndex = secondIndex + padding + maxACLength + 1;
        int fourthIndex = thirdIndex + padding + maxRMILength;
        int fifthIndex = fourthIndex + padding + maxAMILength;
        int end = fifthIndex + padding + 1;

        for(int i = 0; i < lines.length; i++){
            lines[i] = insertChartAt(lines[i], 0, borderChar);
            lines[i] = addExtendedCharAt(lines[i], leftBorderIndex, borderChar);
            lines[i] = addExtendedCharAt(lines[i], costBorderIndex, borderChar);
            lines[i] = addExtendedCharAt(lines[i], acBorderIndex, borderChar);
            lines[i] = addExtendedCharAt(lines[i], rmiBorderIndex, borderChar);
            lines[i] = addExtendedCharAt(lines[i], amiBorderIndex, borderChar);
            lines[i] = addExtendedCharAt(lines[i], probBorderIndex, borderChar);

            double cost = Double.NaN;
            double ac = Double.NaN;
            double rmi = Double.NaN;
            double ami = Double.NaN;
            double prob = Double.NaN;

            if(historyIndex != -1 && historyIndex < textTree.getLineCount()){
                Object object = textTree.getObjectAt(historyIndex);
                if(object instanceof DecisionNode){
                    DecisionNode node = (DecisionNode) object;
                    cost = node.cost;
                    ac = node.ac;
                    ami = node.ami;
                    rmi = node.rmi;
                }else if(object instanceof DecisionLink){
                    DecisionLink link = (DecisionLink) object;
                    prob = link.probability;
                    ami = link.ami;
                }
            }

            String costString = !Double.isNaN(cost) ? "$" + (int) cost : "-";
            lines[i] = appendValueIntoString(lines[i], firstIndex, costString, maxCostLength + 1);

            String acString = !Double.isNaN(ac) ? "$" + (int) ac : "-";
            lines[i] = appendValueIntoString(lines[i], secondIndex, acString, maxACLength + 1);

            String rmiString = !Double.isNaN(rmi) ? NODE_DF.format(rmi) : "-";
            lines[i] = appendValueIntoString(lines[i], thirdIndex, rmiString, maxRMILength);

            String amiString = !Double.isNaN(ami) ? NODE_DF.format(ami) : "-";
            lines[i] = appendValueIntoString(lines[i], fourthIndex, amiString, maxAMILength);

            String probString = !Double.isNaN(prob) ? Math.round(prob * 100.0) + "%" : "-";
            lines[i] = appendValueIntoString(lines[i], fifthIndex, probString, maxProbLength + 1);

            historyIndex++;
        }

        String hLine = "|                                                                                            " +
                "                                                                                                   " +
                "                                                                                                   ";
        /*
        hLine = insertChartAt(hLine, 0, borderChar);
        hLine = addExtendedCharAt(hLine, leftBorderIndex, borderChar);
        hLine = addExtendedCharAt(hLine, costBorderIndex, borderChar);
        hLine = addExtendedCharAt(hLine, acBorderIndex, borderChar);
        hLine = addExtendedCharAt(hLine, rmiBorderIndex, borderChar);
        hLine = addExtendedCharAt(hLine, amiBorderIndex, borderChar);
        hLine = addExtendedCharAt(hLine, probBorderIndex, borderChar);
         */

        hLine = appendValueIntoString(hLine, 3, "TREE", 4);
        hLine = appendValueIntoString(hLine, firstIndex, "COST", maxCostLength);
        hLine = appendValueIntoString(hLine, secondIndex, "AC", maxACLength);
        hLine = appendValueIntoString(hLine, thirdIndex, "RMI", maxRMILength);
        hLine = appendValueIntoString(hLine, fourthIndex, "AMI", maxAMILength);
        hLine = appendValueIntoString(hLine, fifthIndex, "PROB", maxProbLength);
        hLine = hLine.substring(0, end + 1) + "|";

        StringBuilder out = new StringBuilder();
        String FullUnderScoreLine = fillStringUpToChar(end + 2, '_');
        String InsideUnderScoreLine = "|" + fillStringUpToChar(end, '_') + "|";
        out.append(FullUnderScoreLine).append("\n");
        out.append(hLine).append("\n");
        out.append(InsideUnderScoreLine).append("\n");
        for(String line : lines){
            out.append(line).append("\n");
        }
        out.append(InsideUnderScoreLine);
        return out.toString();
    }

    private String insertChartAt(String inString, int charIndex, char character){
        StringBuilder out = new StringBuilder(inString);
        out.insert(charIndex, character);
        return out.toString();
    }

    private String addExtendedCharAt(String inString, int charIndex, char character){
        StringBuilder out = new StringBuilder(inString);
        int length = inString.length();
        while (length <= charIndex){
            if(length != charIndex){
                out.append(" ");
            }else{
                out.append(character);
            }
            length++;
        }
        return out.toString();
    }

    private String fillStringUpToChar(int charIndex, char character){
        int r = Math.max(0, charIndex);
        return String.valueOf(character).repeat(r);
    }

    private String appendValueIntoString(String inString, int stringIndex, String value, int targetLength){
        StringBuilder out = new StringBuilder(inString);
        out.insert(stringIndex, value);
        if(value.length() < targetLength){
            out.insert(stringIndex + value.length(), " ".repeat(targetLength - value.length()));
        }
        return out.toString();
    }

    private String replaceValueIntoString(String inString, int stringIndex, String value){
        StringBuilder out = new StringBuilder(inString);
        for(int i = 0; i < value.length(); i++){
            out.replace(stringIndex + i, stringIndex + i + 1, String.valueOf(value.charAt(i)));
        }
        return out.toString();
    }

    private Pair<Decision, Double> getBestDecision(ArrayList<Decision> decisions, AttackChain currentPath, ArrayList<AttackPoint> attackPoints){
        Decision bestDecision = null;
        double bestAvgImpact = 0.0;

        for(Decision decision : decisions){
            boolean withinBudget = !CAP_BUDGET || currentPath.getAccumulatedCost() + decision.getCost() <= BUDGET;
            if (withinBudget) {
                double avgImpact = getAvgDecisionValue(decision, currentPath, attackPoints);
                if(avgImpact > bestAvgImpact){
                    bestAvgImpact = avgImpact;
                    bestDecision = decision;
                }
            }
        }
        return new Pair<>(bestDecision, bestAvgImpact);
    }

    private double getAvgDecisionValue(Decision decision, AttackChain prevPath, ArrayList<AttackPoint> attackPoints){
        AttackChain newChain = new AttackChain(prevPath);
        newChain.addDecision(decision);
        return getAvgChainValue(newChain, attackPoints);
    }

    private double getAvgRouteValue(Route route, AttackChain prevPath, ArrayList<AttackPoint> attackPoints){
        AttackChain newChain = new AttackChain(prevPath);
        newChain.addRoute(route);
        return getAvgChainValue(newChain, attackPoints);
    }

    private double getAvgChainValue(AttackChain chain, ArrayList<AttackPoint> attackPoints){
        double sum = 0.0;
        int count = 0;
        for(AttackPoint point : attackPoints){
            //TODO: test out difference when using path.equals() vs path.contains()
            if(point.path.containsChain2(chain)){
                double value = getAttackPointValue(point);
                sum += value * point.count;
                count += point.count;
                //sum += value;
                //count += 1;
            }
        }
        return count != 0 ? sum / count : 0.0;
    }

    private double getExactChainValue(AttackChain chain, ArrayList<AttackPoint> attackPoints, int rollUpI){
        for(AttackPoint point : attackPoints){
            if(point.path.equals(chain)){
                return getAttackPointValue(point);
            }
        }
        DecisionTree.runAttackChain(input, chain, rollUpI, targetType);

        return DecisionTree.getAttackPoint(input, chain, rollUpI).impact;
    }

    private double getAttackPointValue(AttackPoint point){
        double value = 0.0;
        if(decisionOption == DecisionOption.IMPACT){
            value = point.impact;
        }else if(decisionOption == DecisionOption.IMPACT_COST_RATIO){
            value = point.cost != 0 ? point.impact / point.cost : point.impact;
        }
        return value;
    }

    private boolean nodeIsLeaf(DecisionNode node){
        boolean isLeaf = true;
        for(DecisionLink link : links){
            if(link.childNode.equals(node)){
                isLeaf = false;
                break;
            }
        }
        return isLeaf;
    }
}

class DecisionNode {
    public Decision decision;
    public double ac;
    public double ami;
    public double rmi = Double.NaN;
    public double cost;
    public String rdLabel;
    public String rdComment;
    public final int uid;

    private final static DecimalFormat NODE_DF = new DecimalFormat("0.00");

    public DecisionNode(double ac, double ami, double cost, String rdLabel, String rdComment, int uid){
        this.ac = ac;
        this.ami = ami;
        this.cost = cost;
        this.rdLabel = rdLabel;
        this.rdComment = rdComment;
        this.uid = uid;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof  DecisionNode){
            return uid == ((DecisionNode) obj).uid;
        }
        return super.equals(obj);
    }

    public String toMathematicaLabel(){
        String idText = getIdText();
        return !rdComment.toLowerCase().equals("stop") ? this + idText : "STOP" + idText;
    }

    public String toSimplifiedMathematicaLabel(){
        return rdLabel + " #" + uid;
    }

    public String getNoIdLabel(){
        return rdLabel;
    }

    public String toTextTreeString(){
        return "'" + rdComment + "' " + rdLabel + ",";
    }

    @Override
    public String toString() {
        return "'" + rdComment + "' " + rdLabel + " | AC: " + NODE_DF.format(ac) + ", AMI: "
                + NODE_DF.format(ami) + ", RMI: " + NODE_DF.format(rmi) + " |";
    }

    private String getIdText(){
        return " ID{" + uid + "}";
    }
}

class DecisionLink {
    public Route route;
    public final String name;
    public final String comment;
    public final DecisionNode childNode;
    public DecisionNode parentNode;

    public final double probability;
    public final double ami;

    public DecisionLink(String name, String comment, DecisionNode childNode, DecisionNode parentNode, double probability, double ami) {
        this.name = name.stripTrailing().stripLeading();
        this.comment = comment;
        this.childNode = childNode;
        this.parentNode = parentNode;
        this.probability = probability;
        this.ami = ami;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DecisionLink){
            return
                    this.childNode.equals(((DecisionLink) obj).childNode) &&
                    this.parentNode.equals(((DecisionLink) obj).parentNode);
        }
        return super.equals(obj);
    }

    public String toTextTreeString(){
        return "'" + comment + "' " + name + ",";
    }

    @Override
    public String toString() {
        if(!Double.isNaN(probability))
            return "'" + comment + "' " + name + " | Prob: " + NODE_DF.format(probability) + " |";
        else
            return name;
    }

    public String toSimplifiedMathematicaLabel(){
        return name;
    }
}

class ValuedRoute{
    private final Route route;
    private final double value;

    public ValuedRoute(Route route, double value){
        this.route = route;
        this.value = value;
    }

    public Route getRoute() {
        return route;
    }

    public double getValue() {
        return value;
    }
}