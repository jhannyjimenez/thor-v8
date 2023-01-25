package org.mitre.thor.input;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mitre.thor.analyses.*;
import org.mitre.thor.analyses.data_holders.NodeAnalysisDataHolder;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.App;
import org.mitre.thor.network.*;
import org.mitre.thor.analyses.cg.ColorSet;
import org.mitre.thor.network.attack.DecisionTree;
import org.mitre.thor.network.attack.Decision;
import org.mitre.thor.network.attack.Route;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Factor;
import org.mitre.thor.network.nodes.Group;
import org.mitre.thor.analyses.crit.PhiCalculationEnum;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;
import org.mitre.thor.network.nodes.Node;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

//TODO add more information on how to create a new input
/**
 * The base class for every input.
 */
public abstract class Input {

    public static int inputCount = 0;
    public int ID;
    public boolean phiForceStop = false;

    public InputConfiguration iConfig;
    public boolean looped = false;
    public Network rawNetwork;
    public Network network;
    public DecisionTree decisionTree;
    public FileInputStream fis;
    public XSSFWorkbook workbook;
    public App app;

    public boolean usesFDNA = false;
    public boolean usesODINN = false;
    public boolean hasRead = false;

    public final static int MAX_TABLE_ROWS = 50000;

    //dy-op
    public int numTimes;
    public List<Integer> timeColumns;
    public List<Integer> seTimeColumns;

    //cg divisions
    public ArrayList<Integer> cGDivisionNumbers = new ArrayList<>();
    public ArrayList<int[]> cGDivisionColors = new ArrayList<>();

    //loading screen
    public BigInteger livePossibilitiesCount = BigInteger.ZERO;
    private double previousPercent = 0.0;
    private final DecimalFormat percentageDF = new DecimalFormat("00");

    //sheets
    public XSSFSheet dependenciesSheet;
    public XSSFSheet groupingSheet;
    public XSSFSheet scaleSheet;

    protected Input(InputConfiguration iConfig, App app){
        this.iConfig = iConfig;
        this.app = app;

        ID = Input.inputCount;
        Input.inputCount++;
    }

    public abstract boolean read();

    /**
     * Analysis the data read based on each Analysis selected from the GUI
     *
     * @return the function failed or succeeded
     */
    public boolean process(){

        getTargetNodes();

        boolean status = true;
        CoreLogger.logger.log(Level.INFO, "Starting analysis process");
        for(Analysis analysis : iConfig.analyses){
            status = analysis.inputProcess(this);
            if(!status){
                break;
            }
        }
        return status;
    }

    /**
     * Close the input workbook and File input stream
     */
    public void close(){
        try {
            workbook.close();
            CoreLogger.logger.log(Level.INFO, "Successfully closed the workbook");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            fis.close();
            CoreLogger.logger.log(Level.INFO, "Successfully closed the File Input Stream");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Update the GUI loading screen for the current process
     *
     * @param startInstant the instant the analysis process was started
     * @param maxMinutes the max number of minutes allowed
     * @param poss the total number of possibilities
     * @param method the phiCalculationMethod
     */
    public void updateLoadScreen(Instant startInstant, double maxMinutes, BigInteger poss, PhiCalculationEnum method){
        String possString = livePossibilitiesCount + " / " + poss + " Possibilities";
        BigDecimal totalPoss = new BigDecimal(poss);
        BigDecimal nowPoss = new BigDecimal(livePossibilitiesCount);
        if(method == PhiCalculationEnum.ALL){
            BigDecimal percent = nowPoss.divide(totalPoss,2, RoundingMode.CEILING);
            if(app.controller != null && previousPercent != percent.doubleValue()){
                app.controller.updateLoadingScreen(possString, "", percent.doubleValue());
            }else if(previousPercent != percent.doubleValue()){
                System.out.println(percentageDF.format(percent.doubleValue() * 100.0) + "%");
            }
            previousPercent = percent.doubleValue();
        }else{
            double minutesNow = (Duration.between(startInstant, Instant.now()).toSeconds()) / 60.0;
            double minutesR = maxMinutes - minutesNow;
            minutesR = Math.round(minutesR * 100.0) / 100.0;
            String minString = minutesR + " Minutes Available";
            double percent = minutesNow / maxMinutes;
            if(app.controller != null && previousPercent != percent){
                app.controller.updateLoadingScreen(possString, minString, percent);
            }else if(previousPercent != percent){
                System.out.println(percentageDF.format(percent * 100.0) + "%");
            }
            previousPercent = percent;
        }
    }

    public void updateLoadScreen(String poss, double percent){
        if(app.controller != null){
            app.controller.updateLoadingScreen(poss, "", percent);
        }else{
            System.out.println(percentageDF.format(percent * 100.0) + "%");
        }
    }

    /**
     * Check if a cell is empty
     *
     * @param cell the cell to be checked
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

    private void getTargetNodes(){

    }

    protected List<String> findRollUpRulesUsedInDependencies(){

        dependenciesSheet = getDependenciesSheet();
        if(dependenciesSheet != null) {
            int numOfRows = dependenciesSheet.getLastRowNum();

            XSSFRow headerRow = dependenciesSheet.getRow(0);
            int nodeRuleColumn = -1;
            int factorRuleColumn = -1;
            int groupRuleColumn = -1;
            int headerRowSize = headerRow.getLastCellNum();

            for (int i = 0; i < headerRowSize; i++) {
                XSSFCell headerCell = headerRow.getCell(i);
                if(headerCell.getStringCellValue().toLowerCase().contains("rule") && (headerCell.getStringCellValue().toLowerCase().contains("node") || headerCell.getStringCellValue().toLowerCase().contains("activity"))
                        && !headerCell.getStringCellValue().toLowerCase().contains("group") && !headerCell.getStringCellValue().toLowerCase().contains("factor")){
                    nodeRuleColumn = i;
                }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && headerCell.getStringCellValue().toLowerCase().contains("group")
                        && !headerCell.getStringCellValue().toLowerCase().contains("factor")){
                    groupRuleColumn = i;
                }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && headerCell.getStringCellValue().toLowerCase().contains("factor")){
                    factorRuleColumn = i;
                }
            }

            ArrayList<String> rollUpRules = new ArrayList<>();
            for(int i = 0; i < numOfRows; i++){
                XSSFRow row = dependenciesSheet.getRow(i + 1);
                XSSFCell nodeRuleCell = row.getCell(nodeRuleColumn);
                rollUpRules.add(nodeRuleCell.getStringCellValue());

                if(groupRuleColumn != -1){
                    XSSFCell groupRuleCell = row.getCell(groupRuleColumn);
                    rollUpRules.add(groupRuleCell.getStringCellValue());
                }

                if(factorRuleColumn != -1){
                    XSSFCell factorRuleCell = row.getCell(factorRuleColumn);
                    rollUpRules.add(factorRuleCell.getStringCellValue());
                }
            }
            return rollUpRules;
        }else{
            return null;
        }
    }

    protected boolean readAnalysisSpecificDependencies(){

        if(iConfig.containsAnalysis(AnalysesForm.CRITICALITY)){
            boolean success = readCriticalityDependencies(network);
            if(success){
                CoreLogger.logger.log(Level.INFO, "Successfully read the 'Dependencies' sheet");
            }else{
                app.GAE("Failed to read the 'Dependencies' sheet", true);
                return true;
            }
        }

        if(iConfig.containsAnalysis(AnalysesForm.DYNAMIC_OPERABILITY)){
            boolean success = readOperabilityDependencies(network);
            if(!success) {
                app.GAE("Failed to read the Dependencies sheet", true);
                return true;
            }
        }

        if(iConfig.containsAnalysis(AnalysesForm.CG)){
            readCGScaling(); //must be first
            readCGGrouping();
        }

        return false;
    }

    protected  void readFactors(){
        XSSFSheet factorsSheet = workbook.getSheet("Factors");

        XSSFRow headerRow = factorsSheet.getRow(0);
        int fidColumn = -1;
        int gidColumn = -1;
        int nidColumn = -1;
        int fivColumn = -1;
        int binaryColumn = -1;
        int nameColumn = -1;

        int headerRowSize = headerRow.getLastCellNum();
        for(int i = 0; i < headerRowSize; i++){
            XSSFCell headerCell = headerRow.getCell(i);
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

        int numOfRows = factorsSheet.getLastRowNum();
        HashMap<Integer, Integer> gIdToNId = new HashMap<>();
        for(int i = 0; i < numOfRows; i++){
            XSSFRow iRow = factorsSheet.getRow(i + 1);
            XSSFCell fIdCell = iRow.getCell(fidColumn);
            XSSFCell nIdCell = iRow.getCell(nidColumn);
            XSSFCell fivCell = iRow.getCell(fivColumn);
            XSSFCell binCell = iRow.getCell(binaryColumn);
            XSSFCell nameCell = iRow.getCell(nameColumn);

            XSSFCell gIdCell = null;
            if(gidColumn != -1){
                gIdCell = iRow.getCell(gidColumn);
            }

            Activity n = (Activity) network.getNode(nIdCell.getNumericCellValue());
            Factor f;

            f = network.createFactor(nameCell.toString(), (int) (fIdCell.getNumericCellValue() + 6000), (int) fIdCell.getNumericCellValue(), true);
            f.mathematicaColor = "LightGray";
            network.addNode(f);
            boolean bin = binCell.getNumericCellValue() == 1;
            if(gIdCell == null){
                network.linkFactorToNode(f, n, fivCell.getNumericCellValue(), bin, null);
            }else{
                int gId = ((int) gIdCell.getNumericCellValue());
                Group group;
                if(!gIdToNId.containsKey(gId)){
                    int nId = gId + 7000;
                    gIdToNId.put(gId, nId);
                    group = new Group("FG-" + gId);
                    group.id = nId;
                    group.decorativeID = gId;
                    group.tag = "factors";
                    network.linkNodes(group, n, null);
                }else{
                    double nId = gIdToNId.get(gId);
                    group = (Group) network.getNode(nId);
                }
                network.linkFactorToNode(f, group, fivCell.getNumericCellValue(), bin, null);
                group.nodes.add(f);
            }
        }
    }

    protected void readGlobalDependencies(){
        dependenciesSheet = getDependenciesSheet();
        if(dependenciesSheet != null){
            int numOfRows = dependenciesSheet.getLastRowNum();

            XSSFRow headerRow = dependenciesSheet.getRow(0);
            if(headerRow != null){
                int removeColumn = -1;
                int seColumn = -1;
                //ArrayList<Integer> factorColumnsI = new ArrayList<>();

                boolean usingNodeName = true;
                int headerRowSize = headerRow.getLastCellNum();
                for(int i = 0; i < headerRowSize; i++){
                    XSSFCell headerCell = headerRow.getCell(i);
                    String lHeaderCell = headerCell.getStringCellValue().toLowerCase();
                    if(lHeaderCell.contains("remove")){
                        removeColumn = i;
                    }
                    /*
                    if(lHeaderCell.contains("factor")){
                        factorColumnsI.add(i);
                    }
                     */
                    if(lHeaderCell.contains("se") && !lHeaderCell.contains("time")){
                        seColumn = i;
                    }
                    if(i == 0 && lHeaderCell.contains("id")){
                        usingNodeName = false;
                    }
                    if(i == 0 &&lHeaderCell.contains("name")){
                        usingNodeName = true;
                    }
                }

                /*
                if(doOdinn){
                    for(Integer i : factorColumnsI){
                        XSSFCell factorNameC = headerRow.getCell(i);
                        String factorName = factorNameC.getStringCellValue();
                        char[] factorLetter = factorName.toCharArray();
                        int start = 0;
                        int end = factorLetter.length - 1;
                        for(int a = 0; a < factorLetter.length; a++){
                            if(factorLetter[a] == '('){
                                start = a + 1;
                            }else if(factorLetter[a] == ')'){
                                end = a;
                            }
                        }
                        double health = 100;
                        try{
                            health = Double.parseDouble(factorName.substring(start, end));
                        }catch (Exception ignored){}
                        double id = Math.random();
                        Factor factor = network.createFactor(factorName, id, id, true);
                        factor.health = health;
                        network.addNode(factor);
                    }
                }
                 */

                for(int i = 0; i < numOfRows; i++){
                    XSSFRow row  = dependenciesSheet.getRow(i + 1);
                    XSSFCell nodeCell = row.getCell(0);
                    Activity activity = null;

                    for(Activity n : network.getActivities()){
                        if((usingNodeName && n.name.equals(nodeCell.toString())) || (!usingNodeName && n.id == nodeCell.getNumericCellValue())){
                            activity = n;
                            break;
                        }
                    }
                    if(activity != null){
                        double a = -1;
                        if(removeColumn != -1){
                            XSSFCell removeCell = row.getCell(removeColumn);
                            if(removeCell != null){
                                try {
                                    a = removeCell.getNumericCellValue();
                                }catch (Exception ignored){}
                            }
                        }

                        if(activity != network.startActivity && activity != network.goalActivity && a == 1){
                            activity.isReal = false;
                            activity.mathematicaColor = "Gray";
                        }

                        double se = Double.NaN;
                        if(seColumn != -1){
                            XSSFCell seCell = row.getCell(seColumn);
                            if(seCell != null && !isCellEmpty(seCell)){
                                try {
                                    se = seCell.getNumericCellValue();
                                }catch (Exception ignored){}
                            }
                        }

                        if(!Double.isNaN(se)){
                            activity.staticSE = se;
                        }

                        /*
                        if(doOdinn){
                            for(int j = 0; j < factorColumnsI.size(); j++){
                                XSSFCell odinnValueC = row.getCell(factorColumnsI.get(j));
                                Factor factor = network.getFactors().get(j);
                                if(odinnValueC != null){
                                    boolean binary = false;
                                    double value = 0.0;
                                    if(odinnValueC.getCellType() == CellType.STRING){
                                        String odinn = odinnValueC.getStringCellValue();
                                        if(odinn.contains("b")){
                                            binary = true;
                                            value = Double.parseDouble(odinn.substring(1));
                                        }
                                    }else{
                                        value =odinnValueC.getNumericCellValue();
                                    }
                                    network.linkFactorToActivity(factor, activity, value, binary, "");
                                }
                            }
                        }
                        */

                    }
                }
            }
        }
    }

    //reads the dependencies sheet
    protected boolean readCriticalityDependencies(Network network){
        dependenciesSheet = getDependenciesSheet();
        if(dependenciesSheet != null){
            int numOfRows = dependenciesSheet.getLastRowNum();

            XSSFRow headerRow = dependenciesSheet.getRow(0);

            int aColumn = -1;
            int bColumn = -1;
            boolean usingNodeNames = true;
            int headerRowSize = headerRow.getLastCellNum();
            for(int i = 0; i < headerRowSize; i++){
                XSSFCell headerCell = headerRow.getCell(i);
                switch (headerCell.getStringCellValue().toLowerCase()) {
                    case "a" -> aColumn = i;
                    case "b" -> bColumn = i;
                }
                if(i == 0 && headerCell.getStringCellValue().toLowerCase().contains("id")){
                    usingNodeNames = false;
                }
            }

            for(int i = 0; i < numOfRows; i++){
                XSSFRow row  = dependenciesSheet.getRow(i + 1);
                XSSFCell nodeCell = row.getCell(0);
                XSSFCell aCell = null;
                if(aColumn != -1){
                    aCell = row.getCell(aColumn);
                }
                XSSFCell bCell = null;
                if(bColumn != -1){
                    bCell = row.getCell(bColumn);
                }

                double a = 1;
                double b = -.5;
                if(aCell != null){
                    a = aCell.getNumericCellValue();
                }
                if(bCell != null){
                    b = bCell.getNumericCellValue();
                }

                Activity n = null;
                for(Activity activity : network.getActivities()){
                    if((usingNodeNames && activity.name.equals(nodeCell.toString())) || (!usingNodeNames && activity.id == nodeCell.getNumericCellValue())){
                        n = activity;
                        break;
                    }
                }
                if(n != null){
                    n.A = a;
                    n.B = b;
                }
            }
        }

        return true;
    }

    protected boolean readOperabilityDependencies(Network network){
        dependenciesSheet = getDependenciesSheet();
        int numOfRows = dependenciesSheet.getLastRowNum();
        timeColumns = new ArrayList<>();
        seTimeColumns = new ArrayList<>();

        XSSFRow headerRow = dependenciesSheet.getRow(0);
        int headerRowSize = headerRow.getLastCellNum();
        boolean usingNodeNames = true;

        for(int i = 0; i < headerRowSize; i++){
            XSSFCell headerCell = headerRow.getCell(i);
            if(headerCell.getStringCellValue().toLowerCase().contains("time") && !headerCell.getStringCellValue().toLowerCase(Locale.ROOT).contains("se")){
                timeColumns.add(i);
                numTimes++;
            }else if(headerCell.getStringCellValue().toLowerCase(Locale.ROOT).equals("se")){
                seTimeColumns.add(i);
            }else if(i == 0 && headerCell.getStringCellValue().toLowerCase().contains("id")){
                usingNodeNames = false;
            }
        }

        for(Activity activity : network.getActivities()){
            for(NodeAnalysisDataHolder result : activity.analysisDataHolders){
                for(Analysis analysis : result.targetAnalyses){
                    if (analysis.formEnum == AnalysesForm.DYNAMIC_OPERABILITY) {
                        result.operabilityPerTime = new double[numTimes];
                        Arrays.fill(result.operabilityPerTime, Double.NaN);
                        result.sePerTime = new double[numTimes];
                        Arrays.fill(result.sePerTime, Double.NaN);
                        break;
                    }
                }
            }
        }

        for(int i = 0; i < numOfRows; i++){
            XSSFRow row = dependenciesSheet.getRow(i + 1);
            XSSFCell nodeCell = row.getCell(0);

            Activity n = null;
            for(Activity activity : network.getActivities()){
                if((usingNodeNames && activity.name.equals(nodeCell.toString())) || (!usingNodeNames && activity.id == nodeCell.getNumericCellValue())){
                    n = activity;
                    break;
                }
            }

            if(n != null){
                for (int a = 0; a < timeColumns.size(); a++) {
                    XSSFCell cell = row.getCell(timeColumns.get(a));
                    for(NodeAnalysisDataHolder result : n.analysisDataHolders){
                        for(Analysis analysis : result.targetAnalyses){
                            if(analysis.formEnum == AnalysesForm.DYNAMIC_OPERABILITY){
                                if (!isCellEmpty(cell)) {
                                    double operability = cell.getNumericCellValue();
                                    result.operabilityPerTime[a] = operability;
                                }
                            }
                        }

                    }
                }
                for(int a = 0; a < seTimeColumns.size(); a++){
                    XSSFCell cell = row.getCell(seTimeColumns.get(a));
                    for(NodeAnalysisDataHolder result : n.analysisDataHolders){
                        for(Analysis analysis : result.targetAnalyses){
                            if(analysis.formEnum == AnalysesForm.DYNAMIC_OPERABILITY){
                                if (!isCellEmpty(cell)) {
                                    double se = cell.getNumericCellValue();
                                    result.sePerTime[a] = se;
                                }
                            }
                        }

                    }
                }

            }
        }
        return true;
    }

    protected void readNodeSpecificRollUpRules(){
        dependenciesSheet = getDependenciesSheet();
        if(dependenciesSheet != null){
            int numOfRows = dependenciesSheet.getLastRowNum();

            XSSFRow headerRow = dependenciesSheet.getRow(0);
            int nodeRuleColumn = -1;
            int factorRuleColumn = -1;
            int groupRuleColumn = -1;
            int headerRowSize = headerRow.getLastCellNum();
            boolean usingNodeNames = true;

            for(int i = 0; i < headerRowSize; i++){
                XSSFCell headerCell = headerRow.getCell(i);
                if(i == 0 && headerCell.getStringCellValue().toLowerCase().contains("id")){
                    usingNodeNames = false;
                }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && (headerCell.getStringCellValue().toLowerCase().contains("node") || headerCell.getStringCellValue().toLowerCase().contains("activity"))
                        && !headerCell.getStringCellValue().toLowerCase().contains("group") && !headerCell.getStringCellValue().toLowerCase().contains("factor")){
                    nodeRuleColumn = i;
                }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && headerCell.getStringCellValue().toLowerCase().contains("group")
                        && !headerCell.getStringCellValue().toLowerCase().contains("factor")){
                    groupRuleColumn = i;
                }else if(headerCell.getStringCellValue().toLowerCase().contains("rule") && headerCell.getStringCellValue().toLowerCase().contains("factor")){
                    factorRuleColumn = i;
                }
            }

            for(int i = 0; i < numOfRows; i++){
                XSSFRow row = dependenciesSheet.getRow(i + 1);
                XSSFCell nodeCell = row.getCell(0);

                RollUpEnum nodeRule = null;
                XSSFCell nodeRuleCell = row.getCell(nodeRuleColumn);
                if(nodeRuleCell != null){
                    nodeRule = switch (nodeRuleCell.getStringCellValue().toLowerCase()) {
                        case "and" -> RollUpEnum.AND;
                        case "fdna" -> RollUpEnum.FDNA;
                        case "fdna-or" -> RollUpEnum.FDNA_OR;
                        case "fdna2" -> RollUpEnum.FDNA2;
                        case "odinn-fti", "soda-fti" -> RollUpEnum.ODINN_FTI;
                        case "odinn", "soda" -> RollUpEnum.ODINN;
                        case "average", "avg" -> RollUpEnum.AVERAGE;
                        case "threshold" -> RollUpEnum.THRESHOLD;
                        default -> RollUpEnum.OR;
                    };
                }

                RollUpEnum groupRule = null;
                if(groupRuleColumn != -1) {
                    XSSFCell groupRuleCell = row.getCell(groupRuleColumn);
                    if(groupRuleCell != null){
                        groupRule = switch (groupRuleCell.getStringCellValue().toLowerCase()) {
                            case "and" -> RollUpEnum.AND;
                            case "average", "avg" -> RollUpEnum.AVERAGE;
                            case "threshold" -> RollUpEnum.THRESHOLD;
                            default -> RollUpEnum.OR;
                        };
                    }
                }

                RollUpEnum factorRule = null;
                if(factorRuleColumn != -1) {
                    XSSFCell factorRuleCell = row.getCell(factorRuleColumn);
                    if(factorRuleCell != null){
                        factorRule = switch (factorRuleCell.getStringCellValue().toLowerCase()) {
                            case "and" -> RollUpEnum.AND;
                            case "average", "avg" -> RollUpEnum.AVERAGE;
                            case "threshold" -> RollUpEnum.THRESHOLD;
                            default -> RollUpEnum.OR;
                        };
                    }
                }

                if(nodeRule != null){
                    Activity n = null;
                    for(Activity activity : network.getActivities()){
                        if((usingNodeNames && activity.name.equals(nodeCell.toString())) || (!usingNodeNames && activity.id == nodeCell.getNumericCellValue())){
                            n = activity;
                            break;
                        }
                    }
                    if(n != null){
                        n.customActivityRule = nodeRule;
                        n.customGroupRule = groupRule;
                        n.customFactorRule = factorRule;
                    }
                }
            }
        }
    }

    protected  void readAttackTree(){
        AttackAnalysis analysis = (AttackAnalysis) iConfig.getAnalysis(AnalysesForm.ATTACK);
        this.decisionTree = new DecisionTree(analysis.USE_BUDGET, analysis.BUDGET);

        XSSFSheet decisionSheet = workbook.getSheet("Decisions");
        if(decisionSheet != null){
            int numOfRow = decisionSheet.getLastRowNum();
            for(int i = 0; i < numOfRow; i++){
                XSSFRow iRow = decisionSheet.getRow(i + 1);
                XSSFCell idCell = iRow.getCell(0);
                XSSFCell requirementCell = iRow.getCell(1);
                XSSFCell costCell = iRow.getCell(2);
                XSSFCell descriptionCell = iRow.getCell(3);

                int id = (int) idCell.getNumericCellValue();
                String requirement = requirementCell != null ? requirementCell.toString() : "";
                double cost = costCell.getNumericCellValue();
                String description = descriptionCell !=  null ? descriptionCell.toString() : "";

                decisionTree.addDecision(new Decision(id, requirement, cost, description));
            }
        }

        XSSFSheet routesSheet = workbook.getSheet("Routes");
        if(routesSheet != null){
            int numOfRows = routesSheet.getLastRowNum();
            for(int i = 0; i < numOfRows; i++){
                XSSFRow iRow = routesSheet.getRow(i + 1);
                XSSFCell decisionIdCell = iRow.getCell(0);
                XSSFCell routeIdCell = iRow.getCell(1);
                XSSFCell probSuccessCell = iRow.getCell(2);
                XSSFCell nodeNameCell = iRow.getCell(3);
                XSSFCell operabilityCell = iRow.getCell(4);
                XSSFCell commentCell = iRow.getCell(5);

                int decisionId = (int) decisionIdCell.getNumericCellValue();
                int routeId = (int) routeIdCell.getNumericCellValue();
                double probSuccess = probSuccessCell.getNumericCellValue();
                String nodeName = nodeNameCell != null ? nodeNameCell.toString() : "";
                double operability = operabilityCell != null ? operabilityCell.getNumericCellValue() : -1;
                String comment = commentCell != null ? commentCell.toString() : "";

                Node node = null;
                if (!nodeName.isBlank() && !nodeName.isEmpty() && operability != -1) {
                    node = network.getNode(nodeName);
                }
                decisionTree.addRoute(new Route(decisionId, routeId, probSuccess, node, operability, comment));
            }
        }

        System.out.println("Testing at StandardInput::readAttackTree()");
    }

    private XSSFSheet getDependenciesSheet(){
        XSSFSheet dependenciesSheet = workbook.getSheet("Dependencies");
        if (dependenciesSheet == null) {
            dependenciesSheet = workbook.getSheet("Activities");
            if (dependenciesSheet == null) {
                dependenciesSheet = workbook.getSheet("Nodes");
            }
        }
        return dependenciesSheet;
    }

    //TODO add error catching
    protected void readCGGrouping(){
        groupingSheet = workbook.getSheet("Grouping");
        if(groupingSheet != null){
            for(int i = 0; i < groupingSheet.getLastRowNum() + 1; i++){
                XSSFRow row = groupingSheet.getRow(i);
                if(row != null){
                    XSSFCell groupingCell = row.getCell(0);
                    Group group = new Group(groupingCell.getStringCellValue());
                    group.id = network.getRandomUnusedId();
                    group.tag = "cg-custom";
                    for(int a = 0 ; a < row.getLastCellNum() - 1; a++){
                        XSSFCell cell = row.getCell(a + 1);
                        if(cell != null){
                            String nodeName = cell.toString();
                            for(Activity activity : network.getActivities()){
                                if(nodeName.equals(activity.name)){
                                    group.nodes.add(activity);
                                }
                            }
                        }
                    }
                    network.addNode(group);
                }
            }
        }
    }

    //TODO add error catching
    protected void readCGScaling(){
        scaleSheet = workbook.getSheet("Scale");
        if(scaleSheet != null){
            for(int i = 0; i < scaleSheet.getLastRowNum(); i++){
                XSSFRow row = scaleSheet.getRow(i + 1);
                if(row != null){
                    XSSFCell numberCell = row.getCell(0);
                    XSSFCell colorCell = row.getCell(1);

                    try{
                        cGDivisionNumbers.add((int) numberCell.getNumericCellValue());
                    }catch (Exception a){
                        a.printStackTrace();
                    }

                    if(isCellEmpty(colorCell)){
                        byte[] rgb = colorCell.getCellStyle().getFillForegroundXSSFColor().getRGB();
                        int[] rgbInt = new int[rgb.length];
                        for(int a = 0; a < rgb.length; a++) {
                            rgbInt[a] = Byte.toUnsignedInt(rgb[a]);
                        }
                        cGDivisionColors.add(rgbInt);
                    }else{
                        try{
                            int first = -1; int second = -1;
                            String rgb = colorCell.getStringCellValue();
                            for(int a = 0; a < rgb.length(); a++){
                                if(rgb.charAt(a) == ','){
                                    if(first == -1){
                                        first = a;
                                    }else{
                                        second = a;
                                    }
                                }
                            }
                            int red = Integer.parseInt(rgb.substring(0, first));
                            int green = Integer.parseInt(rgb.substring(first + 1, second));
                            int blue = Integer.parseInt(rgb.substring(second + 1));
                            int[] color = new int[3];
                            color[0] = red; color[1] = green; color[2] = blue;
                            cGDivisionColors.add(color);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }else{
            addCGDefaults();
        }
    }

    public void addCGDefaults(){
        if(iConfig.containsAnalysis(AnalysesForm.CG)){
            CGAnalysis cgAnalysis = (CGAnalysis) iConfig.getAnalysis(AnalysesForm.CG);
            cGDivisionNumbers.addAll(Arrays.asList(10, 30, 50, 70, 90, 100));
            if(cgAnalysis.colorSet == ColorSet.COLOR_SET1){
                cGDivisionColors.addAll(List.of(
                        new int[]{204, 0,   204},
                        new int[]{255, 0,   0  },
                        new int[]{255, 178, 102},
                        new int[]{255, 255, 51 },
                        new int[]{102, 255, 102},
                        new int[]{0,   204, 0  }
                ));
            }else if(cgAnalysis.colorSet == ColorSet.COLOR_SET2){
                cGDivisionColors.addAll(List.of(
                        new int[]{255, 0,   0  },
                        new int[]{255, 128, 0  },
                        new int[]{255, 255, 0  },
                        new int[]{128, 255, 0  },
                        new int[]{0,   255, 0  },
                        new int[]{0,   255, 128}
                ));
            }
        }
    }

    //Increment values from each thread
    public synchronized void synchronizeThreads(Network threadNetwork){
        for(int queue = 0; queue < iConfig.inputQueues.size(); queue++){
            if(iConfig.inputQueues.get(queue).containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                CriticalityAnalysis analysis = (CriticalityAnalysis) iConfig.inputQueues.get(queue).getAnalysis(AnalysesForm.CRITICALITY);
                for(int trial = 0; trial < analysis.trials; trial++){
                    network.analysisDataHolders.get(queue).networkCriticalityTrials[trial].possibilitiesCount = network.analysisDataHolders.get(queue).networkCriticalityTrials[trial].possibilitiesCount.add(threadNetwork.analysisDataHolders.get(queue).networkCriticalityTrials[trial].possibilitiesCount);
                    network.analysisDataHolders.get(queue).networkCriticalityTrials[trial].phiSum += threadNetwork.analysisDataHolders.get(queue).networkCriticalityTrials[trial].phiSum;
                    network.analysisDataHolders.get(queue).networkCriticalityTrials[trial].phiSumDivisionCounts += threadNetwork.analysisDataHolders.get(queue).networkCriticalityTrials[trial].phiSumDivisionCounts;
                    //network.analysisDataHolders.get(queue).networkCriticalityTrials[trial].E = network.analysisDataHolders.get(queue).networkCriticalityTrials[trial].E.plus(threadNetwork.analysisDataHolders.get(queue).networkCriticalityTrials[trial].E);
                }
            }
        }

        for(int i = 0; i < network.getNodes().size(); i++){
            for(int queue = 0; queue < iConfig.inputQueues.size(); queue++){
                if(iConfig.inputQueues.get(queue).containsTargetAnalysis(AnalysesForm.CRITICALITY)){
                    CriticalityAnalysis analysis = (CriticalityAnalysis) iConfig.inputQueues.get(queue).getAnalysis(AnalysesForm.CRITICALITY);
                    for(int trial = 0; trial < analysis.trials; trial++){
                        network.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].sumOfGoalWhenON += threadNetwork.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].sumOfGoalWhenON;
                        network.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].sumOfGoalWhenOFF += threadNetwork.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].sumOfGoalWhenOFF;
                        network.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].divisionCount += threadNetwork.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].divisionCount;

                        network.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].onCount += threadNetwork.getNodes().get(i).analysisDataHolders.get(queue).nodeCriticalityTrials[trial].onCount;
                    }
                }
            }
        }

        if(!network.connectsToGoal){
            network.connectsToGoal = threadNetwork.connectsToGoal;
        }
    }

    public synchronized void updateLiveCount(){
        livePossibilitiesCount =  livePossibilitiesCount.add(BigInteger.ONE);
    }

    protected String removeTrailingZeros(String name){
        if(name.charAt(name.length() - 1) == '0' && name.charAt(name.length() - 2) == '.'){
            return name.substring(0, name.length() - 2);
        }
        return name;
    }

    //convert any number to its excel alphabetic equivalent
    public String integerToAlphabetic(int i) {
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

}
