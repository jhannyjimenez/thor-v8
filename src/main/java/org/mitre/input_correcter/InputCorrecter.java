package org.mitre.input_correcter;

import javafx.util.Pair;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.mitre.thor.analyses.AttackAnalysis;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.Input;
import org.mitre.thor.network.attack.Decision;
import org.mitre.thor.network.attack.DecisionTree;
import org.mitre.thor.network.attack.Route;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;
import org.mitre.thor.analyses.target.TargetType;
import org.mitre.thor.network.nodes.Factor;
import org.mitre.thor.network.nodes.Group;
import org.mitre.thor.network.nodes.Node;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Used to check for potential user-made errors in any input
 */
public class InputCorrecter {

    protected final Input input;
    protected final XSSFCellStyle fixedStyle;
    
    public final ArrayList<String> errors = new ArrayList<>();
    public final ArrayList<String> fixed = new ArrayList<>();

    InputCorrecter(Input input){
        this.input = input;
        fixedStyle = input.workbook.createCellStyle();
        fixedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        fixedStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.index);
    }

    //TODO: make cleaner using the new private 'validate" methods
    public void findDependenciesErrors(){
        clear();

        boolean dependencies = false;

        XSSFSheet dependenciesSheet = input.workbook.getSheet("Dependencies");
        if(dependenciesSheet != null){
            dependencies = true;
        }else{
            dependenciesSheet = input.workbook.getSheet("Activities");
            if(dependenciesSheet != null){
                dependencies = true;
            }else{
                dependenciesSheet = input.workbook.getSheet("Nodes");
                if(dependenciesSheet != null){
                    dependencies = true;
                }
            }
        }

        if(!dependencies){
            String msg = "Could not find any sheets labeled 'Dependencies' or 'Activities' or 'Nodes'";
            throwError(msg);
        }else{
            int numOfRows = dependenciesSheet.getLastRowNum();
            List<String> usedNodeNames = new ArrayList<>();
            List<Integer> usedNodeIds = new ArrayList<>();

            List<Pair<XSSFCell, String>> badRollUps = new ArrayList<>();
            ArrayList<String> goodRollUps = new ArrayList<>();
            List<Pair<XSSFRow, String>> unusedRows = new ArrayList<>();
            List<Pair<Activity, String>> unlistedNodes = new ArrayList<>();

            int aColumn = -1;
            int bColumn = -1;
            int nodeRuleColumn = -1;
            int groupRuleColumn = -1;
            int factorRuleColumn = -1;
            int seColumn = -1;
            boolean setNamingMethod = false;
            boolean usingNames = true;

            List<Integer> timeColumns = new ArrayList<>();

            XSSFRow headerRow = dependenciesSheet.getRow(0);
            if(headerRow == null){
                String msg = "Header row in the 'Dependencies Sheet' is null";
                throwError(msg);
            }else{
                int headerRowSize = headerRow.getLastCellNum();
                for(int i = 0; i < headerRowSize; i++){
                    XSSFCell headerCell = headerRow.getCell(i);
                    if(isCellEmpty(headerCell)){
                        String msg = "The cell at " + integerToAlphabetic(i) + (1) + " 'Dependencies Sheet' is blank";
                        throwError(msg);
                    }else if(headerCell.getCellType() != CellType.STRING){
                        String msg = "The cell at " + integerToAlphabetic(i) + (1) + " 'Dependencies Sheet' is not a text cell";
                        throwError(msg);
                    }else if(i == 0 && headerCell.getStringCellValue().toLowerCase().contains("name")){
                        setNamingMethod = true;
                    }else if(i == 0 && headerCell.getStringCellValue().toLowerCase().contains("id")){
                        usingNames = false;
                        setNamingMethod = true;
                    }else if(headerCell.getStringCellValue().equalsIgnoreCase("a")){
                        aColumn = i;
                    }else if(headerCell.getStringCellValue().equalsIgnoreCase("b")){
                        bColumn = i;
                    }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && (headerCell.getStringCellValue().toLowerCase().contains("node") || headerCell.getStringCellValue().toLowerCase().contains("activity"))
                    && !headerCell.getStringCellValue().toLowerCase().contains("group") && !headerCell.getStringCellValue().toLowerCase().contains("factor")){
                        nodeRuleColumn = i;
                    }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && headerCell.getStringCellValue().toLowerCase().contains("group")
                            && !headerCell.getStringCellValue().toLowerCase().contains("factor")){
                        groupRuleColumn = i;
                    }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && headerCell.getStringCellValue().toLowerCase().contains("factor")){
                        factorRuleColumn = i;
                    }else if(headerCell.getStringCellValue().toLowerCase().contains("time")){
                        timeColumns.add(i);
                    }else if(headerCell.getStringCellValue().equalsIgnoreCase("se")){
                        seColumn = i;
                    }
                }

                if(!setNamingMethod){
                    String msg = "Cell A1 in the 'Dependencies Sheet' does not contain the value 'nodes' or 'ids'";
                    throwError(msg);
                }

                if(input.iConfig.checkNSP && nodeRuleColumn == -1){
                    String msg = "Could not find a column header containing the words 'node' and 'rule'";
                    throwError(msg);
                }

                if(input.iConfig.checkNSP && groupRuleColumn == -1 && input.iConfig.checkODINN){
                    String msg = "Could not find a column header containing the words 'group' and 'rule'";
                    throwError(msg);
                }

                if(input.iConfig.checkNSP && factorRuleColumn == -1 && input.iConfig.checkODINN){
                    String msg = "Could not find a column header containing the words 'factor' and 'rule'";
                    throwError(msg);
                }

                for(int i = 0; i < numOfRows; i++){
                    XSSFRow row  = dependenciesSheet.getRow(i + 1);
                    XSSFCell nodeCell = row.getCell(0);

                    if(isCellEmpty(nodeCell)){
                        String msg = "The cell at " + integerToAlphabetic(0) + (i + 2) + " 'Dependencies Sheet' is blank - supposed to contain node name";
                        throwError(msg);
                    }else{
                        if(usingNames){
                            if(nodeCell.getCellType() != CellType.STRING){
                                String msg = "The cell at " + integerToAlphabetic(0) + (row.getRowNum() + 1) + " 'Dependencies Sheet' is supposed to be of type String";
                                throwError(msg);
                            }else{
                                usedNodeNames.add(nodeCell.getStringCellValue());
                            }
                        }else{
                            if(nodeCell.getCellType() != CellType.NUMERIC){
                                String msg = "The cell at " + integerToAlphabetic(0) + (row.getRowNum() + 1) + " 'Dependencies Sheet' is supposed to be of type Numeric";
                                throwError(msg);
                            }else{
                                usedNodeIds.add((int) nodeCell.getNumericCellValue());
                            }
                        }

                        boolean nodeIsUsed = false;
                        for(Activity activity : input.network.getActivities()){
                            if(usingNames){
                                if(activity.name.equals(nodeCell.toString())){
                                    nodeIsUsed = true;
                                    break;
                                }
                            }else{
                                if(activity.id == nodeCell.getNumericCellValue()){
                                    nodeIsUsed = true;
                                    break;
                                }
                            }
                        }
                        if(!nodeIsUsed){
                            String msg = "The cell at " + integerToAlphabetic(0) + (row.getRowNum() + 1) + " 'Dependencies Sheet' contains a node which does not exist in the links sheet";
                            unusedRows.add(new Pair<>(row, msg));

                        }
                    }

                    if(input.iConfig.checkNSP && nodeRuleColumn != -1){
                        XSSFCell rollUpRuleCell = row.getCell(nodeRuleColumn);
                        String rule = rollUpRuleCell.getStringCellValue().toLowerCase();

                        if(isCellEmpty(rollUpRuleCell)){
                            String msg = "The cell at " + integerToAlphabetic(nodeRuleColumn) + (i + 2) + " 'Dependencies Sheet' is blank - supposed to contain roll up rule";
                            rollUpRuleCell = row.createCell(nodeRuleColumn);
                            rollUpRuleCell.setCellValue("");
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else if(rollUpRuleCell.getCellType() != CellType.STRING){
                            String msg = "The cell at " + integerToAlphabetic(nodeRuleColumn) + (i + 2) + " 'Dependencies Sheet' is not a text cell - supposed to contain roll up rule";
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else if(!rule.equals("and") && !rule.equals("or") && !rule.equals("fdna") && !rule.equals("fdna2") && !rule.equals("fdna-or") && !rule.equals("odinn-fti")  && !rule.equals("soda-fti") && !rule.equals("odinn") && !rule.equals("soda") && !rule.equals("average") && !rule.equals("avg") && !rule.equals("threshold")){
                            String msg = "The cell at " + integerToAlphabetic(nodeRuleColumn) + (i + 2) + " 'Dependencies Sheet' is supposed have a value of 'and','or','fdna','odinn', 'odinn-ft', 'soda', 'soda-fti', 'fdna-or', 'average', 'avg', or 'threshold'";
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else{
                            goodRollUps.add(rule.toLowerCase());
                        }
                    }

                    if(input.iConfig.checkNSP && factorRuleColumn != -1){
                        XSSFCell rollUpRuleCell = row.getCell(factorRuleColumn);
                        String rule = rollUpRuleCell.getStringCellValue().toLowerCase();

                        if(isCellEmpty(rollUpRuleCell)){
                            String msg = "The cell at " + integerToAlphabetic(factorRuleColumn) + (i + 2) + " 'Dependencies Sheet' is blank - supposed to contain roll up rule";
                            rollUpRuleCell = row.createCell(factorRuleColumn);
                            rollUpRuleCell.setCellValue("");
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else if(rollUpRuleCell.getCellType() != CellType.STRING){
                            String msg = "The cell at " + integerToAlphabetic(factorRuleColumn) + (i + 2) + " 'Dependencies Sheet' is not a text cell - supposed to contain roll up rule";
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else if(!rule.equals("and") && !rule.equals("or")&& !rule.equals("average") && !rule.equals("avg")&& !rule.equals("threshold")){
                            String msg = "The cell at " + integerToAlphabetic(factorRuleColumn) + (i + 2) + " 'Dependencies Sheet' is supposed have a value of 'and', 'or', 'average', 'avg', or 'threshold'";
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else{
                            goodRollUps.add(rule.toLowerCase());
                        }
                    }

                    if(input.iConfig.checkNSP && groupRuleColumn != -1){
                        XSSFCell rollUpRuleCell = row.getCell(groupRuleColumn);
                        String rule = rollUpRuleCell.getStringCellValue().toLowerCase();

                        if(isCellEmpty(rollUpRuleCell)){
                            String msg = "The cell at " + integerToAlphabetic(groupRuleColumn) + (i + 2) + " 'Dependencies Sheet' is blank - supposed to contain roll up rule";
                            rollUpRuleCell = row.createCell(groupRuleColumn);
                            rollUpRuleCell.setCellValue("");
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else if(rollUpRuleCell.getCellType() != CellType.STRING){
                            String msg = "The cell at " + integerToAlphabetic(groupRuleColumn) + (i + 2) + " 'Dependencies Sheet' is not a text cell - supposed to contain roll up rule";
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else if(!rule.equals("and") && !rule.equals("or") && !rule.equals("average") && !rule.equals("avg") && !rule.equals("threshold")){
                            String msg = "The cell at " + integerToAlphabetic(groupRuleColumn) + (i + 2) + " 'Dependencies Sheet' is supposed have a value of 'and', 'or', 'average', 'avg', or 'threshold'";
                            badRollUps.add(new Pair<>(rollUpRuleCell, msg));
                        }else{
                            goodRollUps.add(rule.toLowerCase());
                        }
                    }

                    if(input.iConfig.containsAnalysis(AnalysesForm.CRITICALITY)){
                        XSSFCell aCell = null;
                        if(aColumn >= 0){
                            aCell = row.getCell(aColumn);
                        }
                        XSSFCell bCell = null;
                        if(bColumn >= 0){
                            bCell = row.getCell(bColumn);
                        }
                        if(aCell != null && isCellEmpty(aCell)){
                            String msg = "The cell at " + integerToAlphabetic(aColumn) + (i + 2) + " 'Dependencies Sheet' is blank - supposed to contain linear regression a value";
                            throwError(msg);
                        }
                        if(bCell != null && isCellEmpty(bCell)){
                            String msg = "The cell at " + integerToAlphabetic(bColumn) + (i + 2) + " 'Dependencies Sheet' is blank - supposed to contain linear regression b value";
                            throwError(msg);
                        }
                    }

                    if(input.iConfig.containsAnalysis(AnalysesForm.DYNAMIC_OPERABILITY)){
                        for(Integer timeColumn : timeColumns) {
                            XSSFCell cell = row.getCell(timeColumn);
                            if (!isCellEmpty(cell) && (cell.getCellType() != CellType.NUMERIC && cell.getCellType() != CellType.FORMULA)) {
                                String msg = "The cell at " + integerToAlphabetic(timeColumn) + (i + 2) + " 'Dependencies Sheet' is not a numeric cell";
                                throwError(msg);
                            }else if(!isCellEmpty(cell) && (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA)){
                                double value = cell.getNumericCellValue();
                                if(value > 100){
                                    String msg = "The cell at " + integerToAlphabetic(timeColumn) + (i + 2) + " 'Dependencies Sheet' cannot be greater than 100";
                                    throwError(msg);
                                }else if(value < 0){
                                    String msg = "The cell at " + integerToAlphabetic(timeColumn) + (i + 2) + " 'Dependencies Sheet' cannot be less than 0";
                                    throwError(msg);
                                }
                            }
                        }
                    }

                    if(input.iConfig.targetTypes.contains(TargetType.NODES) && seColumn != -1 && (input.iConfig.checkODINN || (goodRollUps.contains("odinn") || goodRollUps.contains("odinn-fti")))){
                        XSSFCell cell = row.getCell(seColumn);
                        if (!isCellEmpty(cell) && (cell.getCellType() != CellType.NUMERIC && cell.getCellType() != CellType.FORMULA)) {
                            String msg = "The cell at " + integerToAlphabetic(seColumn) + (i + 2) + " 'Dependencies Sheet' is not a numeric cell";
                            throwError(msg);
                        }else if(!isCellEmpty(cell) && (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA)){
                            double value = cell.getNumericCellValue();
                            if(value > 100){
                                String msg = "The cell at " + integerToAlphabetic(seColumn) + (i + 2) + " 'Dependencies Sheet' cannot be greater than 100";
                                throwError(msg);
                            }else if(value < 0){
                                String msg = "The cell at " + integerToAlphabetic(seColumn) + (i + 2) + " 'Dependencies Sheet' cannot be less than 0";
                                throwError(msg);
                            }
                        }
                    }
                }

                if(input.iConfig.targetTypes.contains(TargetType.NODES) && seColumn == -1 && (input.iConfig.checkODINN || (goodRollUps.contains("odinn") || goodRollUps.contains("odinn-fti")))){
                    throwError("Could not find an SE column in the Dependencies Sheet");
                    throwError("\tThis info is required when targeting Nodes with ODINN");
                }

                for(Activity activity :input.network.getActivities()){
                    if(activity != input.network.startActivity){
                        if(usingNames){
                            if((activity.isReal || activity == input.network.goalActivity) && !usedNodeNames.contains(activity.name)){
                                String msg = "The 'Dependencies Sheet' does not contain the node labeled: " + activity.name;
                                unlistedNodes.add(new Pair<>(activity, msg));
                            }
                        }else{
                            if((activity.isReal || activity == input.network.goalActivity) && !usedNodeIds.contains(activity.id)){
                                String msg = "The 'Dependencies Sheet' does not contain the node id: " + activity.id;
                                unlistedNodes.add(new Pair<>(activity, msg));
                            }
                        }
                    }
                }

                if((!unusedRows.isEmpty() || !unlistedNodes.isEmpty() || !badRollUps.isEmpty()) && input.app.controller != null){
                    input.app.controller.askQuestion("Would you like THOR to fix some of the Dependency errors?");
                    if(input.app.controller.questionAnswer){
                        int rowI = dependenciesSheet.getLastRowNum() + 1;
                        for(Pair<Activity, String> pair : unlistedNodes){
                            XSSFRow row = dependenciesSheet.createRow(rowI);
                            XSSFCell nameCell = row.createCell(0);
                            nameCell.setCellValue(pair.getKey().name);
                            nameCell.setCellStyle(fixedStyle);
                            if(aColumn != -1){
                                XSSFCell aCell = row.createCell(aColumn);
                                aCell.setCellValue(1);
                                pair.getKey().A = 1.0;
                                aCell.setCellStyle(fixedStyle);
                            }
                            if(bColumn != -1){
                                XSSFCell bCell = row.createCell(bColumn);
                                bCell.setCellValue(-0.5);
                                pair.getKey().B = -0.5;
                                bCell.setCellStyle(fixedStyle);
                            }
                            if(nodeRuleColumn != -1){
                                XSSFCell ruleCell = row.createCell(nodeRuleColumn);
                                ruleCell.setCellValue("or");
                                pair.getKey().customActivityRule = RollUpEnum.OR;
                                ruleCell.setCellStyle(fixedStyle);
                            }
                            if(factorRuleColumn != -1){
                                XSSFCell ruleCell = row.createCell(factorRuleColumn);
                                ruleCell.setCellValue("and");
                                pair.getKey().customFactorRule = RollUpEnum.AND;
                                ruleCell.setCellStyle(fixedStyle);
                            }
                            rowI++;
                            throwFixed(pair.getValue());
                        }
                        for(Pair<XSSFCell, String> pair : badRollUps){
                            pair.getKey().setCellValue("or");
                            throwFixed(pair.getValue());
                            pair.getKey().setCellStyle(fixedStyle);
                        }
                        for(Pair<XSSFRow, String> pair : unusedRows){
                            deleteRow(dependenciesSheet, pair.getKey());
                            throwFixed(pair.getValue());
                        }
                        write();
                        printFixed();
                    }else{
                        for (Pair<XSSFRow, String> pair : unusedRows){
                            throwError(pair.getValue());
                        }
                        for(Pair<Activity, String> pair : unlistedNodes){
                            throwError(pair.getValue());
                        }
                        for(Pair<XSSFCell, String> pair : badRollUps){
                            throwError(pair.getValue());
                        }
                    }
                }
            }
        }
    }

    //TODO: make cleaner using the new private 'validate" methods
    public void findFactorsErrors() {
        clear();
        XSSFSheet factorsSheet = input.workbook.getSheet("Factors");
        if (factorsSheet == null) {
            throwError("Could not find any sheets labeled 'Factors'");
            return;
        }

        XSSFRow headerRow = factorsSheet.getRow(0);
        if (headerRow == null) {
            throwError("Row 1 in the 'Factors' sheet is null. This row should contain the following cells:");
            throwError("\tFactor ID, Group ID, Node ID, FIV, Binary, and Factor Name");
            return;
        }

        int fidColumn = -1;
        int gidColumn = -1;
        int nidColumn = -1;
        int fivColumn = -1;
        int binaryColumn = -1;
        int nameColumn = -1;

        for(int i = 0; i < headerRow.getLastCellNum(); i++){
            XSSFCell headerCell = headerRow.getCell(i);
            if (headerCell.getCellType() != CellType.STRING) {
                throwError("The cell " + integerToAlphabetic(i) + "1 in the 'Factors' sheet must be a text cell");
            } else {
                String lHeaderCell = headerCell.getStringCellValue().toLowerCase();
                if(lHeaderCell.contains("factor") && lHeaderCell.contains("id")){
                    fidColumn = i;
                }else if(lHeaderCell.contains("group") && lHeaderCell.contains("id")){
                    gidColumn = i;
                }else if(lHeaderCell.contains("node") && lHeaderCell.contains("id")){
                    nidColumn = i;
                }else if(lHeaderCell.equals("fiv") || lHeaderCell.contains("impact")){
                    fivColumn = i;
                }else if(lHeaderCell.equals("binary") || lHeaderCell.equals("vulnerable")){
                    binaryColumn = i;
                }else if(lHeaderCell.contains("name")){
                    nameColumn = i;
                }
            }
        }

        if (fidColumn == -1) {
            throwError("Could not find the 'Factor ID' column in the factors sheet.");
            throwError("\tMake sure that the first cell in the column contains the text 'Factor ID'");
        }

        if (nidColumn == -1) {
            throwError("Could not find the 'Node ID' column in the factors sheet.");
            throwError("\tMake sure that the first cell in the column contains the text 'Node ID'");
        }

        if (fivColumn == -1) {
            throwError("Could not find the 'FIV' column in the factors sheet.");
            throwError("\tMake sure that the first cell in the column contains the text 'FIV'");
        }

        if (binaryColumn == -1) {
            throwError("Could not find the 'Binary' column in the factors sheet.");
            throwError("\tMake sure that the first cell in the column contains the text 'Binary'");
        }

        if (nameColumn == -1) {
            throwError("Could not find the 'Factor Name' column in the factors sheet.");
            throwError("\tMake sure that the first cell in the column contains the text 'Factor Name'");
        }

        int numOfRows = factorsSheet.getLastRowNum();
        for(int i = 0; i < numOfRows; i++){
            XSSFRow iRow = factorsSheet.getRow(i + 1);
            if (iRow == null) {
                throwError("Row " + (i + 1) + " in the 'Factors' sheet is null. Add cells or delete row.");
            } else {
                XSSFCell fIdCell = fidColumn != -1 ? iRow.getCell(fidColumn) : null;
                XSSFCell nIdCell = nidColumn != -1 ? iRow.getCell(nidColumn) : null;
                XSSFCell fivCell = fivColumn != -1 ? iRow.getCell(fivColumn) : null;
                XSSFCell binCell = binaryColumn != -1 ? iRow.getCell(binaryColumn) : null;
                XSSFCell nameCell = nameColumn != -1 ? iRow.getCell(nameColumn) : null;
                XSSFCell gIdCell = gidColumn != -1 ? iRow.getCell(gidColumn) : null;

                int rowExcel = i + 2;

                if (nIdCell != null && nIdCell.getCellType() != CellType.NUMERIC) {
                    throwError("The cell " + integerToAlphabetic(nidColumn) + "" + rowExcel + " in the 'Factors' sheet must be a number");
                } else if (nIdCell != null && nIdCell.getCellType() == CellType.NUMERIC){
                    if (input.network.getNode(nIdCell.getNumericCellValue()) == null) {
                        throwError("The node id '" + nIdCell.getNumericCellValue() + "' in cell " +
                                integerToAlphabetic(nidColumn) + "" + rowExcel +
                                " in the 'Factors' sheet is not the ID of any node in the 'Links' sheet");
                    }
                } else if(nIdCell == null) {
                    throwError("The cell " + integerToAlphabetic(nidColumn) + "" + rowExcel + " in the 'Factors' sheet is null. Add a value.");
                }

                String pre1 = "The cell " + integerToAlphabetic(fidColumn) + "" + rowExcel + " in the 'Factors' sheet";
                if (fIdCell != null && fIdCell.getCellType() != CellType.NUMERIC) {
                    throwError(pre1 + " must be a number");
                } else if (fidColumn != -1 && fIdCell == null) {
                    throwError(pre1 + " is null. Add numeric value");
                }

                String pre2 = "The cell " + integerToAlphabetic(gidColumn) + "" + rowExcel + " in the 'Factors' sheet";
                if (gIdCell != null && gIdCell.getCellType() != CellType.NUMERIC) {
                    throwError(pre2 + " sheet must be a number");
                } else if(gidColumn != -1 && gIdCell == null) {
                    throwError(pre2 + " is null. Add numeric value");
                }

                String pre3 = "The cell " + integerToAlphabetic(fivColumn) + "" + rowExcel + " in the 'Factors' sheet";
                if (fivCell != null && fivCell.getCellType() != CellType.NUMERIC) {
                    throwError(pre3 + " must be a number");
                } else if (fivColumn != -1 && fivCell == null) {
                    throwError(pre3 + " is null. Add numeric value");
                }

                String pre4 = "The cell " + integerToAlphabetic(binaryColumn) + "" + rowExcel + " in the 'Factors' sheet";
                if (binCell != null && binCell.getCellType() != CellType.NUMERIC) {
                    throwError(pre4 + " must be a number");
                } else if (binaryColumn != -1 && binCell == null) {
                    throwError(pre4 + " is null. Add numeric value");
                } else if (binCell != null && binCell.getNumericCellValue() != 0.0 && binCell.getNumericCellValue() != 1.0) {
                    throwError(pre4 + " must be a numeric value. Either 1 or 0");
                }

                String pre5 = "The cell " + integerToAlphabetic(nameColumn) + "" + rowExcel + " in the 'Factors' sheet";
                if (nameCell != null && (nameCell.toString().isBlank() || nameCell.toString().isEmpty())) {
                    throwError(pre5 + " must not be empty or blank");
                } else if (nameColumn != -1 && nameCell == null) {
                    throwError(" is null. Add a value");
                }
            }
        }
    }

    public void findAttackChainErrors() {
        clear();
        DecisionTree decisionTree = new DecisionTree(false, 0.0);
        XSSFSheet decisionSheet = input.workbook.getSheet("Decisions");
        if (!validateSheet(decisionSheet, "Decision", "Attack")) {
            return;
        }

        int numOfRow = decisionSheet.getLastRowNum();
        for(int i = 0; i < numOfRow; i++){
            XSSFRow iRow = decisionSheet.getRow(i + 1);
            if (validateRow(iRow, i + 1, "ID, Requirement, Cost, Description")) {
                XSSFCell idCell = iRow.getCell(0);validateCell(idCell, 0, i + 1, "Decisions", "ID", true);
                XSSFCell costCell = iRow.getCell(2);
                validateCell(costCell, 2, i + 1, "Decisions", "Cost", true);

                int id = idCell != null ? (int) idCell.getNumericCellValue() : -1;
                double cost = costCell != null ? costCell.getNumericCellValue() : 0;
                decisionTree.addDecision(new Decision(id, "", cost, ""));
            }
        }

        XSSFSheet routesSheet = input.workbook.getSheet("Routes");
        if (!validateSheet(routesSheet, "Routes", "Analysis")) {
            return;
        }
        int numOfRows = routesSheet.getLastRowNum();
        boolean opAt100 = false;
        for(int i = 0; i < numOfRows; i++){
            XSSFRow iRow = routesSheet.getRow(i + 1);
            if (validateRow(iRow, i + 1, "Decision ID, Route ID, Probability of Success")) {
                XSSFCell decisionIdCell = iRow.getCell(0);
                if (validateCell(decisionIdCell, 0, i + 1, "Routes", "Decision ID", true)) {
                    int dId = (int) decisionIdCell.getNumericCellValue();
                    if (!decisionTree.containsDecision(dId)) {
                        throwError("Could not find a decision with id: " + dId + ", in the 'Decisions'");
                        throwError("\tPlease fix cell " + integerToAlphabetic(0) + "" + (i + 2) + " in the 'Routes' sheet");
                    }
                }
                XSSFCell routeIdCell = iRow.getCell(1);
                validateCell(routeIdCell, 1, i + 1, "Routes", "Route ID", true);
                XSSFCell probSuccessCell = iRow.getCell(2);
                if (validateCell(probSuccessCell, 2, i + 1, "Routes", "Probability of Success", true)) {
                    double prob = probSuccessCell.getNumericCellValue();
                    clampCellValue(prob,2, i + 1, "Routes", 0, 1);
                }
                XSSFCell nodeNameCell = iRow.getCell(3);
                XSSFCell operabilityCell = iRow.getCell(4);

                String nodeName = nodeNameCell != null ? nodeNameCell.toString() : "";

                if (!nodeName.isBlank() && !nodeName.isEmpty()) {
                    Node node = input.network.getNode(nodeName);
                    if (node == null) {
                        throwError("The node at " + integerToAlphabetic(3) + "" + (i + 2) + " could not be found in the Network");
                    }
                    if (validateCell(operabilityCell, 4, i + 1, "Routes", "Node Operability", true)) {
                        double op = operabilityCell.getNumericCellValue();
                        clampCellValue(op, 4, i + 1, "Routes", 0, 100);
                        if (op == 100.0) {
                            opAt100 = true;
                        }
                    }
                }
            }
        }
        if (opAt100) {
            throwError("In the Routes sheet, no node should have an operability set to 100.");
            throwError("\this causes the node to have a permanent operability of 100");
            throwError("\tinstead leave the node and operability column blank");
        }
    }

    private boolean validateCell(XSSFCell cell, int column, int row, String sheet, String value, boolean isNumeric) {
        if (cell == null) {
            throwError("The cell " + integerToAlphabetic(column) + "" + (row + 1) + " in the " + sheet + " sheet" +
                    "is null.");
            throwError("\tCell should contain the " + value);
            return false;
        } else if (isNumeric && cell.getCellType() != CellType.NUMERIC) {
            throwError("The cell " + integerToAlphabetic(column) + "" + (row + 1) + " in the " + sheet + " sheet" +
                    "is not a numeric cell.");
            throwError("\tCell should contain the " + value + " which is a numeric value");
            return false;
        }
        return true;
    }

    private boolean clampCellValue(double value, int col, int row, String sheet, double min, double max) {
        if (value < min || value > max) {
            throwError("The value at cell " + integerToAlphabetic(col) + "" + (row + 1) + " in the '" + sheet + "' sheet");
            throwError("\tmust be a value between " + min + " and " + max + " (inclusive)");
            return false;
        }
        return true;
    }

    private boolean validateRow(XSSFRow row, int i, String contents) {
        if (row == null) {
            throwError("Row " + (i + 1) + " in the Decisions sheet is null");
            throwError("\trow should contain: " + contents);
            return false;
        }
        return true;
    }

    private boolean validateSheet(XSSFSheet sheet, String name, String analysis) {
        if (sheet == null) {
            throwError("Could not find any sheets labeled '" + name + "'. This sheet is required for the " + analysis + " analysis");
            return false;
        }
        return true;
    }

    public void printErrors(boolean transition){
        for(String error : errors){
            CoreLogger.logger.log(Level.SEVERE, error);
            input.app.GAE(error, transition);
        }
    }

    public void printFixed(){
        for(String fix : fixed){
            CoreLogger.logger.log(Level.INFO, "Fixed: " + fix);
            input.app.GAS("Fixed: " + fix, false);
        }
    }

    /**
     * Logs a message and adds it to a List of errors
     *
     * @param msg the message to be logged
     */
    protected void throwError(String msg){
        errors.add(msg);
    }
    
    protected void throwFixed(String msg){
        fixed.add(msg);
    }
    
    protected void clear(){
        errors.clear();
        fixed.clear();
    }

    /**
     * Convert any number to its excel alphabetic equivalent
     *
     * @param i the number to convert
     * @return the Excel alphabetic equivalent of the number i
     */
    protected String integerToAlphabetic(int i) {
        if( i<0 ) {
            return "-"+integerToAlphabetic(-i-1);
        }

        int quot = i/26;
        int rem = i%26;
        char letter = (char)((int)'A' + rem);
        if( quot == 0 ) {
            return ""+letter;
        } else {
            return integerToAlphabetic(quot-1) + letter;
        }
    }

    /**
     * Checks if an Excel cell is empty
     *
     * @param cell the Excel cell to check
     * @return whether the cell is empty or not
     */
    protected boolean isCellEmpty(XSSFCell cell){
        if(cell == null){
            return true;
        }
        if(cell.toString().replaceAll(" ", "").equals("")){
            return true;
        }
        return cell.getCellType() == CellType.BLANK;
    }

    protected static void deleteRow(XSSFSheet sheet, XSSFRow row)
    {
        int rowIndex = row.getRowNum();
        int lastRowNum = sheet.getLastRowNum();

        sheet.removeRow(row);   // this only deletes all the cell values

        if (rowIndex >= 0 && rowIndex < lastRowNum)
        {
            sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
        }
    }

    protected void write(){
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(input.iConfig.filePath);
        }catch (Exception e){
            e.printStackTrace();
            input.app.GAE("The file could not be written because it does not exist or it is being used by another process", true);
        }
        try {
            input.workbook.write(fos);
        }catch (Exception e){
            e.printStackTrace();
            input.app.GAE("Failed to write the output file at " + input.iConfig.filePath, true);
        }
    }
}
