package org.mitre.thor;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.InputForm;
import org.mitre.thor.analyses.Analysis;
import org.mitre.thor.analyses.CGAnalysis;
import org.mitre.thor.analyses.CriticalityAnalysis;
import org.mitre.thor.analyses.OperabilityAnalysis;
import org.mitre.thor.analyses.cg.ColorSet;
import org.mitre.thor.analyses.grouping.Grouping;
import org.mitre.thor.analyses.cg.CGOrderingMethod;
import org.mitre.thor.analyses.crit.PhiCalculationEnum;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;
import org.mitre.thor.output.OutputForm;
import org.mitre.thor.analyses.target.TargetType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * This is the Application entry class which contains the main function.
 * Its purpose is to initialize the console logger and the GUI application and handle the single function call.
 */
public class Main {
    //Start up function
    public static void main(String[] args){
        //Initialize the console logger
        CoreLogger.initialize();
        //Print arguments for debugging purposes
        System.out.println(Arrays.toString(args));
        //Create Application entry class
        App app = new App();
        if(args.length == 0){
            //If no console args are passed then show the gui
            app.launchApp(true);
        }else{
            System.out.println("Starting function call");
            //Get a list of all the possible arguments
            ArrayList<String> arguments = new ArrayList<>();
            ArrayList<String> requiredArguments = new ArrayList<>();
            for(ConsoleArgument arg : ConsoleArgument.values()){
                arguments.add("-" + arg.name);
                if(arg.required){
                    requiredArguments.add("-" + arg.name);
                }
            }
            //Check if any of the arguments are not supported, and give the supported arguments
            for(String arg : args){
                if(arg.contains("-") && !arguments.contains(arg)){
                    System.out.println("The option [" + arg + "] is invalid");
                    printSupportedArguments(arguments);
                }
            }

            //Handle the help argument
            if(containsArg(args, "-help")){
                printSupportedArguments(arguments);
                System.out.println("The minimum requirements are: filepath, rule, analysis");
                System.out.println("The available rules are: or, and, fdna, fdna2, odinn, custom");
                System.out.println("The available analysis are: criticality, operability, cg");
                System.out.println("The available calcmethod are: all, random, fdna");
                System.out.println("The available grouping are: single, pairs, and triples");
                System.out.println("The available targets are: nodes, factors");
                System.out.println("The available ordering are: ascending, descending");
                System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");
                System.out.println("The -filepath flag should be followed by a string of the absolute path of the input file");
                System.out.println("The -maxminutes flag should be followed by an integer indicating the max amount of minutes for phi calculation in the Criticality analysis");
                System.out.println("The -threads flag should be followed by an integer indicating the number of threads to use in the phi calculation");
                System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");
                System.out.println("The -lite flag tells THOR to use lite option defaults");
                System.out.println("The -group flag tells THOR that the criticality output should group nodes which have similar criticality values");
                System.out.println("The -saveimg flag tells THOR to save the CG Graph when doing a CG analysis");

                System.exit(-1);
            }

            //Check if arguments list contains all the required arguments
            for(String arg : requiredArguments){
                boolean exit = false;
                if(!containsArg(args, arg)){
                    System.out.println("The argument [" + arg + "] is required for all analysis");
                    exit = true;
                }
                if(exit)
                    System.exit(-1);
            }

            //Check if the file given is valid
            String filePath = getArgAnswer(args, "-filepath");
            if(!isValidFilePath(filePath)){
                System.out.println("The file path \"" + filePath + "\" is invalid");
                System.exit(-1);
            }

            File inputFile = new File(filePath);
            InputForm inputForm = getInputType(filePath);

            System.out.println("Input form: " + inputForm);

            OutputForm outputForm = OutputForm.FULL;
            if(containsArg(args, "-" + ConsoleArgument.LITE.name)){
                outputForm = OutputForm.LITE;
            }

            //Get the right roll up rule
            String rollupString = getArgAnswer(args, "-" + ConsoleArgument.ROLL_UP_RULE.name);
            RollUpEnum rollUpRule = null;
            if(rollupString != null){
                switch (rollupString) {
                    case "or" -> rollUpRule = RollUpEnum.OR;
                    case "and" -> rollUpRule = RollUpEnum.AND;
                    case "fdna" -> rollUpRule = RollUpEnum.FDNA;
                    case "fdna2" -> rollUpRule = RollUpEnum.FDNA2;
                    case "odinn" -> rollUpRule = RollUpEnum.ODINN_FTI;
                    case "custom" -> rollUpRule = RollUpEnum.CUSTOM;
                }
            }
            if(rollUpRule == null){
                System.out.println("The roll up rule [" + rollupString  + "] is invalid");
                System.exit(-1);
            }
            System.out.println("Roll up Rule: " + rollUpRule);

            //get the right analysis
            ArrayList<Analysis> analyses = new ArrayList<>();
            String analysisString = getArgAnswer(args, "-" + ConsoleArgument.ANALYSIS.name);

            //get the right target type
            TargetType targetType = TargetType.NODES;
            String targetString = Main.getArgAnswer(args,"-" + ConsoleArgument.ANALYSIS_TARGET.name);
            if(targetString != null){
                switch (targetString){
                    case "factor", "factors" -> targetType = TargetType.FACTORS;
                }
            }

            //get the right grouping
            Grouping grouping = Grouping.SINGLE;
            String groupString = getArgAnswer(args, "-" + ConsoleArgument.GROUP.name);
            if(groupString != null){
                switch (groupString){
                    case "pair", "pairs" -> grouping = Grouping.PAIRS;
                    case "triple", "triples" -> grouping = Grouping.PAIRS_AND_TRIPLES;
                }
            }

            // create the different analysis depending on the analysis argument
            if(analysisString != null){
                switch (analysisString) {
                    case "criticality" -> {
                        System.out.println("Analysis: Criticality");
                        String calcString = getArgAnswer(args, "-" + ConsoleArgument.CALCULATION_METHOD.name);
                        PhiCalculationEnum calculationMethod = null;
                        if (calcString != null) {
                            switch (calcString) {
                                case "all" -> calculationMethod = PhiCalculationEnum.ALL;
                                case "fdna" -> calculationMethod = PhiCalculationEnum.FDNA;
                                case "random" -> calculationMethod = PhiCalculationEnum.RANDOM;
                            }
                        }

                        if(calculationMethod == null){
                            calculationMethod = PhiCalculationEnum.RANDOM;
                        }

                        ArrayList<RollUpEnum> rollUpEnums = new ArrayList<>();
                        rollUpEnums.add(rollUpRule);
                        double maxMinutes = 5;

                        if (calculationMethod == PhiCalculationEnum.RANDOM || calculationMethod == PhiCalculationEnum.FDNA) {
                            maxMinutes = getDoubleArgAnswer(args, "-" + ConsoleArgument.MAX_MINUTES.name, 1);
                        }else if(getArgAnswer(args, "-" + ConsoleArgument.MAX_MINUTES.name) == null){
                            System.out.println("Cannot set the max minutes for Criticality when not using the random or fdna calculation methods");
                        }
                        int threads = getIntArgAnswer(args, "-" + ConsoleArgument.THREADS.name, 1);
                        int trials = getIntArgAnswer(args, "-" + ConsoleArgument.TRIALS.name, 1);
                        boolean group = containsArg(args, "-" + ConsoleArgument.GROUP.name);

                        CriticalityAnalysis crit = new CriticalityAnalysis(calculationMethod, rollUpEnums, grouping, maxMinutes, threads, trials, !group, targetType);
                        analyses.add(crit);
                    }
                    case "operability" -> {
                        System.out.println("Analysis: Operability");
                        ArrayList<RollUpEnum> rollUpEnums2 = new ArrayList<>();
                        rollUpEnums2.add(rollUpRule);
                        OperabilityAnalysis op = new OperabilityAnalysis(rollUpEnums2);
                        analyses.add(op);
                    }
                    case "cg" -> {
                        System.out.println("Analysis: CG");
                        ArrayList<RollUpEnum> rollUpEnums3 = new ArrayList<>();
                        rollUpEnums3.add(rollUpRule);
                        String orderS = getArgAnswer(args, "-" + ConsoleArgument.CG_ORDERING.name);
                        CGOrderingMethod orderingMethod = CGOrderingMethod.DESCENDING;
                        if(orderS != null && orderS.equals("ascending")){
                            orderingMethod = CGOrderingMethod.ASCENDING;
                        }
                        boolean saveImg = getBooleanArgAnswer(args, "-" + ConsoleArgument.CG_SAVE_IMG.name, false);

                        CGAnalysis cg = new CGAnalysis(rollUpEnums3, orderingMethod, grouping, saveImg, ColorSet.COLOR_SET1, targetType);
                        analyses.add(cg);
                    }
                    default -> {
                        System.out.println("The analysis [" + analysisString  + "] is invalid");
                        System.exit(-1);
                    }
                }
            }

            Process process = new Process();
            process.runProcess(app, inputFile, analyses, inputForm, outputForm);
        }
    }


    /**
     * Check if an argument is inside the args list. This essentially copies the 'contains' function from the
     * Arraylist class.
     *
     * @param args The list of arguments from the command line
     * @param arg The argument that should be inside the first list
     * @return whether the second param is inside the first param
     */
    static boolean containsArg(String[] args, String arg){
        for(String s : args){
            if(s.equals(arg)){
                return true;
            }
        }
        return false;
    }


    /**
     * Get whatever the user wrote after an argument in String form.
     * For example if the user puts -filepath C:/important/my_file.xlsx, then the argument is '-filepath', and the
     * answer is 'C:/important/my_file.xlsx'
     *
     * @param args The list of arguments from the command line
     * @param arg The argument which you want the answer of
     * @return a string answer
     */
    static String getArgAnswer(String[] args, String arg){
        String answer = null;
        for(int i = 0; i < args.length; i++){
            if(Objects.equals(args[i], arg) && i < args.length - 1){
                answer = args[i + 1];
                break;
            }
        }
        return answer;
    }

    /**
     * Get whatever the user wrote after an argument and attempt to cast it to an Integer.
     * For example if the user puts -filepath C:/important/my_file.xlsx, then the argument is '-filepath', and the
     * answer is 'C:/important/my_file.xlsx'
     *
     * @param args The list of arguments from the command line
     * @param arg The argument which you want the answer of
     * @return an int answer
     */
    static int getIntArgAnswer(String[] args, String arg, int fallback){
        try {
             return Integer.parseInt(getArgAnswer(args, arg));
        }catch (Exception e){
            //e.printStackTrace();
            //TODO: throw error
        }
        return fallback;
    }

    /**
     * Get whatever the user wrote after an argument and attempt to cast it to a Double.
     * For example if the user puts -filepath C:/important/my_file.xlsx, then the argument is '-filepath', and the
     * answer is 'C:/important/my_file.xlsx'
     *
     * @param args The list of arguments from the command line
     * @param arg The argument which you want the answer of
     * @return a double answer
     */
    static double getDoubleArgAnswer(String[] args, String arg, double fallback){
        try {
            return Double.parseDouble(getArgAnswer(args, arg));
        }catch (Exception e){
            //e.printStackTrace();
            //TODO: throw error
        }
        return fallback;
    }

    /**
     * Get whatever the user wrote after an argument and attempt to cast it to a boolean.
     * For example if the user puts -filepath C:/important/my_file.xlsx, then the argument is '-filepath', and the
     * answer is 'C:/important/my_file.xlsx'
     *
     * @param args The list of arguments from the command line
     * @param arg The argument which you want the answer of
     * @return a boolean answer
     */
    static boolean getBooleanArgAnswer(String[] args, String arg, boolean fallback){
        try{
            switch (getArgAnswer(args, arg)){
                case "true", "1" -> {
                    return true;
                }
                case "false", "0" -> {
                    return false;
                }
            }
        }catch (Exception e){
            //e.printStackTrace();
            //TODO: throw error
        }
        return fallback;
    }

    /**
     * This function determines what type of input the user is passing depending on what sheets are in the input
     * Workbook
     *
     * @param inputPath The absolute path to the input file
     * @return the form of the input
     */
    static InputForm getInputType(String inputPath){
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
     * Determines if a path is actually valid and is in the file system.
     *
     * @param path The absolute path to a file or folder
     * @return whether the path is valid
     */
    public static boolean isValidFilePath(String path) {
        File f = new File(path);
        try {
            f.getCanonicalPath();
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * Closes a workbook and input file stream so that other applications can access the workbook.
     *
     * @param workbook the workbook to close
     * @param fis the file input stream
     */
     static void closeWorkbook(XSSFWorkbook workbook, FileInputStream fis){
        try {
            workbook.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints all the available console arguments.
     * You must first find all the console args then pass them to this function. This function only prints them.
     *
     * @param arguments the available arguments
     */
    private static void printSupportedArguments(ArrayList<String> arguments){
        System.out.println("The available options are: ");
        for(String rg : arguments){
            System.out.print(rg + ", ");
        }
    }
}
