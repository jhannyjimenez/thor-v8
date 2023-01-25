package org.mitre.thor.output;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.Input;
import org.mitre.thor.math.Equations;
import org.mitre.thor.math.MatrixUtil;
import org.mitre.thor.math.AppUtil;
import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.grouping.Grouping;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.analyses.Analysis;
import org.mitre.thor.analyses.CriticalityAnalysis;
import org.mitre.thor.network.nodes.Node;
import org.mitre.thor.output.data_holders.CriticalityTrialData;
import org.mitre.thor.output.data_holders.CriticalityData;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;

import static org.mitre.thor.output.data_holders.EquationsEnum.*;

public class NetworkOutput extends Output{

    private boolean groupByStrength = false;

    public NetworkOutput(Input input) {
        super(input);
        for(Analysis analysis : input.iConfig.analyses){
            if(analysis instanceof CriticalityAnalysis){
                if(((CriticalityAnalysis) analysis).groupByStrength){
                    groupByStrength = true;
                    break;
                }
            }
        }
    }

    //get the matrices for each of the cases and alphas
    @Override
    protected void analyzeInput() {
        if(input.iConfig.containsAnalysis(AnalysesForm.CRITICALITY)){
            CriticalityAnalysis analysis = (CriticalityAnalysis) input.iConfig.getAnalysis(AnalysesForm.CRITICALITY);

            //Decide what to calculate
            ArrayList<Node> nodes = analysis.targetType.getTargetNodes(input.network);
            nodes.addAll(input.network.getGroups("crit"));
            nodeStyles = new XSSFCellStyle[nodes.size()];

            for(int a = 0; a < input.iConfig.inputQueues.size(); a++){
                if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                    CriticalityData nodeData = new CriticalityData();
                    nodeData.criticalityTrialData = new CriticalityTrialData[analysis.trials];
                    for(int trial = 0; trial < analysis.trials; trial++){
                        nodeData.criticalityTrialData[trial] = new CriticalityTrialData();

                        if(analysis.grouping == Grouping.PAIRS){
                            nodeData.criticalityTrialData[trial].specialGroup = Equations.pairsExplicit(input.network, nodes, a, trial);
                        }else if(analysis.grouping == Grouping.PAIRS_AND_TRIPLES){
                            nodeData.criticalityTrialData[trial].specialGroup = Equations.comboExplicit(input.network, nodes, a, trial);
                        }else{
                            nodeData.criticalityTrialData[trial].alpha1 = Equations.getMatrix1Answers(input.network, nodes, a, trial);
                            nodeData.criticalityTrialData[trial].alpha2 = Equations.getMatrix2Answers(input.network, nodes, a, trial);
                            nodeData.criticalityTrialData[trial].case1 = Equations.getCase(input.network, nodes, 1, a, trial);
                            nodeData.criticalityTrialData[trial].case2 = Equations.getCase(input.network, nodes, 2, a, trial);
                            nodeData.criticalityTrialData[trial].case3 = Equations.getCase(input.network, nodes, 3, a, trial);
                        }
                    }
                    if(analysis.grouping == Grouping.PAIRS || analysis.grouping == Grouping.PAIRS_AND_TRIPLES){
                        nodeData.calculateAverage(GROUPS);
                        calculateColorScore(nodeData.averageData.specialGroup,  nodes, a);
                    }else{
                        nodeData.calculateAverage(MATRIX1);
                        nodeData.calculateAverage(MATRIX2);
                        nodeData.calculateAverage(CASE1);
                        nodeData.calculateAverage(CASE2);
                        nodeData.calculateAverage(CASE3);
                        calculateColorScore(nodeData.averageData.alpha1,  nodes, a);
                    }
                    nodeCriData.add(nodeData);
                }
            }
        }

        CoreLogger.logger.log(Level.INFO, "Finished Analysing Input");
    }

    //write the data from 'analyzeInput'
    @Override
    protected void writeOutput() {
        if(input.iConfig.containsAnalysis(AnalysesForm.CRITICALITY)){
            CriticalityAnalysis criticalityAnalysis = (CriticalityAnalysis) input.iConfig.getAnalysis(AnalysesForm.CRITICALITY);

            ArrayList<Node> nodes = criticalityAnalysis.targetType.getTargetNodes(input.network);
            nodes.addAll(input.network.getGroups("crit"));

            for(int a = 0; a < super.nodeCriData.size(); a++){
                StringBuilder outputSheetName = new StringBuilder();
                if(criticalityAnalysis.calculationMethod != null && input.iConfig.inputQueues.get(a).rollUpRule != null){
                    outputSheetName.append(criticalityAnalysis.calculationMethod);
                    outputSheetName.append("-");
                    outputSheetName.append(input.iConfig.inputQueues.get(a).rollUpRule);
                }else{
                    outputSheetName.append("PhiT Output");
                }

                if (outputSheetName.length() > 31){
                    outputSheetName.delete(30, outputSheetName.length());
                }

                XSSFSheet tableFormSheet = input.workbook.getSheet(outputSheetName.toString());
                if (tableFormSheet != null) {
                    input.workbook.removeSheetAt(input.workbook.getSheetIndex(tableFormSheet));
                }
                tableFormSheet = input.workbook.createSheet(outputSheetName.toString());

                int n = Math.min(nodes.size(), MAX_NODES);
                Node[] aNodes = new Node[n];
                String[] nodeNames = new String[n];
                double[] nodeIds = new double[n];
                double[] A = new double[n];
                double[] B = new double[n];

                for(int i = 0; i < n && i < MAX_NODES; i++){
                    aNodes[i] = nodes.get(i);
                    nodeNames[i] = nodes.get(i).getDecorativeName();
                    nodeIds[i] = nodes.get(i).decorativeID;
                    A[i] = nodes.get(i).A;
                    B[i] = nodes.get(i).B;
                }

                //write each row with its corresponding data
                writeRowFromVector("Node ID", nodeIds, tableFormSheet, 0);
                writeRowFromStrings("Node Names", nodeNames, tableFormSheet, 1);
                writeRowFromVector("A", A, tableFormSheet, 2);
                writeRowFromVector("B", B, tableFormSheet, 3);

                int nStart = 10;

                if(criticalityAnalysis.grouping == Grouping.PAIRS || criticalityAnalysis.grouping == Grouping.PAIRS_AND_TRIPLES){
                    writeRowFromVector("pairs;alpha", MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.specialGroup), tableFormSheet, 4);
                    nStart = 6;
                }else{
                    writeRowFromVector("case 1;alpha", MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.case1), tableFormSheet, 4);
                    writeRowFromVector("case 2;alpha", MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.case2), tableFormSheet, 5);
                    writeRowFromVector("case 3;alpha", MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.case3), tableFormSheet, 6);
                    writeRowFromVector("matrix;alpha", MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.alpha1), tableFormSheet, 7);
                    writeRowFromVector("matrix 2;alpha", MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.alpha2), tableFormSheet, 8);
                }

                boolean failedToCalculate = false;

                Pair<Node[], double[]> ordered;
                if(criticalityAnalysis.grouping == Grouping.PAIRS || criticalityAnalysis.grouping == Grouping.PAIRS_AND_TRIPLES){
                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.specialGroup));
                    if(groupByStrength){
                        writeRowAndColorByStrength("groups;ordering", ordered.getKey(), tableFormSheet, nStart, a);
                    }else{
                        writeRowAndGroupByColor("groups;ordering", ordered.getKey(), tableFormSheet, nStart, a);
                    }
                    writeRowFromVector("", ordered.getValue(), tableFormSheet, nStart + 2);
                }else{
                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.case1));
                    if(groupByStrength){
                        writeRowAndColorByStrength("case 1;ordering", ordered.getKey(), tableFormSheet, nStart, a);
                    }else{
                        writeRowAndGroupByColor("case 1;ordering", ordered.getKey(), tableFormSheet, nStart, a);
                    }
                    writeRowFromVector("", ordered.getValue(), tableFormSheet, nStart + 2);
                    failedToCalculate = AppUtil.vectorContainsNAN(ordered.getValue());


                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.case2));
                    if(groupByStrength){
                        writeRowAndColorByStrength("case 2;ordering", ordered.getKey(), tableFormSheet, nStart + 3, a);
                    }else{
                        writeRowAndGroupByColor("case 2;ordering", ordered.getKey(), tableFormSheet, nStart + 3, a);
                    }
                    writeRowFromVector("", ordered.getValue(), tableFormSheet, nStart + 5);
                    if(!failedToCalculate){
                        failedToCalculate = AppUtil.vectorContainsNAN(ordered.getValue());
                    }

                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.case3));
                    if(groupByStrength){
                        writeRowAndColorByStrength("case 3;ordering", ordered.getKey(), tableFormSheet, nStart + 6, a);
                    }else{
                        writeRowAndGroupByColor("case 3;ordering", ordered.getKey(), tableFormSheet, nStart + 6, a);
                    }

                    writeRowFromVector("", ordered.getValue(), tableFormSheet, nStart + 8);
                    if(!failedToCalculate){
                        failedToCalculate = AppUtil.vectorContainsNAN(ordered.getValue());
                    }

                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.alpha1));
                    if(groupByStrength){
                        writeRowAndColorByStrength("matrix;ordering", ordered.getKey(), tableFormSheet, nStart + 9, a);
                    }else{
                        writeRowAndGroupByColor("matrix;ordering", ordered.getKey(), tableFormSheet, nStart + 9, a);
                    }
                    writeRowFromVector("", ordered.getValue(), tableFormSheet, nStart + 11);
                    if(!failedToCalculate){
                        failedToCalculate = AppUtil.vectorContainsNAN(ordered.getValue());
                    }

                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.alpha2));
                    if(groupByStrength){
                        writeRowAndColorByStrength("matrix 2;ordering", ordered.getKey(), tableFormSheet, nStart + 12, a);
                    }else{
                        writeRowAndGroupByColor("matrix 2;ordering", ordered.getKey(), tableFormSheet, nStart + 12, a);
                    }
                    writeRowFromVector("", ordered.getValue(), tableFormSheet, nStart + 14);
                    if(!failedToCalculate){
                        failedToCalculate = AppUtil.vectorContainsNAN(ordered.getValue());
                    }
                }

                if(failedToCalculate){
                    input.app.GAE("An error occurred while trying to calculate the criticality of the nodes",false);
                    input.app.GAE("\t This error is most likely due to there not being a solution to the network", false);
                    input.app.GAE("\t / the network can not reach the goal node", true);
                    input.app.GAE("\t / or there is a node which is both a parent and child of another node", true);
                }

                CoreLogger.logger.log(Level.INFO, "Finished creating information for Table Form sheet");

                //write data to the resources sheet
                XSSFSheet resourcesSheet = input.workbook.getSheet("Resources");
                if (resourcesSheet != null) {
                    input.workbook.removeSheetAt(input.workbook.getSheetIndex(resourcesSheet.getSheetName()));
                }
                resourcesSheet = input.workbook.createSheet("Resources");

                XSSFRow headerRow = resourcesSheet.createRow(0);
                XSSFCell cell = headerRow.createCell(0);
                cell.setCellStyle(centerAligned);
                cell.setCellValue("Label");
                cell = headerRow.createCell(1);
                cell.setCellStyle(centerAligned);
                cell.setCellValue("Id");
                cell = headerRow.createCell(2);
                cell.setCellStyle(centerAligned);
                cell.setCellValue("Color Score");
                cell = headerRow.createCell(3);
                cell.setCellStyle(centerAligned);
                cell.setCellValue("Alphas");

                int c = 0;
                int finalA = a;
                Pair<Node[], double[]> alpha1Ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.alpha1));

                for(int i = 0; i < alpha1Ordered.getKey().length && i < MAX_NODES; i++){
                    XSSFRow row = resourcesSheet.createRow(i + 1);
                    cell = row.createCell(0);
                    XSSFCellStyle outputStyle = super.nodeStyles[i];
                    if(outputStyle != null){
                        cell.setCellStyle(outputStyle);
                    }else{
                        cell.setCellStyle(centerAligned);
                    }
                    cell.setCellValue(alpha1Ordered.getKey()[i].getDecorativeName());
                    cell = row.createCell(1);
                    cell.setCellValue(alpha1Ordered.getKey()[i].decorativeID);
                    cell = row.createCell(2);
                    cell.setCellStyle(centerAligned);
                    cell.setCellValue(alpha1Ordered.getKey()[i].analysisDataHolders.get(a).colorScore);
                    cell = row.createCell(3);
                    cell.setCellStyle(roundedCenterAligned);
                    cell.setCellValue(alpha1Ordered.getKey()[i].analysisDataHolders.get(a).alpha);
                    c++;
                }

                double[] colorScores = new double[nodes.size()];
                for(int i = 0; i < nodes.size() && i < MAX_NODES; i++){
                    colorScores[i] = nodes.get(i).analysisDataHolders.get(a).colorScore;
                }
                Arrays.sort(colorScores);

                double mean;
                double sum = 0;
                double median;
                double standardDeviation;
                double meanDifferencesSquaredSum = 0;
                double meanDifferencesSquaredMean;

                for (double colorScore : colorScores) {
                    sum += colorScore;
                }
                mean = sum / colorScores.length;

                if (colorScores.length % 2 == 0){
                    median = (colorScores[colorScores.length/2] + colorScores[colorScores.length/2 - 1])/2;
                }else{
                    median = colorScores[colorScores.length/2];
                }

                for (double colorScore : colorScores) {
                    meanDifferencesSquaredSum += Math.pow(colorScore - mean, 2);
                }
                meanDifferencesSquaredMean = meanDifferencesSquaredSum / colorScores.length;
                standardDeviation = Math.sqrt(meanDifferencesSquaredMean);

                XSSFRow meanRow = resourcesSheet.createRow(c + 2);
                XSSFRow medianRow = resourcesSheet.createRow(c + 3);
                XSSFRow standardDeviationRow = resourcesSheet.createRow(c + 4);

                XSSFCell meanLabelCell = meanRow.createCell(0);
                meanLabelCell.setCellStyle(boldCenterAligned);
                meanLabelCell.setCellValue("Mean");
                XSSFCell meanCell = meanRow.createCell(2);
                meanCell.setCellType(CellType.NUMERIC);
                meanCell.setCellStyle(roundedCenterAligned);
                meanCell.setCellValue(mean);

                XSSFCell medianLabelCell = medianRow.createCell(0);
                medianLabelCell.setCellStyle(boldCenterAligned);
                medianLabelCell.setCellValue("Median");
                XSSFCell medianCell = medianRow.createCell(2);
                medianCell.setCellType(CellType.NUMERIC);
                medianCell.setCellStyle(roundedCenterAligned);
                medianCell.setCellValue(median);

                XSSFCell sdLabelCell = standardDeviationRow.createCell(0);
                sdLabelCell.setCellStyle(boldCenterAligned);
                sdLabelCell.setCellValue("SD");
                XSSFCell sdCell = standardDeviationRow.createCell(2);
                sdCell.setCellType(CellType.NUMERIC);
                sdCell.setCellStyle(roundedCenterAligned);
                sdCell.setCellValue(standardDeviation);

                if(!input.network.networkOrder.isEmpty()){
                    XSSFSheet networkOrderSheet = input.workbook.getSheet("Network Order");
                    if (networkOrderSheet != null) {
                        input.workbook.removeSheetAt(input.workbook.getSheetIndex(networkOrderSheet.getSheetName()));
                    }
                    networkOrderSheet = input.workbook.createSheet("Network Order");

                    XSSFRow row0 = networkOrderSheet.createRow(0);
                    XSSFCell cellA0 = row0.createCell(0);
                    cellA0.setCellValue("Node Name");
                    XSSFCell cellA1 = row0.createCell(1);
                    cellA1.setCellValue("Node ID");

                    int r = 0;
                    for(int i = 0; i < input.network.networkOrder.size() && i < MAX_NODES; i++){
                        if(input.network.networkOrder.get(i).isReal){
                            XSSFRow rowI = networkOrderSheet.createRow(r + 1);
                            XSSFCell nodeName = rowI.createCell(0);
                            nodeName.setCellValue(input.network.networkOrder.get(i).getDecorativeName());
                            XSSFCell nodeId = rowI.createCell(1);
                            nodeId.setCellValue(input.network.networkOrder.get(i).decorativeID);
                            r++;
                        }
                    }
                }else{
                    CoreLogger.logger.log(Level.WARNING, "Could not create a network order");
                }

                CoreLogger.logger.log(Level.INFO, "Finished creating information for Resources sheet");

                tableFormSheet.autoSizeColumn(0);
                resourcesSheet.autoSizeColumn(0);
                resourcesSheet.autoSizeColumn(2);
            }
        }
        if(input.iConfig.containsAnalysis(AnalysesForm.DYNAMIC_OPERABILITY)){
            super.outputStandardDynamicOperability();
        }
        if(input.iConfig.containsAnalysis(AnalysesForm.CG)){
            super.outputStandardCoolGraph();
        }
        if(input.iConfig.containsAnalysis(AnalysesForm.ATTACK)){
            super.outputAttack();
        }

        outputMathematica();
        //TODO: Make toggle for this
        //outputPhiTable();
        //outputAllNodesSheet();
        //outputAllLinksSheet();
        outputDagger();
    }

    private void writeRowAndGroupByColor(String header, Node[] nodes, XSSFSheet sheet, int rowNumber, int rollUpIndex) {
        XSSFRow row = sheet.createRow(rowNumber);
        XSSFRow row2 = sheet.createRow(rowNumber + 1);
        XSSFCell headerCell = row.createCell(0);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(boldCenterAligned);
        int counter = 0;

        for (int i = 0; i < nodes.length && i < MAX_NODES; i++) {
            if(nodes[i] != null){
                XSSFCell cell = row.createCell(i + 1);
                cell.setCellValue(nodes[i].getDecorativeName());
                XSSFCell cell2 = row2.createCell(i + 1);
                cell2.setCellValue(nodes[i].decorativeID);

                if (i != 0) {
                    Activity prevActivity = null;
                    for(Activity activity : this.input.network.getActivities()){
                        if(activity.id == nodes[i-1].id){
                            prevActivity = activity;
                        }
                    }

                    if (prevActivity != null && nodes[i].analysisDataHolders.get(rollUpIndex).colorScore != prevActivity.analysisDataHolders.get(rollUpIndex).colorScore) {
                        if (counter != this.clusterStyles.size() - 1) {
                            counter++;
                        } else {
                            counter = 0;
                        }
                    }
                }
                cell.setCellStyle(clusterStyles.get(counter));
                cell2.setCellStyle(clusterStyles.get(counter));
            }
        }
    }

    private void writeRowAndColorByStrength(String header, Node[] nodes, XSSFSheet sheet, int rowNumber, int rollUpIndex){
        XSSFRow row = sheet.createRow(rowNumber);
        XSSFRow row2 = sheet.createRow(rowNumber + 1);
        XSSFCell headerCell = row.createCell(0);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(boldCenterAligned);
        XSSFCell header2Cell = row2.createCell(0);
        header2Cell.setCellValue("ID");
        header2Cell.setCellStyle(boldCenterAligned);

        for (int i = 0; i < nodes.length && i < MAX_NODES; i++) {
            if(nodes[i] != null){
                XSSFCell cell = row.createCell(i + 1);
                cell.setCellValue(nodes[i].getDecorativeName());
                XSSFCell cell2 = row2.createCell(i + 1);
                cell2.setCellValue(nodes[i].decorativeID);

                int index;
                if(nodes[i].analysisDataHolders.get(rollUpIndex).colorScore <= 10){
                    index = 1;
                }else if(nodes[i].analysisDataHolders.get(rollUpIndex).colorScore <= 30){
                    index = 2;
                }else if(nodes[i].analysisDataHolders.get(rollUpIndex).colorScore <= 50){
                    index = 3;
                }else if(nodes[i].analysisDataHolders.get(rollUpIndex).colorScore <= 70){
                    index = 4;
                }else if (nodes[i].analysisDataHolders.get(rollUpIndex).colorScore <= 90){
                    index = 5;
                }else{
                    index = 6;
                }
                nodeStyles[i] = strengthStyles.get(index);

                cell.setCellStyle(strengthStyles.get(index));
                cell2.setCellStyle(strengthStyles.get(index));
                if(index == 1 || index == 6){
                    cell.getCellStyle().setFont(whiteFont);
                    cell2.getCellStyle().setFont(whiteFont);
                }else{
                    cell.getCellStyle().setFont(defaultFont);
                    cell2.getCellStyle().setFont(defaultFont);
                }
            }
        }
    }

}
