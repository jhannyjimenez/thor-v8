package org.mitre.thor.gui;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.controlsfx.control.CheckComboBox;
import org.mitre.thor.analyses.*;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.App;
import org.mitre.thor.Process;
import org.mitre.thor.analyses.cg.ColorSet;
import org.mitre.thor.analyses.grouping.Grouping;
import org.mitre.thor.analyses.target.TargetType;
import org.mitre.thor.analyses.cg.CGOrderingMethod;
import org.mitre.thor.network.attack.DecisionOption;
import org.mitre.thor.output.OutputForm;
import org.mitre.transitions.FxTransitions;
import org.mitre.thor.input.*;
import org.mitre.thor.analyses.crit.PhiCalculationEnum;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Class that controls the GUI. This is where all of the GUI logic is placed.
 */
public class Controller {

    //FXML objects
    //Panes
    public AnchorPane homePane;
    public AnchorPane loadingPane;
    public AnchorPane questionPane;
    public AnchorPane analysisTypePane;
    public AnchorPane criticalityPane;
    public AnchorPane operabilityPane;
    public AnchorPane coolGraphPane;
    public AnchorPane attackPane;
    public ScrollPane consolePane;
    public ScrollPane processesPane;

    //Buttons
    public Button homeImportButton;
    public Button homeConsoleButton;
    public Button consoleHomeButton;
    public Button loadingHomeButton;
    public Button loadingStopButton;
    public Button analysisContinueButton;
    public Button criticalityRunButton;
    public Button operabilityRunButton;
    public Button coolGraphRunButton;
    public Button attackRunButton;
    public Button consoleBackButton;
    public Button exitButton;
    public Button consoleOpenFileButton;
    public Button processesRunningButton;
    public Button processesHomeButton;
    public Button processesBackButton;

    public Button analysisCancelButton;
    public Button criticalityCancelButton;
    public Button criticalityBackButton;
    public Button operabilityCancelButton;
    public Button operabilityBackButton;
    public Button coolGraphCancelButton;
    public Button coolGraphBackButton;
    public Button attackCancelButton;
    public Button attackBackButton;

    public Button questionYesButton;
    public Button questionNoButton;

    //ChoiceBox
    public CheckComboBox<AnalysesForm> analysisChoiceBox;
    public ChoiceBox<PhiCalculationEnum> criticalityCalculationMethodChoiceBox;
    public CheckComboBox<RollUpEnum> criticalityRollUpRuleChoiceBox;
    public ChoiceBox<RollUpEnum> operabilityRollUpRuleChoiceBox;
    public CheckComboBox<RollUpEnum> coolGraphRollUpRuleChoiceBox;
    public CheckComboBox<RollUpEnum> attackRollUpRuleChoiceBox;
    public ChoiceBox<CGOrderingMethod> coolGraphOrderChoiceBox;
    public ChoiceBox<TargetType> criticalityTargetChoiceBox;
    public ChoiceBox<TargetType> cgTargetChoiceBox;
    public ChoiceBox<Grouping> cgGroupingChoiceBox;
    public ChoiceBox<Grouping> criticalityGroupingChoiceBox;
    public ChoiceBox<ColorSet> cgColorChoiceBox;
    public ChoiceBox<DecisionOption> attackDecisionTreeChoiceBox;

    //Text
    public Text versionText;
    public Text loadingPossibilitiesText;
    public Text loadingMinutesText;
    public Label questionLabel;

    //Text Fields
    public TextField criticalityMaxMinutesTextField;
    public TextField criticalityThreadsTextField;
    public TextField criticalityTrialsTextField;
    public TextField attackBudgetTextField;
    public TextField attackPointsTextField;

    //Other
    public ListView<String> consoleListView;
    public ListView<Integer> processesListView;
    public ProgressBar loadingProgressBar;
    public CheckBox strengthCheckBox;
    public CheckBox liteCheckBox;
    public CheckBox coolGraphSaveGraphCheckBox;
    public CheckBox attackBudgetCheckBox;
    public CheckBox attackIncludeTextStopCheckBox;
    public CheckBox attackIncludeMathStopCheckBox;

    //Class Objects
    public boolean askingQuestion = false;
    public boolean questionAnswer = false;
    public final Object MONITOR = new Object();

    public App app;
    Process currentProcess;
    ArrayList<Process> processes = new ArrayList<>();
    ArrayList<Analysis> analyses = new ArrayList<>();
    public ArrayList<Node> transitionHistory = new ArrayList<>();
    public Stage primStage;
    public Node currentPane;
    public FileChooser fileChooser = new FileChooser();
    public File currentFile = null;
    public double animationTime = .3;
    public InputForm inputType;
    public ArrayList<AnalysesForm> analysesQ = new ArrayList<>();

    public Controller(App app, Stage primStage){
        this.app = app;
        this.primStage = primStage;
    }

    /**
     * Sets up the initial logic of the GUI by adding button actions, filling in checkboxes, and setting up Lists.
     */
    @FXML
    public void initialize(){
        currentPane = homePane;

        //set the event handlers for each button
        exitButton.setOnAction(this::exitButtonOnClicked);
        analysisContinueButton.setOnAction(this::analysisContinueButtonOnClicked);
        loadingHomeButton.setOnAction(this::loadingHomeButtonOnClicked);
        loadingStopButton.setOnAction(this::loadingStopButtonOnAction);
        homeImportButton.setOnAction(this::homeImportButtonOnClicked);
        criticalityRunButton.setOnAction(this::criticalityRunButtonOnClicked);
        criticalityCalculationMethodChoiceBox.setOnAction(this::criticalityCalcMethodOnAction);
        operabilityRunButton.setOnAction(this::operabilityRunButtonOnClicked);
        coolGraphRunButton.setOnAction(this::coolGraphRunButtonOnClicked);
        attackRunButton.setOnAction(this::attackRunButtonOnClicked);
        homeConsoleButton.setOnAction(this::homeConsoleButtonOnClicked);
        consoleHomeButton.setOnAction(this::homeButtonOnClicked);
        processesHomeButton.setOnAction(this::homeButtonOnClicked);
        consoleBackButton.setOnAction(this::backButtonOnClicked);
        processesBackButton.setOnAction(this::backButtonOnClicked);
        consoleOpenFileButton.setOnAction(this::consoleOpenFileButtonOnClicked);
        processesRunningButton.setOnAction(this::processRunningButtonOnAction);
        attackBudgetCheckBox.setOnAction(this::attackChoiceBoxOnAction);

        analysisCancelButton.setOnAction(this::cancelButtonOnClicked);
        criticalityCancelButton.setOnAction(this::cancelButtonOnClicked);
        criticalityBackButton.setOnAction(this::backButtonOnClicked);
        operabilityCancelButton.setOnAction(this::cancelButtonOnClicked);
        operabilityBackButton.setOnAction(this::backButtonOnClicked);
        coolGraphCancelButton.setOnAction(this::cancelButtonOnClicked);
        coolGraphBackButton.setOnAction(this::backButtonOnClicked);
        attackCancelButton.setOnAction(this::cancelButtonOnClicked);
        attackBackButton.setOnAction(this::backButtonOnClicked);

        questionYesButton.setOnAction(this::yesQuestionButtonOnAction);
        questionNoButton.setOnAction(this::noQuestionButtonOnAction);

        //fill the choices for each choice box
        for(AnalysesForm analyses : AnalysesForm.values()){
            analysisChoiceBox.getItems().add(analyses);
        }
        for(PhiCalculationEnum method : PhiCalculationEnum.values()){
            criticalityCalculationMethodChoiceBox.getItems().add(method);
        }
        for(RollUpEnum rule : RollUpEnum.values()){
            if(rule.isVisible){
                criticalityRollUpRuleChoiceBox.getItems().add(rule);
                operabilityRollUpRuleChoiceBox.getItems().add(rule);
                coolGraphRollUpRuleChoiceBox.getItems().add(rule);
                attackRollUpRuleChoiceBox.getItems().add(rule);
            }
        }
        for(CGOrderingMethod method : CGOrderingMethod.values()){
            coolGraphOrderChoiceBox.getItems().add(method);
        }
        for(TargetType type : TargetType.values()){
            criticalityTargetChoiceBox.getItems().add(type);
            cgTargetChoiceBox.getItems().add(type);
        }
        for(Grouping calcMethod : Grouping.values()){
            cgGroupingChoiceBox.getItems().add(calcMethod);
            criticalityGroupingChoiceBox.getItems().add(calcMethod);
        }
        for(ColorSet colorSet : ColorSet.values()){
            cgColorChoiceBox.getItems().add(colorSet);
        }
        for(DecisionOption option: DecisionOption.values()){
            attackDecisionTreeChoiceBox.getItems().add(option);
        }

        //customize the console list view so that whenever the first litter of an item is R the background
        //is red, and when the first letter is G the background is green
        consoleListView.setCellFactory(cell -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setTextFill(Paint.valueOf("#D4D4D3"));
                setFont(Font.font("Century Gothic", 18));

                if (item == null || empty) {
                    setText(null);
                    setStyle("-fx-background-color: #005B94");
                } else if (item.charAt(0) == 'R') {
                    item = item.replaceFirst("R", "");
                    setText(item);
                    setStyle("-fx-background-color: red");
                } else if (item.charAt(0) == 'G'){
                    item = item.replaceFirst("G", "");
                    setText(item);
                    setStyle("-fx-background-color: green");
                }else{
                    setText(item);
                    setStyle("-fx-background-color: #005B94");
                }
            }
        });

        processesListView.setCellFactory(cell -> new ProcessCell(this));

        addDefaultValues();

        setVersion();
    }

    private void setVersion() {
        try {
            String version = new String(Files.readAllBytes(Paths.get(Path.of("version.txt").toUri())));
            versionText.setText("V " + version);
        } catch (IOException ignored) {
            versionText.setText("NULL");
        }
    }

    /**
     * Selects default options for each choice so that the user does not see a blank choice box.
     */
    private void addDefaultValues(){
        //analyses
        analysisChoiceBox.getCheckModel().check(AnalysesForm.CRITICALITY);
        //criticality
        criticalityCalculationMethodChoiceBox.setValue(PhiCalculationEnum.RANDOM);
        criticalityRollUpRuleChoiceBox.getCheckModel().check(RollUpEnum.OR);
        criticalityMaxMinutesTextField.setText("5");
        criticalityThreadsTextField.setText("1");
        criticalityTrialsTextField.setText("1");
        criticalityTargetChoiceBox.setValue(TargetType.NODES);
        criticalityGroupingChoiceBox.setValue(Grouping.SINGLE);
        //operability
        operabilityRollUpRuleChoiceBox.setValue(RollUpEnum.FDNA);
        //CG
        coolGraphRollUpRuleChoiceBox.getCheckModel().check(RollUpEnum.FDNA);
        coolGraphOrderChoiceBox.setValue(CGOrderingMethod.DESCENDING);
        cgTargetChoiceBox.setValue(TargetType.NODES);
        cgGroupingChoiceBox.setValue(Grouping.SINGLE);
        cgColorChoiceBox.setValue(ColorSet.COLOR_SET1);
        attackRollUpRuleChoiceBox.getCheckModel().check(RollUpEnum.ODINN_FTI);
        attackBudgetCheckBox.setSelected(false);
        attackDecisionTreeChoiceBox.setValue(DecisionOption.IMPACT);
        attackBudgetTextField.setText("180");
        attackBudgetTextField.setDisable(true);
        attackPointsTextField.setText("100000");
        attackIncludeTextStopCheckBox.setSelected(true);
        attackIncludeMathStopCheckBox.setSelected(true);
    }

    /**
     * Opens the output file in Excel or the primary program in the user's computer that opens .xlsx files
     */
    public void openCurrentFile(){
        if(currentFile != null){
            try {
                Desktop.getDesktop().open(currentFile);
            } catch (IOException e) {
                app.GAE("Could not open the file at " + currentFile.getAbsolutePath(), true);
            }
        }else{
            app.GAE("Cannot open the output file because it is null", true);
        }
    }

    //freezes the thread that calls this function until the yes or no button are clicked
    public void askQuestion(String question) {
        if(!Platform.isFxApplicationThread()){
            askingQuestion = true;
            questionAnswer = false;

            Platform.runLater(() -> {
                setQuestionText(question);
                transitionToPane(questionPane);
            });

            synchronized (MONITOR){
                try {
                    MONITOR.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }else{
            CoreLogger.logger.warning("Cannot ask question from the JavaFx application thread");
        }
    }

    private void setQuestionText(String question){
        questionLabel.setText(question);
    }

    private void attackChoiceBoxOnAction(ActionEvent actionEvent) {
        attackBudgetTextField.setDisable(!attackBudgetCheckBox.isSelected());
    }

    //unfreeze the frozen thread and set the question answer to yes
    public void yesQuestionButtonOnAction(ActionEvent event) {
        questionAnswer = true;
        askingQuestion = false;

        synchronized (MONITOR) {
            MONITOR.notify();
        }
    }

    //unfreeze the frozen thread and set the question answer to no
    public void noQuestionButtonOnAction(ActionEvent event) {
        questionAnswer = false;
        askingQuestion = false;

        synchronized (MONITOR) {
            MONITOR.notify();
        }
    }

    /**
     * Update the loading screen with new number of possibilities and time left
     *
     * @param possString the number of possibilities calculated compared to the total possibilities as a string
     * @param minutesString the number of minutes left as a string
     * @param percentage the percentage of the calculation that is done, this is used for the loading bar
     */
    public void updateLoadingScreen(String possString, String minutesString, double percentage){
        //check if function is being called from the GUI thread
        Platform.runLater(() ->{
            String endString;
            if(possString.toCharArray().length > 40){
                endString = possString.substring(0, 40) + "...";
            }else{
                endString = possString;
            }
            loadingPossibilitiesText.setText(endString);
            loadingMinutesText.setText(minutesString);
            loadingProgressBar.setProgress(percentage);
        });
    }

    /**
     * Add a new item to the console list with a red background
     *
     * @param error the item to be added
     * @param transition determines whether the GUI will transition to the console so that the user can see the item
     */
    public void throwConsoleError(String error, boolean transition){
        //check if function is being called from the GUI thread
        Platform.runLater(() ->{
            consoleListView.getItems().add("R"+error+"\n");
            if(transition && currentPane != consolePane){
                transitionToPane(consolePane);
            }
        });
    }

    /**
     * add a new item to the console list with a green background
     *
     * @param status the item to be added
     * @param transition determines whether the GUI will transition to the console so that the user can see the item
     */
    public void throwConsoleStatus(String status, boolean transition){
        //check if function is being called from the GUI thread
        Platform.runLater(() -> {
            consoleListView.getItems().add("G"+status+"\n");
            if(transition && currentPane != consolePane){
                transitionToPane(consolePane);
            }
        });
    }

    /**
     * Clear the console list of any items
     */
    public void clearConsole(){
        consoleListView.getItems().clear();
    }

    /**
     * Open a file chooser, get a file, determine what type of input it is, and
     *
     * @param event the home button clicked event
     */
    public void homeImportButtonOnClicked(ActionEvent event){

        //open a file chooser
        currentFile = fileChooser.showOpenDialog(primStage);

        analyses.clear();
        analysesQ.clear();

        if(currentFile != null){
            CoreLogger.logger.log(Level.INFO, "File chosen at " + currentFile.getAbsolutePath());
            //find input type
            inputType = getInputType(currentFile.getAbsolutePath());
            clearConsole();
            if(inputType != null && (inputType == InputForm.STANDARD || inputType == InputForm.INCIDENCE)){
                transitionToPane(analysisTypePane);
            }else if(inputType != null && inputType == InputForm.TABLE){
                processTableInput();
            }else if(inputType == null){
                app.GAE("Could not determine the input type. Please make sure to rename your Excel sheets correctly.", true);
            }
        }else{
            app.GAE("The file chosen does not exist", true);
        }
    }

    /**
     * Exit the program with the status code 0
     *
     * @param event the exit button clicked event
     */
    public void exitButtonOnClicked(ActionEvent event){
        System.exit(0);
    }

    /**
     * Transition to the previous pane
     *
     * @param event the back button clicked event
     */
    public void backButtonOnClicked(ActionEvent event){
        while (transitionHistory.size() > 1 && transitionHistory.get(transitionHistory.size() - 1) == currentPane){
            transitionHistory.remove(transitionHistory.size() - 1);
        }
        transitionToPane(transitionHistory.get(transitionHistory.size() - 1));
        transitionHistory.remove(transitionHistory.size() - 1);
    }

    /**
     * Get the type of analysis and send the user to the appropriate next page based on their chosen analysis
     *
     * @param event analysis continue button clicked event
     */
    public void analysisContinueButtonOnClicked(ActionEvent event){
        ObservableList<AnalysesForm> analyses = analysisChoiceBox.getCheckModel().getCheckedItems();
        analysesQ.clear();
        analysesQ.addAll(analyses);
        if(!analyses.isEmpty() && currentFile.canRead() && currentFile.canWrite()) {
            runAnalysisQ();
        }
    }

    /**
     * Gather the data from the criticality pane. This includes calculation methods, roll up rules, color nodes by criticality,
     * minutes, threads, and trials numbers.
     *
     * @param event criticality run button clicked event
     */
    public void criticalityRunButtonOnClicked(ActionEvent event){

        boolean successGatherData = true;

        PhiCalculationEnum calculationMethod = null;
        if(criticalityCalculationMethodChoiceBox.getValue() != null){
            calculationMethod = criticalityCalculationMethodChoiceBox.getValue();
        }else{
            app.GAE("Please select a criticality calculation method", true);
            successGatherData = false;
        }

        ArrayList<RollUpEnum> rollUpRules = new ArrayList<>();
        if(!criticalityRollUpRuleChoiceBox.getCheckModel().getCheckedItems().isEmpty()){
            rollUpRules.addAll(criticalityRollUpRuleChoiceBox.getCheckModel().getCheckedItems());
        }else{
            app.GAE("Please select at least 1 criticality roll up rule", true);
            successGatherData = false;
        }

        double currentMaxMinutes = 0;
        if(!criticalityMaxMinutesTextField.isDisabled()){
            try {
                currentMaxMinutes = Double.parseDouble(criticalityMaxMinutesTextField.getText());
            }catch (Exception ignored){
                app.GAE("Please fill the criticality max minutes field with a number > 0", true);
                successGatherData = false;
            }
        }else{
            currentMaxMinutes = Double.MAX_VALUE;
        }

        double currentThreads = 0;
        try {
            currentThreads = (int) Double.parseDouble(criticalityThreadsTextField.getText());
        }catch (Exception ignored){
            app.GAE("Please fill the criticality threads field with a whole number > 0", true);
            successGatherData = false;
        }

        double currentTrials = 0;
        try{
            currentTrials = (int) Double.parseDouble(criticalityTrialsTextField.getText());
        }catch (Exception ignored){
            app.GAE("Please fill the criticality trials field with a whole number > 0", true);
            successGatherData = false;
        }

        TargetType targetType = null;
        if(criticalityTargetChoiceBox.getValue() != null){
            targetType = criticalityTargetChoiceBox.getValue();
        }else{
            app.GAE("Please select a Target Type", true);
            successGatherData = false;
        }

        Grouping grouping = null;
        if(criticalityGroupingChoiceBox.getValue() != null){
            grouping = criticalityGroupingChoiceBox.getValue();
        }else{
            app.GAE("Please select a Grouping Type", true);
            successGatherData = false;
        }

        if(successGatherData){
            CriticalityAnalysis analysis = new CriticalityAnalysis(calculationMethod, rollUpRules, grouping, currentMaxMinutes, (int) currentThreads, (int) currentTrials, strengthCheckBox.isSelected(), targetType);
            analyses.add(analysis);

            runAnalysisQ();
        }else{
            app.GAE("Please fill out all criticality fields before proceeding", true);
        }
    }

    /**
     * Disable the option to select a time limit if the use selects the all possibilities method
     *
     * @param event calculation method on select event
     */
    private void criticalityCalcMethodOnAction(ActionEvent event) {
        boolean disable = false;
        PhiCalculationEnum method = criticalityCalculationMethodChoiceBox.getValue();
        if(method == PhiCalculationEnum.ALL){
            criticalityMaxMinutesTextField.setText("");
            disable = true;
        }
        criticalityMaxMinutesTextField.setDisable(disable);
    }

    /**
     * Gather the data from the operability pane. This only includes a Singular roll up rule.
     *
     * @param event operability run button clicked event
     */
    public void operabilityRunButtonOnClicked(ActionEvent event){
        boolean successGatherData = true;

        RollUpEnum rollUpEnum = null;
        if(operabilityRollUpRuleChoiceBox.getValue() != null){
            rollUpEnum = operabilityRollUpRuleChoiceBox.getValue();
        }else{
            app.GAE("Please select an operability roll up rule", false);
            successGatherData = false;
        }

        if(successGatherData){
            ArrayList<RollUpEnum> rollUps = new ArrayList<>();
            rollUps.add(rollUpEnum);
            OperabilityAnalysis analysis = new OperabilityAnalysis(rollUps);
            analyses.add(analysis);
            runAnalysisQ();
        }else{
            app.GAE("Please fill out all operability fields before proceeding", true);
        }
    }

    private void attackRunButtonOnClicked(ActionEvent actionEvent) {
        boolean successGatherData = true;
        ArrayList<RollUpEnum> rollUpRules = new ArrayList<>();
        if(!attackRollUpRuleChoiceBox.getCheckModel().getCheckedItems().isEmpty()){
            rollUpRules.addAll(attackRollUpRuleChoiceBox.getCheckModel().getCheckedItems());
        }else{
            app.GAE("Please select at least 1 attack roll up rule", true);
            successGatherData = false;
        }
        if(attackDecisionTreeChoiceBox == null){
            app.GAE("Please select a Decision Tree Option", true);
            successGatherData = false;
        }
        double budget = 0.0;
        if(attackBudgetCheckBox.isSelected()){
            try{
                budget = Double.parseDouble(attackBudgetTextField.getText());
            }catch (Exception e){
                app.GAE("If using a budget, please enter a number in the budget text field", true);
                successGatherData = false;
            }
        }
        double maxMinutes = 0.0;
        try{
            maxMinutes = Double.parseDouble(attackPointsTextField.getText());
        }catch (Exception e){
            app.GAE("Please enter a number in the max points field", true);
            successGatherData = false;
        }
        if(successGatherData){
            AttackAnalysis analysis = new AttackAnalysis(rollUpRules, maxMinutes, attackBudgetCheckBox.isSelected(),
                    budget, attackDecisionTreeChoiceBox.getValue(), attackIncludeTextStopCheckBox.isSelected(),
                    attackIncludeMathStopCheckBox.isSelected());
            analyses.add(analysis);
            runAnalysisQ();
        }else{
            app.GAE("Please fill out all attack fields before proceeding", true);
        }
    }

    /**
     * Gather the data form the CG pane. This only includes roll up rules.
     *
     * @param event cg run button clicked event
     */
    public void coolGraphRunButtonOnClicked(ActionEvent event){
        boolean successGatherData = true;
        ArrayList<RollUpEnum> rollUpRules = new ArrayList<>();
        CGOrderingMethod orderingMethod = null;
        Grouping calcMethod = null;
        if(!coolGraphRollUpRuleChoiceBox.getCheckModel().getCheckedItems().isEmpty()){
            rollUpRules.addAll(coolGraphRollUpRuleChoiceBox.getCheckModel().getCheckedItems());
        }else{
            app.GAE("Please select at least 1 CG roll up rule", true);
            successGatherData = false;
        }
        if(coolGraphOrderChoiceBox.getValue() != null){
            orderingMethod = coolGraphOrderChoiceBox.getValue();
        }else{
            app.GAE("Please select an ordering method", true);
            successGatherData = false;
        }
        if(cgGroupingChoiceBox.getValue() != null){
            calcMethod = cgGroupingChoiceBox.getValue();
        }else{
            app.GAE("Please select a grouping method", true);
            successGatherData = false;
        }

        TargetType targetType = null;
        if(cgTargetChoiceBox.getValue() != null){
            targetType = cgTargetChoiceBox.getValue();
        }else{
            app.GAE("Please select a Target Type", true);
            successGatherData = false;
        }

        ColorSet colorSet = null;
        if(cgColorChoiceBox.getValue() != null){
            colorSet = cgColorChoiceBox.getValue();
        }else{
            app.GAE("Please select a Color Set", true);
            successGatherData = false;
        }

        if(successGatherData){
            CGAnalysis analysis = new CGAnalysis(rollUpRules, orderingMethod, calcMethod, coolGraphSaveGraphCheckBox.isSelected(), colorSet, targetType);
            analyses.add(analysis);
            runAnalysisQ();
        }else{
            app.GAE("Please fill out all CG fields before proceeding", true);
        }
    }

    public void loadingHomeButtonOnClicked(ActionEvent event){
        transitionToPane(homePane);
    }

    /**
     * Disable the stop button, and tell the current process to force stop
     *
     * @param event loading stop button clicked event
     */
    private void loadingStopButtonOnAction(ActionEvent event) {
        if(currentProcess != null){
            currentProcess.input.phiForceStop = true;
        }
    }

    public void homeConsoleButtonOnClicked(ActionEvent event){
        transitionToPane(consolePane);
    }

    public void homeButtonOnClicked(ActionEvent event){
        transitionToPane(homePane);
    }

    public void cancelButtonOnClicked(ActionEvent event){transitionToPane(homePane);}

    public void consoleOpenFileButtonOnClicked(ActionEvent event){
        openCurrentFile();
    }

    /**
     * Transition to the process pane which contains a list of all currently running processes.
     * 
     * @param event process button on clicked
     */
    private void processRunningButtonOnAction(ActionEvent event) {
        if(currentPane != processesPane){
            transitionToPane(processesPane);
        }
    }

    /**
     * Transition to the corresponding pane of each analysis based on the analysisQ list until its empty, then run the core
     * of the program.
     */
    public void runAnalysisQ(){
        if(analysesQ.isEmpty()){
            runProgram();
        }else{
            AnalysesForm analysisForm = analysesQ.get(analysesQ.size() - 1);
            if(liteCheckBox.isSelected()){
                switch (analysisForm){
                    case CRITICALITY -> {
                        ArrayList<RollUpEnum> rules = new ArrayList<>();
                        rules.add(RollUpEnum.OR);
                        CriticalityAnalysis analysis = new CriticalityAnalysis(PhiCalculationEnum.RANDOM, rules, Grouping.SINGLE, 5, 1, 1, true, TargetType.NODES);
                        analyses.add(analysis);
                    }
                    case DYNAMIC_OPERABILITY -> {
                        ArrayList<RollUpEnum> rollUps = new ArrayList<>();
                        rollUps.add(RollUpEnum.FDNA);
                        OperabilityAnalysis analysis = new OperabilityAnalysis(rollUps);
                        analyses.add(analysis);
                    }
                    case CG -> {
                        ArrayList<RollUpEnum> rollUps = new ArrayList<>();
                        rollUps.add(RollUpEnum.FDNA);
                        CGAnalysis analysis = new CGAnalysis(rollUps, CGOrderingMethod.DESCENDING, Grouping.SINGLE, false, ColorSet.COLOR_SET1, TargetType.NODES);
                        analyses.add(analysis);
                    }
                    case ATTACK -> {
                        ArrayList<RollUpEnum> rollUps = new ArrayList<>();
                        rollUps.add(RollUpEnum.ODINN_FTI);
                        AttackAnalysis analysis = new AttackAnalysis(rollUps, 1.0, false ,0.0, DecisionOption.IMPACT, true, false);
                        analyses.add(analysis);
                    }
                }
                analysesQ.remove(analysisForm);
                runAnalysisQ();
            }else{
                switch (analysisForm) {
                    case CRITICALITY -> {
                        transitionToPane(criticalityPane);
                        analysesQ.remove(analysisForm);
                    }
                    case DYNAMIC_OPERABILITY -> {
                        transitionToPane(operabilityPane);
                        analysesQ.remove(analysisForm);
                    }
                    case CG -> {
                        transitionToPane(coolGraphPane);
                        analysesQ.remove(analysisForm);
                    }
                    case ATTACK -> {
                        transitionToPane(attackPane);
                        analysesQ.remove(analysisForm);
                    }
                }
            }
        }
    }

    /**
     * Transition from the current pane to any other pane
     *
     * @param pane the pane to transition to
     */
    public void transitionToPane(Node pane){
        if(currentPane != pane){
            Platform.runLater(() -> {
                if(transitionHistory.size() > 10){
                    transitionHistory.remove(0);
                }
                transitionHistory.add(currentPane);
                FxTransitions.slideNodeInAndNodeOut(pane, currentPane, animationTime);
                currentPane = pane;
            });
        }
    }

    /**
     * Create and run a new process
     */
    private void runProgram(){
        if(currentFile != null){
            OutputForm outputType = OutputForm.FULL;
            if(liteCheckBox.isSelected()){
                outputType = OutputForm.LITE;
            }
            Process process = new Process();
            currentProcess = process;
            process.runProcess(app, currentFile, analyses, inputType, outputType);
            transitionToPane(homePane);
        }
    }

    /**
     * Show a process in the processes pane
     *
     * @param process the process to show
     */
    public void showProcess(Process process){
        if(!processesRunningButton.isVisible()){
            Platform.runLater(() -> processesRunningButton.setVisible(true));
        }
        processes.add(process);
        Platform.runLater(() -> {
            processesRunningButton.setText(processes.size() + " Processes Running");
            processesListView.getItems().add(process.ID);
        });
        selectProcess(process.ID);

        transitionToPane(homePane);
    }

    /**
     * Select a process which enables the user to see the status of that process
     *
     * @param processID the ID of the process to select
     */
    public void selectProcess(int processID){
        Process process = null;
        for(Process pro : processes){
            if(pro.ID == processID){
                process = pro;
            }
        }
        if(process != null){
            process.isSelected = true;
            currentProcess = process;
        }
    }

    /**
     * Remove a process from the processes pane
     *
     * @param process the process to remove
     */
    public void hideProcess(Process process){
        processes.remove(process);
        Platform.runLater(() -> {
            for(int i = 0; i < processesListView.getItems().size(); i++){
                if(processesListView.getItems().get(i) == process.ID){
                    processesListView.getItems().remove(i);
                    break;
                }
            }
            processesRunningButton.setText(processes.size() + " Processes Running. Click to open list");
        });
        if(processes.size() == 0){
            Platform.runLater(() -> processesRunningButton.setVisible(false));
        }
    }

    //TODO fix
    private void processTableInput(){
        //process the input and create an output in a separate thread
        /*
        Thread thread = new Thread(() -> {
            ArrayList<AnalysesEnum> analyses = new ArrayList<>();
            analyses.add(AnalysesEnum.CRITICALITY);
            InputConfiguration config = new InputConfiguration(currentFile.getAbsolutePath(), analyses, null, null, null, null, 1.0, 1);
            TableInput input = new TableInput(config, this);

            //TODO catch errors
            try{
                input.read();
            }catch (Exception a){
                input.close();
                CoreLogger.logger.log(Level.SEVERE, a.getMessage());
            }

            try{
                NetworkOutput output = new NetworkOutput(input, true);
                output.write(false);
                output.close();
            }catch (Exception a){
                input.close();
                CoreLogger.logger.log(Level.SEVERE, a.getMessage());
            }

            input.close();
        });
        thread.start();
         */
    }

    /**
     * Read the input file from the input path and determine what type of input it is based on the sheets that it contains
     *
     * @param inputPath the path of the input file
     * @return the input type of the input file
     */
    private InputForm getInputType(String inputPath){
        XSSFWorkbook input;
        FileInputStream fis;
        try {
            fis = new FileInputStream(inputPath);
            input = new XSSFWorkbook(fis);
        } catch (IOException e) {
            return null;
        }

        XSSFSheet linksSheet = input.getSheet(InputForm.STANDARD.mainSheetName);
        if(linksSheet != null){
            closeWorkbook(input, fis);
            return InputForm.STANDARD;
        }else{
            XSSFSheet phiTableSheet = input.getSheet(InputForm.TABLE.mainSheetName);
            if(phiTableSheet != null){
                closeWorkbook(input, fis);
                return InputForm.TABLE;
            }else{
                XSSFSheet incidenceLinksSheet = input.getSheet(InputForm.INCIDENCE.mainSheetName);
                if(incidenceLinksSheet != null){
                    closeWorkbook(input, fis);
                    return InputForm.INCIDENCE;
                }
            }
        }
        closeWorkbook(input, fis);
        return null;
    }

    /**
     * Close the FileInputStream and Workbook which allows outside access to the input file
     *
     * @param workbook the workbook to close
     * @param fis the file input stream to close
     */
    private void closeWorkbook(XSSFWorkbook workbook, FileInputStream fis){
        try {
            workbook.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
