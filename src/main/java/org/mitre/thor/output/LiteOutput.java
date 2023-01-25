package org.mitre.thor.output;

import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.Input;
import org.mitre.thor.math.Equations;
import org.mitre.thor.math.MatrixUtil;
import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.grouping.Grouping;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.analyses.CriticalityAnalysis;
import org.mitre.thor.network.nodes.Node;
import org.mitre.thor.output.data_holders.CriticalityData;
import org.mitre.thor.output.data_holders.CriticalityTrialData;

import java.util.ArrayList;
import java.util.logging.Level;

import static org.mitre.thor.output.data_holders.EquationsEnum.MATRIX1;
import static org.mitre.thor.output.data_holders.EquationsEnum.GROUPS;

public class LiteOutput extends Output{

    private final boolean second;

    public LiteOutput(Input input, boolean second){
        super(input);
        this.second = second;
    }

    //Calculate the alpha 1 matrix
    @Override protected void analyzeInput() {
        if(!second){
            if(input.iConfig.containsAnalysis(AnalysesForm.CRITICALITY)){
                CriticalityAnalysis analysis = (CriticalityAnalysis) input.iConfig.getAnalysis(AnalysesForm.CRITICALITY);

                ArrayList<Node> nodes = analysis.targetType.getTargetNodes(input.network);
                nodes.addAll(input.network.getGroups("crit"));
                nodeStyles = new XSSFCellStyle[nodes.size()];

                for(int a = 0; a < input.iConfig.inputQueues.size(); a++){
                    if(input.iConfig.inputQueues.get(a).containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                        CriticalityData data = new CriticalityData();
                        data.criticalityTrialData = new CriticalityTrialData[analysis.trials];
                        for(int trial = 0; trial < analysis.trials; trial++){
                            data.criticalityTrialData[trial] = new CriticalityTrialData();
                            if(analysis.grouping == Grouping.PAIRS){
                                data.criticalityTrialData[trial].specialGroup = Equations.pairsExplicit(input.network, nodes, a, trial);
                            }else if(analysis.grouping == Grouping.PAIRS_AND_TRIPLES){
                                data.criticalityTrialData[trial].specialGroup = Equations.comboExplicit(input.network, nodes, a, trial);
                            }else{
                                data.criticalityTrialData[trial].alpha1 = Equations.getMatrix1Answers(input.network,nodes, a, trial);
                            }
                        }
                        if(analysis.grouping == Grouping.PAIRS || analysis.grouping == Grouping.PAIRS_AND_TRIPLES){
                            data.calculateAverage(GROUPS);
                            calculateColorScore(data.averageData.specialGroup,  nodes, a);
                        }else{
                            data.calculateAverage(MATRIX1);
                            calculateColorScore(data.averageData.alpha1,  nodes, a);
                        }
                        nodeCriData.add(data);
                    }
                }
            }

            CoreLogger.logger.log(Level.INFO, "Finished Analysing Input");
        }
    }

    @Override protected void writeOutput() {
        //handle the output for the criticality analysis
        if(input.iConfig.containsAnalysis(AnalysesForm.CRITICALITY)){
            CriticalityAnalysis criticalityAnalysis = (CriticalityAnalysis) input.iConfig.getAnalysis(AnalysesForm.CRITICALITY);

            ArrayList<Node> nodes = criticalityAnalysis.targetType.getTargetNodes(input.network);

            for(int a = 0; a < super.nodeCriData.size(); a++){
                //if sheet exist delete and create a new one, else just create a new one
                StringBuilder outputSheetName = new StringBuilder();
                if(criticalityAnalysis.calculationMethod != null && input.iConfig.inputQueues.get(a).rollUpRule != null){
                    outputSheetName.append("L-");
                    outputSheetName.append(criticalityAnalysis.calculationMethod);
                    outputSheetName.append("-");
                    outputSheetName.append(input.iConfig.inputQueues.get(a).rollUpRule);
                }else{
                    outputSheetName.append("PhiT Output");
                }

                if (outputSheetName.length() > 31){
                    outputSheetName.delete(30, outputSheetName.length());
                }

                XSSFSheet liteSheet = input.workbook.getSheet(outputSheetName.toString());
                if (liteSheet != null) {
                    input.workbook.removeSheetAt(input.workbook.getSheetIndex(liteSheet));
                }
                liteSheet = input.workbook.createSheet(outputSheetName.toString());

                //get nodes
                int n = nodes.size();
                Node[] aNodes = new Node[n];
                for(int i = 0; i < n && i < MAX_NODES; i++){
                    aNodes[i] = nodes.get(i);
                }

                //order the node names based on the alpha 1 matrix values for "a" roll up rule
                Pair<Node[], double[]> ordered;
                if(criticalityAnalysis.grouping == Grouping.PAIRS || criticalityAnalysis.grouping == Grouping.PAIRS_AND_TRIPLES){
                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.specialGroup));
                }else{
                    ordered = orderNodesListBasedOnVector(aNodes, MatrixUtil.createVectorFromMatrixData(super.nodeCriData.get(a).averageData.alpha1));
                }


                //get nodeIds
                double[] nodeIds = new double[n];
                Node[] newNodes = new Node[n];
                for(int i = 0; i < ordered.getKey().length  && i < MAX_NODES; i++){
                    nodeIds[i] = ordered.getKey()[i].decorativeID;
                    newNodes[i] = ordered.getKey()[i];
                }

                writeColumnFromVector("Node ID", nodeIds, liteSheet, 0, centerAligned);
                writeColoredColumn(newNodes, liteSheet, a);
                writeColumnFromVector("Alpha Values", ordered.getValue(), liteSheet, 2, roundedCenterAligned);

                //gather the color scores into a double array
                double[] colorScores = new double[n];
                for(int i = 0; i < ordered.getKey().length && i < MAX_NODES; i++){
                    for(Activity activity : input.network.getActivities()){
                        if(activity.id == ordered.getKey()[i].id){
                            colorScores[i] = activity.analysisDataHolders.get(a).colorScore;
                        }
                    }
                }

                writeColumnFromVector("Color Score", colorScores, liteSheet, 3, integerCenterAligned);

                CoreLogger.logger.log(Level.INFO, "Finished creating information for" + outputSheetName + " sheet");

                liteSheet.setColumnWidth(0, 4500);
                liteSheet.setColumnWidth(1, 4500);
                liteSheet.setColumnWidth(2, 4500);
            }
        }
        if(!second && input.iConfig.containsAnalysis(AnalysesForm.DYNAMIC_OPERABILITY)){
            outputStandardDynamicOperability();
        }
        if(!second && input.iConfig.containsAnalysis(AnalysesForm.CG)){
            super.outputStandardCoolGraph();
        }

        if(!second){
            outputMathematica();
        }
    }

    //write an excel column from a vector composed of a double array
    private void writeColumnFromVector(String header, double[] vector, XSSFSheet sheet, int columnIndex, XSSFCellStyle style){
        XSSFRow headerRow = sheet.getRow(0);
        if(headerRow == null){
            headerRow = sheet.createRow(0);
        }
        XSSFCell headerCell = headerRow.createCell(columnIndex);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(boldCenterAligned);
        for(int i = 0; i < vector.length; i++){
            XSSFRow row = sheet.getRow(i + 1);
            if(row == null){
                row = sheet.createRow(i + 1);
            }
            XSSFCell cell = row.createCell(columnIndex);
            cell.setCellValue(vector[i]);
            cell.setCellType(CellType.NUMERIC);
            cell.setCellStyle(style);
        }
    }

    private void writeColoredColumn(Node[] nodes, XSSFSheet sheet, int rollUpIndex) {
        XSSFRow headerRow = sheet.getRow(0);
        if(headerRow == null){
            headerRow = sheet.createRow(0);
        }
        XSSFCell headerCell = headerRow.createCell(1);
        headerCell.setCellValue("Node Names");
        headerCell.setCellStyle(boldCenterAligned);

        for (int i = 0; i < nodes.length; i++) {
            if(nodes[i] != null){
                XSSFRow row = sheet.getRow(i + 1);
                if(row == null){
                    row = sheet.createRow(i + 1);
                }
                XSSFCell cell = row.createCell(1);
                cell.setCellValue(nodes[i].getDecorativeName());

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
                if(index == 1 || index == 6){
                    cell.getCellStyle().setFont(whiteFont);
                }else{
                    cell.getCellStyle().setFont(defaultFont);
                }
            }
        }
    }
}
