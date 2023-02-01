package org.mitre.thor.output;

import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.ejml.simple.SimpleMatrix;
import org.mitre.chart.CoolGraph;
import org.mitre.chart.LinePlotter;
import org.mitre.chart.ScatterPlotter;
import org.mitre.thor.analyses.AttackAnalysis;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.Input;
import org.mitre.thor.network.*;
import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.CGAnalysis;
import org.mitre.thor.analyses.CriticalityAnalysis;
import org.mitre.thor.analyses.cg.CGOrderingMethod;
import org.mitre.thor.analyses.grouping.Grouping;
import org.mitre.thor.network.attack.AttackTreeBuilder;
import org.mitre.thor.network.attack.AttackPoint;
import org.mitre.thor.network.attack.DecisionOption;
import org.mitre.thor.network.links.Link;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Group;
import org.mitre.thor.network.nodes.Node;
import org.mitre.thor.output.data_holders.CriticalityData;
import org.mitre.thor.analyses.target.TargetType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;

public abstract class Output {

    public FileOutputStream fos;
    public Input input;
    protected final static int MAX_NODES = 100;

    //holds data for criticality calculations
    public ArrayList<CriticalityData> nodeCriData = new ArrayList<>();

    //Cell styles for designing Excel outputs
    protected  XSSFFont whiteFont;
    protected  XSSFFont defaultFont;
    protected XSSFCellStyle centerAligned;
    protected XSSFCellStyle roundedCenterAligned;
    protected XSSFCellStyle boldCenterAligned;
    protected  XSSFCellStyle integerCenterAligned;
    protected  XSSFCellStyle heading1Style;
    protected  XSSFCellStyle heading3Style;
    protected  XSSFCellStyle outputStyle;
    protected  XSSFCellStyle noteStyle;

    //Indexed colors
    protected ArrayList<XSSFCellStyle> clusterStyles;
    protected ArrayList<XSSFCellStyle> strengthStyles;
    protected XSSFCellStyle[] nodeStyles;

    private final DecimalFormat idDF = new DecimalFormat("0.##");
    private final DecimalFormat sizeDF = new DecimalFormat("##.##");
    private final static int MAX_CHARS = 32000;
    private final static int MAX_CG_DAGGER_NODES = 10;
    private final static int MAX_ATTACK_POINTS = 100000;

    protected Output(Input input){
        this.input = input;
        if(!input.hasRead){
            input.read();
        }
        initializeCellStyles();
        initializeIndexColors();
    }

    /*
    process and write the output. This is the only function that should be called outside of the Output class or any
    class that inherits it
     */
    public boolean write(boolean processInput){
        boolean success = true;
        if(processInput){
            success =  input.process();
        }
        if(!success){
            input.app.GAE("Could not process the input", true);
            return false;
        }

        analyzeInput();
        writeOutput();
        try {
            fos = new FileOutputStream(input.iConfig.filePath);
            CoreLogger.logger.log(Level.INFO, "Successfully created the File Output Stream");
        }catch (Exception e){
            e.printStackTrace();
            input.app.GAE("The file could not be written because it does not exist or it is being used by another process", true);
        }

        try {
            input.workbook.write(fos);
            input.app.GAS("Successfully wrote output file at " + input.iConfig.filePath, true);
            return true;
        }catch (Exception e){
            //TODO add more
            e.printStackTrace();
            input.app.GAE("Failed to write the output file at " + input.iConfig.filePath, true);
        }

        return false;
    }

    /** Function Available to Classes that Inherent Output **/

    //do some calculation with data provided by the input
    protected abstract void analyzeInput();

    //output the calculations from the analysis
    protected abstract void writeOutput();

    //handles the output for Dynamic Operability Analysis
    protected void outputStandardDynamicOperability(){
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++){
            if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.DYNAMIC_OPERABILITY)){
                writeDynamicOperabilityDependencies(a);
                if(input.app.controller != null){
                    graphTimes(a);
                }
            }
        }
    }

    //handles the output for Cool Graph Analysis
    protected void outputStandardCoolGraph(){
        CGAnalysis cgAnalysis = (CGAnalysis) input.iConfig.getAnalysis(AnalysesForm.CG);
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++){
            if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CG))
                graphWriteCoolGraph(input.cGDivisionNumbers, input.cGDivisionColors, a,input.iConfig.inputQueues.get(a).rollUpRule.name(), cgAnalysis.orderingMethod);
        }
    }

    protected void outputAttack(){
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++){
            if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.ATTACK)){
                //graphAttackChain(a);
                outputAttackDecisionTree(a, input.iConfig.inputQueues.get(a).rollUpRule.name());
            }
        }
    }

    protected void outputPhiTable(){
        CriticalityAnalysis cAnalysis = (CriticalityAnalysis) input.iConfig.getAnalysis(AnalysesForm.CRITICALITY);
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++) {
            if (input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CRITICALITY)) {
                writePhiTable(input.network, cAnalysis.targetType, a, input.iConfig.inputQueues.get(a).rollUpRule.name());
            }
        }
    }

    protected void outputDagger(){
        CGAnalysis cgAnalysis = (CGAnalysis) input.iConfig.getAnalysis(AnalysesForm.CG);
        CriticalityAnalysis critAnalysis = (CriticalityAnalysis) input.iConfig.getAnalysis(AnalysesForm.CRITICALITY);
        for(int a = 0; a < input.iConfig.inputQueues.size(); a++) {
            if (input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CG)){
                writeDaggerCGRunMap(a, cgAnalysis.targetType, input.iConfig.inputQueues.get(a).rollUpRule.name());
            }
            if (input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                writeDaggerCrit(a, critAnalysis.targetType, input.iConfig.inputQueues.get(a).rollUpRule.name());
            }
        }
    }

    protected void outputAllLinksSheet(){
        XSSFSheet allLinksSheet = input.workbook.getSheet("All-Links");
        if (allLinksSheet != null) {
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(allLinksSheet.getSheetName()));
        }
        allLinksSheet = input.workbook.createSheet("All-Links");

        XSSFRow headerRow = allLinksSheet.createRow(0);
        XSSFCell A1 = headerRow.createCell(0);
        XSSFCell B1 = headerRow.createCell(1);
        XSSFCell C1 = headerRow.createCell(2);
        XSSFCell D1 = headerRow.createCell(3);
        XSSFCell E1 = headerRow.createCell(4);
        A1.setCellValue("Dependency ID");
        B1.setCellValue("Child Name");
        C1.setCellValue("Child ID");
        D1.setCellValue("Parent Name");
        E1.setCellValue("Parent ID");

        int i = 1;
        for(Link link : input.network.getLinks()){
            XSSFRow row = allLinksSheet.createRow(i);
            XSSFCell A = row.createCell(0);
            XSSFCell B = row.createCell(1);
            XSSFCell C = row.createCell(2);
            XSSFCell D = row.createCell(3);
            XSSFCell E = row.createCell(4);
            A.setCellValue(i);
            B.setCellValue(link.child.name);
            C.setCellValue(link.child.decorativeID);
            D.setCellValue(link.parent.name);
            E.setCellValue(link.parent.decorativeID);

            i++;
        }
    }

    protected  void outputAllNodesSheet(){
        XSSFSheet allNodesSheet = input.workbook.getSheet("All-Nodes");
        if (allNodesSheet != null) {
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(allNodesSheet.getSheetName()));
        }
        allNodesSheet = input.workbook.createSheet("All-Nodes");

        XSSFRow headerRow = allNodesSheet.createRow(0);
        XSSFCell A1 = headerRow.createCell(0);
        XSSFCell B1 = headerRow.createCell(1);
        XSSFCell C1 = headerRow.createCell(2);
        A1.setCellValue("ID");
        B1.setCellValue("Name");
        C1.setCellValue("Rule");

        int i = 1;
        for(Node node : input.network.getNodes()){
            XSSFRow row = allNodesSheet.createRow(i);
            XSSFCell A = row.createCell(0);
            XSSFCell B = row.createCell(1);
            XSSFCell C = row.createCell(2);
            A.setCellValue(node.decorativeID);
            B.setCellValue(node.name);
            C.setCellValue(node.analysisDataHolders.get(0).queue.rollUpRule.name());

            i++;
        }
    }

    private void writeDaggerCGRunMap(int rollUpIndex, TargetType targetType, String rollUpName){

        XSSFSheet daggerRSheet = input.workbook.getSheet("DAGGER-CG-" + rollUpName);
        if (daggerRSheet != null) {
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(daggerRSheet.getSheetName()));
        }
        daggerRSheet = input.workbook.createSheet("DAGGER-CG-" + rollUpName);

        ArrayList<Node> targetNodes = targetType.getTargetNodes(input.network);
        targetNodes.sort(Comparator.comparingDouble(o -> o.analysisDataHolders.get(rollUpIndex).cgSCore));
        Collections.reverse(targetNodes);
        XSSFRow headerRow = daggerRSheet.createRow(0);

        XSSFCell instructionHeaderCell = headerRow.createCell(0);
        instructionHeaderCell.setCellStyle(heading1Style);
        instructionHeaderCell.setCellValue("Instructions");
        XSSFCell instructionCell= headerRow.createCell(1);
        instructionCell.setCellStyle(noteStyle);
        instructionCell.setCellValue("To put a DAGGER input together first copy and paste the base text onto an xml file, then copy and paste any of the value texts right after the first paste.");

        XSSFCell baseCell = headerRow.createCell(2);
        baseCell.setCellStyle(heading1Style);
        baseCell.setCellValue("Base");
        String base = input.network.getPt1DaggerCode(rollUpIndex);
        ArrayList<String> breaks = breakStringIntoListWithMaxChars(base, MAX_CHARS);
        int t = 3;
        for (String aBreak : breaks) {
            XSSFCell baseValueCell = headerRow.createCell(t);
            baseValueCell.setCellStyle(outputStyle);
            baseValueCell.setCellValue(aBreak);
            t++;
        }
        XSSFCell nodeNameCell = headerRow.createCell(t);
        nodeNameCell.setCellStyle(heading1Style);
        nodeNameCell.setCellValue("ROW = Node, COL = Operability, VALUE = Bubble Up Effect");
        for(int i = 0; i < targetNodes.size() && i < MAX_CG_DAGGER_NODES; i++){
            Node node = targetNodes.get(i);
            XSSFRow row = daggerRSheet.createRow(i + 1);
            XSSFCell nameCell = row.createCell(t);
            nameCell.setCellStyle(heading3Style);
            nameCell.setCellValue(node.name);
            int c = t + 1;
            for(int a = 0; a < 101; a++){
                XSSFCell headerCell = headerRow.createCell(c);
                headerCell.setCellStyle(heading3Style);
                headerCell.setCellValue(a);
                String value = input.network.getCGDaggerCode(rollUpIndex, node.id, a);
                ArrayList<String> values = breakStringIntoListWithMaxChars(value, MAX_CHARS);
                for(int j = 0; j < values.size(); j++){
                    XSSFCell cell = row.createCell(c + j);
                    cell.setCellStyle(outputStyle);
                    cell.setCellValue(values.get(j));
                }
                c += values.size();
            }
            row.setHeight((short) (100 * 15));
        }
        headerRow.setHeight((short) (100 * 15));
        daggerRSheet.autoSizeColumn(0);
        daggerRSheet.setColumnWidth(1, 600 * 15);
        daggerRSheet.autoSizeColumn(t);
    }

    private void writeDaggerCrit(int rollUpIndex, TargetType targetType, String rollUpName){
        XSSFSheet daggerSheet = input.workbook.getSheet("DAGGER-CRIT-" + rollUpName);
        if (daggerSheet != null) {
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(daggerSheet.getSheetName()));
        }
        daggerSheet = input.workbook.createSheet("DAGGER-CRIT-" + rollUpName);

        ArrayList<Node> targetNodes = targetType.getTargetNodes(input.network);
        targetNodes.sort(Comparator.comparingDouble(o -> o.analysisDataHolders.get(rollUpIndex).colorScore));
        Collections.reverse(targetNodes);
        ArrayList<Node> critNodes = targetNodes.size() > 10 ? new ArrayList<>(targetNodes.subList(0, 10)) : targetNodes;

        ArrayList<String> bases = breakStringIntoListWithMaxChars(input.network.getPt1DaggerCode(rollUpIndex), MAX_CHARS);
        ArrayList<String> values = breakStringIntoListWithMaxChars(input.network.geCritGDaggerCode(rollUpIndex, critNodes), MAX_CHARS);

        XSSFRow instructionRow = daggerSheet.createRow(0);
        XSSFCell instructionHeaderCell = instructionRow.createCell(0);
        instructionHeaderCell.setCellStyle(heading1Style);
        instructionHeaderCell.setCellValue("Instructions");
        XSSFCell instructionCell= instructionRow.createCell(1);
        instructionCell.setCellStyle(noteStyle);
        instructionCell.setCellValue("To put a DAGGER input together first copy and paste the base text onto an xml file, then copy and paste all of the criticality texts right after the first paste.");

        XSSFRow baseRow = daggerSheet.createRow(1);
        XSSFCell baseCell = baseRow.createCell(0);
        baseCell.setCellStyle(heading1Style);
        baseCell.setCellValue("Base");
        for(int i = 0; i < bases.size(); i++){
            XSSFCell bCell = baseRow.createCell(i + 1);
            bCell.setCellStyle(outputStyle);
            bCell.setCellValue(bases.get(i));
        }
        baseRow.setHeight((short) (100 * 15));

        XSSFRow critRow = daggerSheet.createRow(2);
        XSSFCell labelCell = critRow.createCell(0);
        labelCell.setCellStyle(heading1Style);
        labelCell.setCellValue("Criticality");
        for(int i = 0; i < values.size(); i++){
            XSSFCell valueCell = critRow.createCell(i + 1);
            valueCell.setCellStyle(outputStyle);
            valueCell.setCellValue(values.get(i));
        }
        critRow.setHeight((short) (15 * 100));

        daggerSheet.autoSizeColumn(0);
        daggerSheet.setColumnWidth(1, 600 * 15);
    }

    private ArrayList<String> breakStringIntoListWithMaxChars(String string, int maxChars){
        ArrayList<String> values = new ArrayList<>();
        if(string.length() > maxChars){
            double div = Math.ceil(string.length() / (double) maxChars);
            int start = 0;
            for(int i = 0; i < div; i++){
                int end = Math.min(maxChars + start, string.length());
                String iSub = string.substring(start, end);
                values.add(iSub);
                start += maxChars;
            }
        }else{
            values.add(string);
        }
        return values;
    }

    private void writePhiTable(Network network, TargetType targetType, int rollUpI, String rollUpName){
        XSSFSheet phiTableSheet = input.workbook.getSheet("Phi-" + rollUpName);
        if (phiTableSheet != null) {
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(phiTableSheet.getSheetName()));
        }
        phiTableSheet = input.workbook.createSheet("Phi-" + rollUpName);

        ArrayList<Node> nodes = targetType.getTargetNodes(input.network);
        nodes.addAll(input.network.getGroups("crit"));

        XSSFRow headerRow = phiTableSheet.createRow(0);
        for(int i = 0; i < nodes.size() && i < MAX_NODES; i++){
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(nodes.get(i).getDecorativeName());
        }

        for(int i = 0; i < network.analysisDataHolders.get(rollUpI).possibilitiesTable.size() && i < Input.MAX_TABLE_ROWS; i++){
            XSSFRow row = phiTableSheet.createRow(i + 1);
            for(int a = 0; a < network.analysisDataHolders.get(rollUpI).possibilitiesTable.get(i).size() && a < MAX_NODES; a++){
                XSSFCell cell = row.createCell(a);
                cell.setCellValue( network.analysisDataHolders.get(rollUpI).possibilitiesTable.get(i).get(a));
            }
        }

        int c = Math.min(nodes.size(), MAX_NODES);
        XSSFCell endCell =  headerRow.createCell(c);
        endCell.setCellValue("END");

        for(int i = 0; i < network.analysisDataHolders.get(rollUpI).endResults.size() && i < Input.MAX_TABLE_ROWS; i++){
            XSSFRow row = phiTableSheet.getRow(i + 1);
            XSSFCell cell = row.createCell(c);
            cell.setCellValue(network.analysisDataHolders.get(rollUpI).endResults.get(i));
        }
    }

    //handles the output for Mathematica sheet
    protected void outputMathematica(){

        XSSFSheet mathematicaSheet = input.workbook.getSheet("Mathematica");
        int startRow = -1;
        if (mathematicaSheet != null) {
            startRow = mathematicaSheet.getLastRowNum();
        }else{
            mathematicaSheet = input.workbook.createSheet("Mathematica");
        }

        int rowCounter = 0;

        if(startRow == -1){
            XSSFRow rC = mathematicaSheet.createRow(rowCounter);
            XSSFCell cC = rC.createCell(0);
            cC.setCellValue("Glossary");
            cC.setCellStyle(heading1Style);
            rowCounter++;

            XSSFRow r1 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r1c1 = r1.createCell(0);
            r1c1.setCellValue("Final");
            r1c1.setCellStyle(heading3Style);
            XSSFCell r1c2 = r1.createCell(1);
            r1c2.setCellValue("If there are loops, this is the modified version of the network where the loops have been fixed. If there aren't any loops, than this is just the regular network without any modifications");
            r1c2.setCellStyle(noteStyle);
            rowCounter++;

            XSSFRow r2 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r2c1 = r2.createCell(0);
            r2c1.setCellValue("Original");
            r2c1.setCellStyle(heading3Style);
            XSSFCell r2c2 = r2.createCell(1);
            r2c2.setCellValue("If there are loops, this is the raw network which contains and highlights the loops. If there aren't any loops this tag will not appear.");
            r2c2.setCellStyle(noteStyle);
            rowCounter++;

            XSSFRow r3 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r3c1 = r3.createCell(0);
            r3c1.setCellValue("F-");
            r3c1.setCellStyle(heading3Style);
            XSSFCell r3c2 = r3.createCell(1);
            r3c2.setCellValue("Uses the final network");
            r3c2.setCellStyle(noteStyle);
            rowCounter++;

            XSSFRow r4 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r4c1 = r4.createCell(0);
            r4c1.setCellValue("O-");
            r4c1.setCellStyle(heading3Style);
            XSSFCell r4c2 = r4.createCell(1);
            r4c2.setCellValue("Uses the original network");
            r4c2.setCellStyle(noteStyle);
            rowCounter++;

            XSSFRow r5 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r5c1 = r5.createCell(0);
            r5c1.setCellValue("CRIT-");
            r5c1.setCellStyle(heading3Style);
            XSSFCell r5c2 = r5.createCell(1);
            r5c2.setCellValue("Highlights important nodes in red based on the alpha output of the Criticality analysis");
            r5c2.setCellStyle(noteStyle);
            rowCounter++;

            XSSFRow r6 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r6c1 = r6.createCell(0);
            r6c1.setCellValue("CG-");
            r6c1.setCellStyle(heading3Style);
            XSSFCell r6c2 = r6.createCell(1);
            r6c2.setCellValue("Highlights important nodes in red based on the CG Score output of the CG analysis");
            r6c2.setCellStyle(noteStyle);
            rowCounter++;

            XSSFRow r7 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r7c1 = r7.createCell(0);
            r7c1.setCellValue("NODES");
            r7c1.setCellStyle(heading3Style);
            XSSFCell r7c2 = r7.createCell(1);
            r7c2.setCellValue("Nodes were targeted in the chosen analysis");
            r7c2.setCellStyle(noteStyle);
            rowCounter++;

            XSSFRow r8 = mathematicaSheet.createRow(rowCounter);
            XSSFCell r8c1 = r8.createCell(0);
            r8c1.setCellValue("FACTORS");
            r8c1.setCellStyle(heading3Style);
            XSSFCell r8c2 = r8.createCell(1);
            r8c2.setCellValue("Factors were targeted in the chosen analysis");
            r8c2.setCellStyle(noteStyle);
            rowCounter++;
            rowCounter++;
        }else{
            rowCounter = startRow + 1;
        }

        boolean usedID = false;
        int rowCounterResetValue = rowCounter;
        int max = input.iConfig.includeIDMathematica ? 2 : 1;
        for(int i = 0; i < max; i++){
            String head = "Using Names";
            int col = 0;
            if(usedID){
                col = 2;
                head = "Using IDs";
            }

            if(startRow == -1){
                XSSFRow rh= mathematicaSheet.getRow(rowCounter);
                if(rh == null){
                    rh= mathematicaSheet.createRow(rowCounter);
                }
                XSSFCell ch = rh.createCell(col);
                ch.setCellValue(head);
                ch.setCellStyle(heading1Style);
                rowCounter++;

                String finalHeader = "Final";
                writeMathematicaStrip(mathematicaSheet, rowCounter, col, finalHeader, input.network, usedID, null, null,0);
                rowCounter++;

                if(input.looped){
                    for(Activity activity : input.rawNetwork.getActivities()){
                        for(Activity activity2 : input.network.getActivities()){
                            if(activity.id == activity2.id){
                                activity.analysisDataHolders = activity2.analysisDataHolders;
                                break;
                            }
                        }
                    }

                    String originalHeader = "Original";
                    writeMathematicaStrip(mathematicaSheet, rowCounter, col, originalHeader, input.rawNetwork, usedID, null, null,0);
                    rowCounter++;
                }
            }

            for(int a = 0; a < input.iConfig.inputQueues.size(); a++) {
                AnalysesForm form = null;
                TargetType targetType = null;
                String target = "";
                String analysis = "";
                String rule = input.iConfig.inputQueues.get(a).rollUpRule.name();
                String postInfo = "";
                if (input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                    CriticalityAnalysis CA = (CriticalityAnalysis) input.iConfig.getAnalysis(AnalysesForm.CRITICALITY);
                    form = AnalysesForm.CRITICALITY;
                    analysis = "CRIT";
                    targetType = CA.targetType;
                    target = targetType.name();
                }else if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CG)){
                    CGAnalysis CG = (CGAnalysis) input.iConfig.getAnalysis(AnalysesForm.CG);
                    form = AnalysesForm.CG;
                    analysis = "CG";
                    targetType = CG.targetType;
                    target = targetType.name();

                    if(CG.grouping == Grouping.SINGLE){
                        postInfo = ",S";
                    }else if(CG.grouping == Grouping.PAIRS){
                        postInfo = ",P";
                    }else if(CG.grouping == Grouping.PAIRS_AND_TRIPLES){
                        postInfo = ",P&T";
                    }
                }
                if (form != null){
                    String header = target + "-" + analysis + "-" + rule;
                    String fHeader = header + ",F" + postInfo;
                    writeMathematicaStrip(mathematicaSheet, rowCounter, col, fHeader, input.network, usedID, form, targetType, a);
                    rowCounter++;
                    if(input.looped){
                        String oHeader = header + ",O" + postInfo;
                        writeMathematicaStrip(mathematicaSheet, rowCounter, col, oHeader, input.rawNetwork, usedID, form, targetType, a);
                        rowCounter++;
                    }
                }
            }
            rowCounter = rowCounterResetValue;
            usedID = true;
        }

        mathematicaSheet.setColumnWidth(0, 28*256);
        mathematicaSheet.setColumnWidth(1, 56*256);
        mathematicaSheet.setColumnWidth(2, 28*256);
        mathematicaSheet.setColumnWidth(3, 56*256);
    }

    private void writeMathematicaStrip(XSSFSheet mathS, int rowI, int columnI, String header, Network network, boolean useID, AnalysesForm form, TargetType targetType, int rollUpI){
        String mathematicaString = getMathematicaString(network, useID, form, targetType, rollUpI);
        XSSFRow row = mathS.getRow(rowI);
        if(row == null){
            row = mathS.createRow(rowI);
        }
        row.setHeight((short) (20 * 25));
        XSSFCell headerCell = row.createCell(columnI);
        XSSFCell value = row.createCell(columnI + 1);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(heading3Style);
        value.setCellValue(mathematicaString);
        value.setCellStyle(outputStyle);
        resetRedColoredNodes();
    }

    private void resetRedColoredNodes(){
        for(Node node : input.network.getNodes()){
            if (node.mathematicaColor != null && (node.mathematicaColor.equals("Red") || node.mathematicaColor.equals("Orange"))){
                node.mathematicaColor = null;
                if(node.mathematicaSize != null && (node.mathematicaSize.equals("largeSize") || node.mathematicaSize.equals("medSize"))){
                    node.mathematicaSize = null;
                }
            }
        }
    }

    /*
    calculate the color score for the Criticality Analysis
    color score is a number from 0 - 100 that represents how important a node is in relation to the other nodes in
    the same network
     */
    protected void calculateColorScore(SimpleMatrix matrix, ArrayList<Node> nodes, int rollUpIndex){
        double max = -Integer.MAX_VALUE;
        double min = Integer.MAX_VALUE;
        for(int i = 0; i < nodes.size(); i++){
            //calculate max and min
            double m = roundAlpha(matrix.get(i, 0));
            if(m >= max){
                max = m;
            }
            if(m <= min){
                min = m;
            }
            nodes.get(i).analysisDataHolders.get(rollUpIndex).alpha = matrix.get(i, 0);
        }
        //normalize the values from "matrix" using the max and min
        for(int i = 0; i < nodes.size(); i++){
            if((max - min) == 0){
                nodes.get(i).analysisDataHolders.get(rollUpIndex).colorScore = 100;
            }else{
                nodes.get(i).analysisDataHolders.get(rollUpIndex).colorScore = (int) (100.0 * (roundAlpha(matrix.get(i, 0)) - min) / (max - min));
            }
        }
    }

    private final DecimalFormat alphaDF = new DecimalFormat("###.##");

    private double roundAlpha(double alpha){
        return Double.parseDouble(alphaDF.format(alpha));
    }

    //write the bytes to the file and close the workbook so that other programs can access
    public void close() {
        try {
            fos.close();
            CoreLogger.logger.log(Level.INFO, "Successfully closed the File Output Stream");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Used to Write Data to Sheet **/

    //return an excel row from a vector composed of a double array
    protected void writeRowFromVector(String header, double[] vector, XSSFSheet sheet, int rowNumber){
        XSSFRow row = sheet.createRow(rowNumber);
        XSSFCell headerCell = row.createCell(0);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(boldCenterAligned);
        for(int i = 0; i < vector.length && i < MAX_NODES; i++){
            XSSFCell cell = row.createCell(i + 1);
            if(!Double.isNaN(vector[i])){
                cell.setCellValue(vector[i]);
            }else{
                cell.setCellValue("NAN");
            }
            cell.setCellType(CellType.NUMERIC);
            cell.setCellStyle(roundedCenterAligned);
        }
    }

    //return an excel row from a vector composed of a double array
    protected void writeRowFromStrings(String header, String[] strings, XSSFSheet sheet, int rowNumber){
        XSSFRow row = sheet.createRow(rowNumber);
        XSSFCell headerCell = row.createCell(0);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(boldCenterAligned);
        for(int i = 0; i < strings.length; i++){
            XSSFCell cell = row.createCell(i + 1);
            cell.setCellValue(strings[i]);
            cell.setCellType(CellType.STRING);
            cell.setCellStyle(roundedCenterAligned);
        }
    }

    //order a vector which also relates to a list of strings
    protected Pair<Node[], double[]> orderNodesListBasedOnVector(Node[] nodes, double[] vector){
        //TODO make this error proof
        for (double v : vector) {
            if (Double.isNaN(v)) {
                new Pair<>(nodes, vector);
            }
        }

        Node[] orderedNodes = new Node[nodes.length];
        double[] ascendingVector = new double[nodes.length];
        System.arraycopy(vector, 0, ascendingVector, 0, nodes.length);
        Arrays.sort(ascendingVector);
        double[] descendingVector = new double[vector.length];
        for(int i = 0; i < ascendingVector.length; i++){
            descendingVector[ascendingVector.length - 1 - i] = ascendingVector[i];
        }

        ArrayList<Integer> used = new ArrayList<>();
        for(int a = 0; a < nodes.length; a++){
            for(int i = 0; i < nodes.length; i++){
                if(descendingVector[a] == vector[i] && !used.contains(i)){
                    orderedNodes[a] = nodes[i];
                    used.add(i);
                    break;
                }
            }
        }

        if(orderedNodes.length > MAX_NODES && descendingVector.length > MAX_NODES){
            orderedNodes = Arrays.copyOfRange(orderedNodes, 0, MAX_NODES);
            descendingVector = Arrays.copyOfRange(descendingVector, 0, MAX_NODES);
        }

        return new Pair<>(orderedNodes, descendingVector);
    }

    /** Private Fields **/

    private void initializeCellStyles(){
        //Create fonts and cell styles used to write the output
        XSSFFont boldFont = input.workbook.createFont();
        boldFont.setBold(true);

        XSSFFont calibri15Blue = input.workbook.createFont();
        calibri15Blue.setFontHeight(15);
        calibri15Blue.setBold(true);
        calibri15Blue.setColor(IndexedColors.BLUE_GREY.index);

        XSSFFont calibri11Blue = input.workbook.createFont();
        calibri11Blue.setFontHeight(13);
        calibri11Blue.setBold(true);
        calibri11Blue.setColor(IndexedColors.BLUE_GREY.index);

        centerAligned = input.workbook.createCellStyle();
        centerAligned.setAlignment(HorizontalAlignment.CENTER);

        roundedCenterAligned = input.workbook.createCellStyle();
        roundedCenterAligned.setAlignment(HorizontalAlignment.CENTER);
        roundedCenterAligned.setDataFormat((input.workbook.createDataFormat().getFormat("0.0#")));

        boldCenterAligned = input.workbook.createCellStyle();
        boldCenterAligned.setFont(boldFont);
        boldCenterAligned.setAlignment(HorizontalAlignment.CENTER);

        heading1Style = input.workbook.createCellStyle();
        heading1Style.setFont(calibri15Blue);
        heading1Style.setBorderBottom(BorderStyle.THICK);
        heading1Style.setBottomBorderColor(IndexedColors.LIGHT_BLUE.index);
        heading1Style.setAlignment(HorizontalAlignment.CENTER);
        heading1Style.setVerticalAlignment(VerticalAlignment.CENTER);

        heading3Style = input.workbook.createCellStyle();
        heading3Style.setFont(calibri11Blue);
        heading3Style.setBorderBottom(BorderStyle.DASHED);
        //heading3Style.setBottomBorderColor(IndexedColors.LIGHT_BLUE.index);
        heading3Style.setAlignment(HorizontalAlignment.CENTER);
        heading3Style.setVerticalAlignment(VerticalAlignment.CENTER);

        outputStyle = input.workbook.createCellStyle();
        outputStyle.setFont(boldFont);
        outputStyle.setBorderTop(BorderStyle.THIN);
        outputStyle.setBorderRight(BorderStyle.THIN);
        outputStyle.setBorderBottom(BorderStyle.THIN);
        outputStyle.setBorderLeft(BorderStyle.THIN);
        outputStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        outputStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        outputStyle.setAlignment(HorizontalAlignment.FILL);

        noteStyle = input.workbook.createCellStyle();
        noteStyle.setFont(boldFont);
        noteStyle.setBorderTop(BorderStyle.THIN);
        noteStyle.setTopBorderColor(IndexedColors.GREY_40_PERCENT.index);
        noteStyle.setBorderRight(BorderStyle.THIN);
        noteStyle.setRightBorderColor(IndexedColors.GREY_40_PERCENT.index);
        noteStyle.setBorderBottom(BorderStyle.THIN);
        noteStyle.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.index);
        noteStyle.setBorderLeft(BorderStyle.THIN);
        noteStyle.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.index);
        noteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        noteStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.index);
        noteStyle.setWrapText(true);
    }

    private void initializeIndexColors(){
        //Create list of possibles colors to be used when grouping nodes
        IndexedColors[] clusterColors = new IndexedColors[]{
                IndexedColors.GOLD, IndexedColors.ROSE, IndexedColors.SKY_BLUE, IndexedColors.CORAL, IndexedColors.GREY_25_PERCENT,
                IndexedColors.LIGHT_ORANGE, IndexedColors.LIGHT_GREEN, IndexedColors.VIOLET
        };

        IndexedColors[] strengthColors = new IndexedColors[]{
                IndexedColors.WHITE, IndexedColors.GREEN, IndexedColors.LIGHT_GREEN,
                IndexedColors.YELLOW, IndexedColors.ORANGE, IndexedColors.RED, IndexedColors.VIOLET
        };

        clusterStyles = new ArrayList<>();
        for(IndexedColors color : clusterColors){
            XSSFCellStyle style = input.workbook.createCellStyle();
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setFillForegroundColor(color.getIndex());
            clusterStyles.add(style);
        }

        strengthStyles = new ArrayList<>();
        for(IndexedColors color : strengthColors){
            XSSFCellStyle style = input.workbook.createCellStyle();
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setFillForegroundColor(color.getIndex());
            strengthStyles.add(style);
        }

        whiteFont = input.workbook.createFont();
        whiteFont.setColor(IndexedColors.WHITE.index);

        defaultFont = input.workbook.createFont();
        defaultFont.setColor(XSSFFont.DEFAULT_FONT_COLOR);
    }

    private void writeDynamicOperabilityDependencies(int rollUpIndex){
        int numOfRows = input.dependenciesSheet.getLastRowNum();
        XSSFRow headerRow = input.dependenciesSheet.getRow(0);
        int headerRowSize = headerRow.getLastCellNum();
        boolean usingNodeName = true;
        for(int i = 0; i < headerRowSize; i++){
            XSSFCell headerCell = headerRow.getCell(i);
            String lHeaderCell = headerCell.getStringCellValue().toLowerCase();
            if(lHeaderCell.contains("id")){
                usingNodeName = false;
            }
            if(lHeaderCell.contains("name")){
                usingNodeName = true;
            }
        }
        for(int i = 0; i < numOfRows; i++){
            XSSFRow row = input.dependenciesSheet.getRow(i + 1);

            XSSFCell nodeCell = row.getCell(0);
            Activity currentActivity = null;
            for(Activity activity : input.network.getActivities()){
                if((usingNodeName && activity.name.equals(nodeCell.toString())) || (!usingNodeName && activity.id == nodeCell.getNumericCellValue())){
                    currentActivity = activity;
                    break;
                }
            }
            if(currentActivity == null){
                input.app.GAE("Could not find a node named " + nodeCell.toString() + "; Please remove this cell at " + input.integerToAlphabetic(0) + (i + 2) + " 'Dependencies Sheet'", true);
            }else{
                for(int a = 0; a < input.numTimes; a++){
                    if(!Double.isNaN(currentActivity.analysisDataHolders.get(rollUpIndex).operabilityPerTime[a])){
                        XSSFCell cell = row.createCell(input.timeColumns.get(a));
                        cell.setCellValue(currentActivity.analysisDataHolders.get(rollUpIndex).operabilityPerTime[a]);
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellStyle(roundedCenterAligned);
                    }
                }
            }
        }
    }

    private void graphTimes(int rollUpIndex){
        //create line chart plotter
        LinePlotter linePlotter = new LinePlotter();
        linePlotter.setLineChartTitle("Operability per Time");
        linePlotter.setXAxisLabel("Time");
        linePlotter.setYAxisLabel("Operability");
        linePlotter.setYAxisBounds(0, 100);
        linePlotter.setXStep(1);
        linePlotter.setYStep(10);

        Platform.runLater(() -> linePlotter.start(new Stage()));
        Platform.runLater((() -> linePlotter.setTitle("Dynamic Operability-"+input.network.analysisDataHolders.get(rollUpIndex).queue.rollUpRule.name())));

        linePlotter.setXAxisBounds(1, input.numTimes);
        for(Activity activity : input.network.getRealLeafActivities()){
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(activity.getDecorativeName());

            for(int i = 0; i < activity.analysisDataHolders.get(rollUpIndex).operabilityPerTime.length; i++){
                series.getData().add(new XYChart.Data<>(i + 1, activity.analysisDataHolders.get(rollUpIndex).operabilityPerTime[i]));
            }
            Platform.runLater(() -> linePlotter.addSeries(series));
        }

        XYChart.Series<Number, Number> goalSeries = new XYChart.Series<>();
        goalSeries.setName(input.network.goalActivity.getDecorativeName() + "(MISSION)");

        for(int i = 0; i < input.network.goalActivity.analysisDataHolders.get(rollUpIndex).operabilityPerTime.length; i++){
            goalSeries.getData().add(new XYChart.Data<>(i + 1, input.network.goalActivity.analysisDataHolders.get(rollUpIndex).operabilityPerTime[i]));
        }
        Platform.runLater(() -> linePlotter.addSeries(goalSeries));
        Platform.runLater(() -> linePlotter.plotterStage.show());
    }

    protected void outputAttackDecisionTree(int rollUpIndex, String rollUpName){

        AttackTreeBuilder attackTreeBuilder = input.network.analysisDataHolders.get(rollUpIndex).attackTreeBuilder;

        XSSFSheet decisionTreeSheet = input.workbook.getSheet("DT-" + rollUpName);
        if(decisionTreeSheet != null){
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(decisionTreeSheet.getSheetName()));
        }
        decisionTreeSheet = input.workbook.createSheet("DT-" + rollUpName);

        XSSFRow headerRow = decisionTreeSheet.createRow(0);
        XSSFCell A1 = headerRow.createCell(0);
        A1.setCellStyle(heading1Style);
        XSSFCell B1 = headerRow.createCell(1);
        B1.setCellStyle(heading1Style);
        XSSFCell C1 = headerRow.createCell(2);
        C1.setCellStyle(heading1Style);
        A1.setCellValue("Simple Mathematica Tree");
        B1.setCellValue("Tooltip Mathematica Tree");
        C1.setCellValue("Text Tree");

        XSSFRow valueRow = decisionTreeSheet.createRow(1);
        XSSFCell A2 = valueRow.createCell(0);
        A2.setCellStyle(outputStyle);
        XSSFCell B2 = valueRow.createCell(1);
        B2.setCellStyle(outputStyle);
        XSSFCell C2 = valueRow.createCell(2);
        C2.setCellStyle(outputStyle);
        A2.setCellValue(attackTreeBuilder.getMathematicaCode(false));
        B2.setCellValue(attackTreeBuilder.getMathematicaCode(true));
        C2.setCellValue(attackTreeBuilder.getTextTreeString());
        valueRow.setHeight((short) (100 * 15));

        decisionTreeSheet.setColumnWidth(0, 600 * 15);
        decisionTreeSheet.setColumnWidth(1, 600 * 15);
        decisionTreeSheet.setColumnWidth(2, 600 * 15);
    }

    /*
    protected void graphAttackChain(int rollUpIndex){
        ScatterPlotter attacKGraph = new ScatterPlotter();
        attacKGraph.setLineChartTitle("Impact per Cost");
        attacKGraph.setXAxisLabel("Cost");
        attacKGraph.setYAxisLabel("Impact");

        Platform.runLater(() -> attacKGraph.start(new Stage()));
        Platform.runLater((() -> attacKGraph.setTitle("Attack-"+input.network.analysisDataHolders.get(rollUpIndex).queue.rollUpRule.name())));

        XYChart.Series<Number, Number> rawSerires = new XYChart.Series<>();
        rawSerires.setName("Raw Data");

        XYChart.Series<Number, Number> bestSeries = new XYChart.Series<>();
        bestSeries.setName("Best Data");

        ArrayList<AttackPoint> bestPoints = new ArrayList<>();
        ArrayList<Double> xValues = new ArrayList<>();
        for(AttackPoint point : input.network.analysisDataHolders.get(rollUpIndex).attackPoints){
            double cost = point.cost;
            if(!xValues.contains(cost)){
                xValues.add(cost);
            }
        }

        double maxCost = 0;
        for(Double x : xValues){
            if(x > maxCost){
                maxCost = x;
            }

            AttackPoint bestPoint = null;
            for(int i = 0; i < input.network.analysisDataHolders.get(rollUpIndex).attackPoints.size() && i < MAX_ATTACK_POINTS; i++){
                AttackPoint point = input.network.analysisDataHolders.get(rollUpIndex).attackPoints.get(i);
                if(point.cost == x && (bestPoint == null || point.impact > bestPoint.impact)){
                    bestPoint = point;
                }
            }
            bestPoints.add(bestPoint);
        }

        for(int i = 0; i < input.network.analysisDataHolders.get(rollUpIndex).attackPoints.size() && i < MAX_ATTACK_POINTS; i++){
            AttackPoint point = input.network.analysisDataHolders.get(rollUpIndex).attackPoints.get(i);
            if(bestPoints.contains(point)){
                bestSeries.getData().add(new XYChart.Data<>(point.cost, point.impact, point.path));
            }else{
                rawSerires.getData().add(new XYChart.Data<>(point.cost, point.impact, point.path));
            }
        }

        attacKGraph.setXStep(maxCost / 10.0);
        attacKGraph.setYStep(10);
        attacKGraph.setXAxisBounds(0, maxCost + 10);
        attacKGraph.setYAxisBounds(0, 120);

        Platform.runLater(() -> attacKGraph.addSeries(rawSerires));
        Platform.runLater(() -> attacKGraph.addSeries(bestSeries));

        //loop through data and add tooltip
        //THIS MUST BE DONE AFTER ADDING THE DATA TO THE CHART!
        for (XYChart.Data<Number, Number> entry : rawSerires.getData()) {
            Tooltip t = new Tooltip(entry.getExtraValue().toString());
            t.setShowDelay(Duration.seconds(0.1));
            Tooltip.install(entry.getNode(), t);
        }
        for (XYChart.Data<Number, Number> entry : bestSeries.getData()) {
            Tooltip t = new Tooltip(entry.getExtraValue().toString());
            t.setShowDelay(Duration.seconds(0.1));
            Tooltip.install(entry.getNode(), t);
        }

        Platform.runLater(attacKGraph::show);
    }
     */

    private void graphWriteCoolGraph(List<Integer> divisions, List<int[]> divisionColors, int rollUpI, String rollUpName, CGOrderingMethod method){
        CGAnalysis CGA = (CGAnalysis)input.iConfig.getAnalysis(AnalysesForm.CG);

        //create sheet
        XSSFSheet cgOutSheet = input.workbook.getSheet("CG-" + rollUpName + "-" + method.name());
        if(cgOutSheet != null){
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(cgOutSheet.getSheetName()));
        }
        cgOutSheet = input.workbook.createSheet("CG-" + rollUpName + "-" + method.name());

        int rows;
        ArrayList<Node> nodes;
        ArrayList<Group> cgGroups = input.network.getGroups("cg");
        ArrayList<Group> cgGroupsCustom = input.network.getGroups("cg-custom");
        ArrayList<Node> tNodes = CGA.targetType.getTargetNodes(input.network);
        if(cgGroups.size() > 0){
            nodes = new ArrayList<>(cgGroups);
        }else if(cgGroupsCustom.size() > 0){
            nodes = new ArrayList<>(cgGroupsCustom);
        }else {
            nodes = new ArrayList<>(tNodes);
        }

        nodes.sort((o1, o2) -> Double.compare(o2.analysisDataHolders.get(rollUpI).cgSCore, o1.analysisDataHolders.get(rollUpI).cgSCore));
        rows = nodes.size();

        XSSFRow headerRow = cgOutSheet.createRow(0);
        XSSFCell nodeIDCell = headerRow.createCell(0);
        nodeIDCell.setCellStyle(centerAligned);
        nodeIDCell.setCellValue("Node ID");
        XSSFCell nodeNameCell = headerRow.createCell(1);
        nodeNameCell.setCellStyle(centerAligned);
        nodeNameCell.setCellValue("Node Name");
        XSSFCell nodeValueCell = headerRow.createCell(2);
        nodeValueCell.setCellStyle(centerAligned);
        nodeValueCell.setCellValue("CG Score");
        XSSFCell logValueCell = headerRow.createCell(3);
        logValueCell.setCellStyle(centerAligned);
        logValueCell.setCellValue("Log of Score");
        XSSFCell normalizedValueCell = headerRow.createCell(4);
        normalizedValueCell.setCellStyle(centerAligned);
        normalizedValueCell.setCellValue("Norm. Value");
        XSSFCell aDerivative = headerRow.createCell(5);
        aDerivative.setCellStyle(centerAligned);
        aDerivative.setCellValue("A. Derivative");
        XSSFCell eDerivative = headerRow.createCell(6);
        eDerivative.setCellStyle(centerAligned);
        eDerivative.setCellValue("E. Derivative");

        for(int i = 0; i < rows; i++){
           if(nodes.get(i).isReal){
               //crate row and cells
               XSSFRow iRow = cgOutSheet.createRow(i + 1);
               XSSFCell idCell = iRow.createCell(0);
               XSSFCell nameCell = iRow.createCell(1);
               XSSFCell valueCell = iRow.createCell(2);
               XSSFCell logCell = iRow.createCell(3);
               XSSFCell norCell = iRow.createCell(4);
               XSSFCell aDerCell = iRow.createCell(5);
               XSSFCell eDerCell = iRow.createCell(6);

               //Find the max division and set appropriate name color
               int minDiv = nodes.get(i).analysisDataHolders.get(rollUpI).sizePerDivision.size() - 1;

               for(int a = nodes.get(i).analysisDataHolders.get(rollUpI).sizePerDivision.size() - 1; a > -1; a--){
                   if(nodes.get(i).analysisDataHolders.get(rollUpI).sizePerDivision.get(a) > 0 && a < minDiv){
                       minDiv = a;
                   }
               }

               XSSFCellStyle style = cgOutSheet.getWorkbook().createCellStyle();
               byte[] rgb = {(byte) divisionColors.get(minDiv)[0], (byte) divisionColors.get(minDiv)[1], (byte) divisionColors.get(minDiv)[2]};
               style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
               style.setFillForegroundColor(new XSSFColor(rgb, new DefaultIndexedColorMap()));
               style.setAlignment(HorizontalAlignment.CENTER);
               nameCell.setCellStyle(style);
               idCell.setCellStyle(centerAligned);

               //set cell values
               if(nodes.get(i).decorativeID != -1){
                   idCell.setCellValue(nodes.get(i).decorativeID);
               }
               nameCell.setCellValue(nodes.get(i).getDecorativeName());

               double cgScore = nodes.get(i).analysisDataHolders.get(rollUpI).cgSCore;
               valueCell.setCellValue(cgScore);
               valueCell.setCellStyle(roundedCenterAligned);

               logCell.setCellValue(Math.log(cgScore) + 10);
               logCell.setCellStyle(roundedCenterAligned);

               norCell.setCellValue(nodes.get(i).analysisDataHolders.get(rollUpI).normalCgScore);
               norCell.setCellStyle(roundedCenterAligned);

               javafx.util.Pair<Double, Double> derivatives;
               derivatives = CGA.orderingMethod.getDerivatives(nodes.get(i), rollUpI);
               double aD = derivatives.getKey();
               double eD = derivatives.getValue();
               aDerCell.setCellValue(aD);
               eDerCell.setCellValue(eD);
               aDerCell.setCellStyle(roundedCenterAligned);
               eDerCell.setCellStyle(roundedCenterAligned);
           }
        }

        cgOutSheet.autoSizeColumn(0);
        cgOutSheet.autoSizeColumn(1);
        cgOutSheet.autoSizeColumn(2);
        cgOutSheet.autoSizeColumn(3);
        cgOutSheet.autoSizeColumn(4);
        cgOutSheet.autoSizeColumn(5);
        cgOutSheet.autoSizeColumn(6);

        //create OP sheet
        XSSFSheet cgOpSheet = input.workbook.getSheet("CG-" + rollUpName + "-OP");
        if(cgOpSheet != null){
            input.workbook.removeSheetAt(input.workbook.getSheetIndex(cgOpSheet.getSheetName()));
        }
        cgOpSheet = input.workbook.createSheet("CG-" + rollUpName + "-OP");

        XSSFRow hRow = cgOpSheet.createRow(0);
        XSSFCell hCell = hRow.createCell(0);
        hCell.setCellValue("Operability");

        for(int i = 0; i < 101; i++){
            XSSFCell iCell = hRow.createCell(i + 1);
            iCell.setCellValue(i);
        }

        for(int i = 0; i < nodes.size(); i++){
            XSSFRow iRow = cgOpSheet.createRow(i + 1);
            XSSFCell nCell = iRow.createCell(0);
            nCell.setCellValue(nodes.get(i).getDecorativeName());

            for(int a = 0; a < nodes.get(i).analysisDataHolders.get(rollUpI).coolGraphMissionOperability.length; a++){
                XSSFCell aCell = iRow.createCell(a + 1);
                aCell.setCellValue(nodes.get(i).analysisDataHolders.get(rollUpI).coolGraphMissionOperability[a]);
                aCell.setCellStyle(roundedCenterAligned);
            }
        }

        cgOpSheet.autoSizeColumn(0);

        if(input.app.controller != null){
            CoolGraph cg = new CoolGraph(divisions, divisionColors);
            Platform.runLater(() -> cg.setTitle("CG-" + (input.iConfig.inputQueues.get(rollUpI).rollUpRule.name())));
            cg.graphNodes(nodes, rollUpI);
            cg.show();

            if(CGA.saveGraph){
                cg.captureAndSaveDisplay();
            }
        }
    }

    //MUST BE CALLED AFTER CALCULATING THE NODE'S COLOR SCORE
    private String getMathematicaString(Network network, boolean useID, AnalysesForm analysis, TargetType targetType, int rollUpI){

        double root = Math.sqrt(network.getNodes().size());
        double multiplier = useID ? 1.5 : 1.0;
        double defaultSize = multiplier * ((Math.log10(root) / 10) * root);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("data = {");

        ArrayList<Node> usedNodes = new ArrayList<>();
        ArrayList<Link> usedLinks = new ArrayList<>();

        for(Link link : network.getLinks()){
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

        stringBuilder.append("scale = ").append(sizeDF.format(defaultSize)).append(";\n")
                .append("medSize = scale * 1.3;\n")
                .append("largeSize = scale * 1.6;\n")
                .append("g = LayeredGraphPlot[data,\n ");

        ArrayList<Node> lRedNodes = new ArrayList<>();
        ArrayList<Node> mRedNodes = new ArrayList<>();

        if(analysis == AnalysesForm.CRITICALITY || analysis == AnalysesForm.CG && targetType != null){
            //large nodes' max 10% min of 5%
            //medium nodes min 5%
            int rN = targetType.getTargetNodes(network).size();
            int threePercent = (int) (rN * 0.03);
            int tenPercent = (int) (rN * 0.1);
            lRedNodes = new ArrayList<>(network.getTopNodes(0, threePercent, analysis , rollUpI, targetType));
            mRedNodes = new ArrayList<>();
            /*
            if(lRedNodes.size() < tenPercent){
                mRedNodes = new ArrayList<>(network.getTopNodes(lRedNodes.size(), threePercent, analysis, rollUpI, targetType));
            }
             */
            mRedNodes = new ArrayList<>(network.getTopNodes(lRedNodes.size(), threePercent, analysis, rollUpI, targetType));
        }

        for(Node lRed : lRedNodes){
            lRed.mathematicaColor = "Red";
            lRed.mathematicaSize = "largeSize";
        }

        for(Node mRed : mRedNodes){
            mRed.mathematicaColor = "Orange";
            mRed.mathematicaSize = "medSize";
        }

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
}
