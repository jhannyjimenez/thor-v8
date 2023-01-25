package org.mitre.thor.input.formats;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mitre.input_correcter.StandardInputCorrecter;
import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.App;
import org.mitre.thor.input.Input;
import org.mitre.thor.input.InputConfiguration;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.loops.TDeLoop;
import org.mitre.thor.network.nodes.Activity;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

//input from standard form excel
public class StandardInput extends Input {

    public XSSFSheet linksSheet;

    public StandardInput(InputConfiguration iConfig, App app){
        super(iConfig, app);
    }

    @Override
    public boolean read() {
        //attempt to gain access to the workbook
        try {
            super.fis = new FileInputStream(super.iConfig.filePath);
            super.workbook = new XSSFWorkbook(fis);
            app.GAS("Successfully Loaded the Excel XSSF Workbook at " + super.iConfig.filePath, false);
        }catch (IOException exception){
            app.GAE("Failed to load the Excel XSSF Workbook at " + super.iConfig.filePath, true);
            return false;
        }

        StandardInputCorrecter correcter = new StandardInputCorrecter(this);
        correcter.findLinksSheetErrors(usesFDNA);

        if(correcter.errors.isEmpty()){
            app.GAS("'Links Sheet' does not contain any errors", false);
        }else{
            app.GAE("Errors were found at the 'Links sheet' in the loaded Excel XSSF Workbook", true);
            correcter.printErrors(false);
            return false;
        }

        boolean success = readLinks();
        if(success){
            CoreLogger.logger.log(Level.INFO, "Successfully read the links sheet");
        }else{
            app.GAE("Failed to read the 'Links' sheet", true);
            return false;
        }

        if(!super.network.findStartAndEnd()){
            app.GAE("Failed to establish start and end node. Please make sure that there is only one root and end node", true);
            return false;
        }

        correcter.findDependenciesErrors();

        super.usesFDNA = false; super.usesODINN = false;
        if(iConfig.checkNSP){
            if(correcter.errors.isEmpty()){
                List<String> rules = findRollUpRulesUsedInDependencies();
                for(String rule: rules){
                    if(rule.toLowerCase().contains("fdna")){
                        usesFDNA = true;
                    }
                    if(rule.toLowerCase().contains("odinn") || rule.toLowerCase().contains("soda")){
                        usesODINN = true;
                    }
                    if(usesODINN && usesFDNA){
                        break;
                    }
                }

                readNodeSpecificRollUpRules();

            }else{
                app.GAE("Errors were found in the 'Dependencies sheet' in the loaded Excel XSSF Workbook", true);
                correcter.printErrors(false);
                return false;
            }
        }

        if(iConfig.checkFDNA){
            usesFDNA = true;
        }
        if(iConfig.checkODINN){
            usesODINN = true;
        }

        if(usesODINN){
            correcter.findFactorsErrors();
            if (correcter.errors.isEmpty()) {
                readFactors();
            } else {
                app.GAE("Errors were found in the 'Factors' sheet in the loaded XSSF Workbook", true);
                correcter.printErrors(false);
                return false;
            }
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
                    this.network = TDeLoop.createDeLoopedGraph(this.network);
                    System.out.println("Check");
                }else{
                    loopsExist = false;
                    if(super.looped){
                        app.GAS("Loops fixed!", true);
                    }
                }
            }

        }catch (CloneNotSupportedException e){
            e.printStackTrace();
        }

        readGlobalDependencies();
        if(readAnalysisSpecificDependencies()){
            return false;
        }

        if(iConfig.containsAnalysis(AnalysesForm.ATTACK)){
            super.readAttackTree();
        }

        super.network.addNetworkAnalysisDataHolders(true);
        hasRead = true;
        return true;
    }

    //reads the link sheet
    private boolean readLinks(){

        linksSheet = super.workbook.getSheet("Links");
        super.network = new Network();
        super.network.inputQueues = super.iConfig.inputQueues;

        XSSFRow row0 = linksSheet.getRow(0);
        int chanceColumnIndex = -5;
        int iodColumnIndex = -5;
        int codColumnIndex = -5;
        int sodColumnIndex = -5;

        //TODO: document links on chance
        //TODO: change documentation to say that the alpha and beta columns require specific headers
        if(row0 != null){
            for(int i = 0; i < row0.getLastCellNum(); i++){
                XSSFCell cellI = row0.getCell(i);
                if(cellI != null){
                    String cellS = cellI.getStringCellValue().toLowerCase(Locale.ROOT);
                    if(cellS.contains("chance")){
                        chanceColumnIndex = i;
                    }else if(cellS.contains("iod")){
                        iodColumnIndex = i;
                    }else if(cellS.contains("sod") || cellS.contains("alpha")){
                        sodColumnIndex = i;
                    }else if(cellS.contains("cod") || cellS.contains("beta")){
                        codColumnIndex = i;
                    }
                }
            }
        }

        //region create all the nodes and assign the children and parents
        for (int i = 0; i < linksSheet.getLastRowNum(); i++) {
            XSSFRow row = linksSheet.getRow(i + 1);
            XSSFCell columnB = row.getCell(1);
            XSSFCell columnC = row.getCell(2);
            XSSFCell columnD = row.getCell(3);
            XSSFCell columnE = row.getCell(4);

            double iod = 0, sod = 0, cod = 0;
            if(iodColumnIndex >= 0){
                XSSFCell iodCell = row.getCell(iodColumnIndex);
                iod = iodCell.getNumericCellValue();
            }
            if(sodColumnIndex >= 0){
                XSSFCell sodCell = row.getCell(sodColumnIndex);
                sod = sodCell.getNumericCellValue();
            }
            if(codColumnIndex >= 0){
                XSSFCell codCell = row.getCell(codColumnIndex);
                cod = codCell.getNumericCellValue();
            }

            double linkChance = 1.0;
            if(chanceColumnIndex != -5){
                XSSFCell onChanceColumn = row.getCell(chanceColumnIndex);
                if(onChanceColumn != null){
                    linkChance = onChanceColumn.getNumericCellValue();
                }
            }

            if(Double.isNaN(linkChance)){
                linkChance = 1.0;
            }
            if(Double.isNaN(iod)){
                iod = 0.0;
            }
            if(Double.isNaN(sod)){
                sod = 0.0;
            }
            if(Double.isNaN(cod)){
                cod = 0.0;
            }

            String childName = removeTrailingZeros(columnB.toString());
            String parentName = removeTrailingZeros(columnD.toString());

            int cId =  (int) columnC.getNumericCellValue();
            int pId = (int) columnE.getNumericCellValue();

            Activity childActivity = super.network.createActivity(childName, cId, cId, true);
            Activity parentActivity = super.network.createActivity(parentName, pId, pId, true);

            super.network.linkActivities(childActivity, parentActivity, linkChance, iod, sod, cod,null);
        }
        //endregion

        return true;
    }
}
