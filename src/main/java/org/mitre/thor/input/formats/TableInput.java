package org.mitre.thor.input.formats;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mitre.input_correcter.TableInputCorrecter;
import org.mitre.thor.App;
import org.mitre.thor.input.Input;
import org.mitre.thor.input.InputConfiguration;
import org.mitre.thor.network.links.Link;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Node;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TableInput extends Input {

    public XSSFSheet phiSheet;

    private int phiColumnIndex = -1;

    public TableInput(InputConfiguration iConfig, App app){
        super(iConfig, app);
    }

    @Override
    public boolean read() {

        try {
            super.fis = new FileInputStream(super.iConfig.filePath);
            super.workbook = new XSSFWorkbook(fis);
            app.GAS("Successfully Loaded the Excel XSSF Workbook at " + super.iConfig.filePath, false);
        }catch (IOException exception){
            app.GAE("Failed to load the Excel XSSF Workbook at " + super.iConfig.filePath, true);
            return false;
        }

        TableInputCorrecter correcter = new TableInputCorrecter(this);

        List<String> phiErrors = correcter.findPhiErrors();
        if(phiErrors.isEmpty()){
            findNetwork();
            getPhis();
            app.GAS("'PhiSheet' does not contain any errors", false);
        }else{
            app.GAE("Errors were found in the 'Phi' sheet", true);
            for(String error : phiErrors){
                app.GAE(error, true);
            }
            return false;
        }

        correcter.findDependenciesErrors();
        if(correcter.errors.isEmpty()){
            super.readCriticalityDependencies( super.network);
            app.GAS("'Dependencies Sheet' does not contain any errors", false);
        }else{
            app.GAE("Errors were found in the 'Dependencies' sheet", true);
            correcter.printErrors(false);
            return false;
        }

        hasRead = true;
        return true;
    }

    private void findNetwork(){
        if(phiSheet == null){
            phiSheet = workbook.getSheet("Phi");
        }
        XSSFRow tableHeaderRow = phiSheet.getRow(0);
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        if(tableHeaderRow != null){
            for(int i = 0; i < tableHeaderRow.getLastCellNum(); i++){
                XSSFCell nameCell = tableHeaderRow.getCell(i);
                if(nameCell != null && !nameCell.toString().equalsIgnoreCase("phi")){
                    String activityName = removeTrailingZeros(nameCell.toString());
                    Activity activity = new Activity(activityName);
                    activity.isReal = true;
                    nodes.add(activity);
                }else if(nameCell != null){
                    phiColumnIndex = i;
                }
            }
            super.network = new Network(nodes, links, iConfig.inputQueues);
            if(!super.network.findStartAndEnd()){
                app.GAE("Failed to establish start and end node. Please make sure that there is only one root and end node", true);
            }
        }
    }

    private void getPhis(){
        for(int i = 1; i < phiSheet.getLastRowNum(); i++){
            XSSFRow row = phiSheet.getRow(i);
            if(row != null){
                XSSFCell phiCell = row.getCell(phiColumnIndex);
                for(int a = 0; a < row.getLastCellNum(); a++){
                    if(a != phiColumnIndex){
                        XSSFCell cellStatus = row.getCell(a);
                        if(cellStatus != null){
                            if(cellStatus.getNumericCellValue() == 1){
                                super.network.getActivities().get(a).analysisDataHolders.get(0).nodeCriticalityTrials[0].sumOfGoalWhenON += phiCell.getNumericCellValue();
                            }else if(cellStatus.getNumericCellValue() == 0){
                                super.network.getActivities().get(a).analysisDataHolders.get(0).nodeCriticalityTrials[0].sumOfGoalWhenOFF += phiCell.getNumericCellValue();
                            }
                        }
                    }
                }
                network.analysisDataHolders.get(0).networkCriticalityTrials[0].phiSum += Double.parseDouble(phiCell.toString());
                network.analysisDataHolders.get(0).networkCriticalityTrials[0].possibilitiesCount = network.analysisDataHolders.get(0).networkCriticalityTrials[0].possibilitiesCount.add(BigInteger.ONE);
            }
        }
    }
}
