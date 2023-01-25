package org.mitre.thor.logger;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/**
 * The CoreLogger writes formatted output to the console and to a .log file.
 * It must be statically initialized before use.
 */
public class CoreLogger {

    public static final Logger logger = Logger.getLogger("Core");

    /**
     * Modifies the logger's style and sets it to output to the console and a .logger file
     */
    public static void initialize(){

        deletePreviousLogFile();

        logger.setUseParentHandlers(false);
        try {
            Handler fileHandler = new FileHandler("log.logger", true);
            fileHandler.setFormatter(new loggerFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.out.println("Could add 'File Handler' to 'logger'");
            e.printStackTrace();
        }

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new loggerFormatter());
        logger.addHandler(consoleHandler);

        logger.setLevel(Level.ALL);
    }

    /**
     * Deletes the previous log file by deleting a file named 'log.logger' in the current directory
     */
    private static void deletePreviousLogFile(){
        String currentDir = System.getProperty("user.dir");
        File prevLog = new File(currentDir + "/log.logger");
        try {
            if(prevLog.delete()){
                System.out.println("Successfully deleted previous log file");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
