package org.mitre.thor.input.formats;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.App;
import org.mitre.thor.input.Input;
import org.mitre.thor.input.InputConfiguration;
import org.mitre.thor.network.links.ActivityLink;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.links.Link;
import org.mitre.thor.network.loops.JDeLoop;
import org.mitre.thor.network.nodes.Activity;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class IncidenceInput extends Input {

    public IncidenceInput(InputConfiguration iConfig, App app){
        super(iConfig, app);
    }

    @Override
    public boolean read() {

        try {
            super.fis = new FileInputStream(super.iConfig.filePath);
            super.workbook = new XSSFWorkbook(fis);
            String msg = "Successfully Loaded the Excel XSSF Workbook at " + super.iConfig.filePath;
            app.GAS(msg, false);
        }catch (IOException exception){
            String msg = "Failed to load the Excel XSSF Workbook at " + super.iConfig.filePath;
            app.GAE(msg, true);
            return false;
        }

        super.usesFDNA = false;


        if(iConfig.checkNSP){
            List<String> rules = findRollUpRulesUsedInDependencies();
            if(rules == null){
                CoreLogger.logger.log(Level.SEVERE, "Could not find the roll up rules used");
                return false;
            }else{
                for(String rule: rules){
                    if(rule.equalsIgnoreCase("fdna") || rule.equalsIgnoreCase("fdna2")){
                        usesFDNA = true;
                        break;
                    }
                }
            }
        }else{
            if(iConfig.checkFDNA){
                usesFDNA = true;
            }
        }

        readLinks(getActivities());
        if(!super.network.findStartAndEnd()){
            app.GAE("Failed to establish start and end node. Please make sure that there is only one root and end node", true);
            return false;
        }

        try{
            rawNetwork = (Network) network.clone();
            if(!rawNetwork.findStartAndEnd()){
                app.GAE("Failed to establish start and end node. Please make sure that there is only one root and end node", true);
                return false;
            }

            boolean loopsExist = true;
            while(loopsExist){
                List<Activity> loops =  this.network.getLoops(true, rawNetwork);
                if(!loops.isEmpty()){
                    app.GAS("Working on fixing loops ... Please wait", true);
                    super.looped = true;
                    this.network = JDeLoop.createDeLoopedGraph(this.network);
                }else{
                    app.GAS("Loops fixed!", true);
                    loopsExist = false;
                }
            }

        }catch (CloneNotSupportedException e){
            e.printStackTrace();
        }


        if(usesFDNA){
            readAlpha();
            readBeta();
        }

        if(readAnalysisSpecificDependencies()){
            return false;
        }

        super.network.addNetworkAnalysisDataHolders(true);
        hasRead = true;
        return true;
    }

    private ArrayList<Activity> getActivities(){
        XSSFSheet labelsSheet = workbook.getSheet("Labels");
        ArrayList<Activity> activities = new ArrayList<>();
        for(int i = 0; i < labelsSheet.getLastRowNum(); i++){
            XSSFRow rowI = labelsSheet.getRow(i + 1);
            if(rowI != null){
                XSSFCell cell0 = rowI.getCell(0);
                if(!super.isCellEmpty(cell0)){
                    String activityName = removeTrailingZeros(cell0.toString());
                    Activity activity = new Activity(activityName);
                    if(activity.name.equalsIgnoreCase("goal") || activity.name.equalsIgnoreCase("start")){
                        activity.isReal = false;
                    }
                    activities.add(activity);
                }
            }
        }
        return activities;
    }

    private void readLinks(List<Activity> activities){
        XSSFSheet linksSheet = workbook.getSheet("Links - Incidence");
        ArrayList<Link> links = new ArrayList<>();
        XSSFRow row0 = linksSheet.getRow(0);
        if(row0 != null){
            for(int i = 0; i < row0.getLastCellNum() - 1; i++){
                XSSFCell parentNameCell = row0.getCell(i + 1);
                if(!super.isCellEmpty(parentNameCell)){
                    Activity parentNode = null;
                    for(Activity activity : activities){
                        if(activity.name.equals(parentNameCell.toString())){
                            parentNode = activity;
                        }
                    }
                    if(parentNode != null){
                        for(int j = 0; j < linksSheet.getLastRowNum(); j++){
                            XSSFRow rowJ = linksSheet.getRow(j + 1);
                            XSSFCell childNameCell = rowJ.getCell(0);
                            if(!super.isCellEmpty(childNameCell)){
                                Activity childNode = null;
                                for(Activity node : activities){
                                    if(node.name.equals(childNameCell.toString())){
                                        childNode = node;
                                    }
                                }
                                XSSFCell valueCell = rowJ.getCell(i + 1);
                                if(childNode != null && !super.isCellEmpty(valueCell)){
                                    ActivityLink link = new ActivityLink(childNode, parentNode);
                                    links.add(link);
                                }
                            }
                        }
                    }
                }
            }
        }

        super.network = new Network(new ArrayList<>(activities), links, iConfig.inputQueues);
    }

    private void readAlpha(){
        XSSFSheet alphaSheet = workbook.getSheet("Alpha");
        XSSFRow row0 = alphaSheet.getRow(0);
        if(row0 != null){
            for(int i = 0; i < row0.getLastCellNum() - 1; i++){
                XSSFCell parentNameCell = row0.getCell(i + 1);
                if(!super.isCellEmpty(parentNameCell)){
                    Activity parentActivity = null;
                    for(Activity activity : super.network.getActivities()){
                        if(activity.name.equals(parentNameCell.toString())){
                            parentActivity = activity;
                        }
                    }
                    if(parentActivity != null){
                        for(int j = 0; j < alphaSheet.getLastRowNum(); j++){
                            XSSFRow rowJ = alphaSheet.getRow(j + 1);
                            XSSFCell childNameCell = rowJ.getCell(0);
                            if(!super.isCellEmpty(childNameCell)){
                                Activity childActivity = null;
                                for(Activity activity : super.network.getActivities()){
                                    if(activity.name.equals(childNameCell.toString())){
                                        childActivity = activity;
                                    }
                                }
                                XSSFCell valueCell = rowJ.getCell(i + 1);
                                if(childActivity != null && !super.isCellEmpty(valueCell)){
                                    ActivityLink parentChildLink = (ActivityLink) network.getLink(parentActivity, childActivity);
                                    if(parentChildLink != null){
                                        parentChildLink.SOD = valueCell.getNumericCellValue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void readBeta(){
        XSSFSheet betaSheet = workbook.getSheet("Beta");
        XSSFRow row0 = betaSheet.getRow(0);
        if(row0 != null){
            for(int i = 0; i < row0.getLastCellNum() - 1; i++){
                XSSFCell parentNameCell = row0.getCell(i + 1);
                if(!super.isCellEmpty(parentNameCell)){
                    Activity parentActivity = null;
                    for(Activity activity : super.network.getActivities()){
                        if(activity.name.equals(parentNameCell.toString())){
                            parentActivity = activity;
                        }
                    }
                    if(parentActivity != null){
                        for(int j = 0; j < betaSheet.getLastRowNum(); j++){
                            XSSFRow rowJ = betaSheet.getRow(j + 1);
                            XSSFCell childNameCell = rowJ.getCell(0);
                            if(!super.isCellEmpty(childNameCell)){
                                Activity childActivity = null;
                                for(Activity activity : super.network.getActivities()){
                                    if(activity.name.equals(childNameCell.toString())){
                                        childActivity = activity;
                                    }
                                }
                                XSSFCell valueCell = rowJ.getCell(i + 1);
                                if(childActivity != null && !super.isCellEmpty(valueCell)){
                                    ActivityLink parentChildLink = (ActivityLink) network.getLink(parentActivity, childActivity);
                                    if(parentChildLink != null){
                                        parentChildLink.COD = valueCell.getNumericCellValue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
