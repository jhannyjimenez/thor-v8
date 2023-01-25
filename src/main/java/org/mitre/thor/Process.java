package org.mitre.thor;

import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.Input;
import org.mitre.thor.input.InputConfiguration;
import org.mitre.thor.input.InputForm;
import org.mitre.thor.input.formats.IncidenceInput;
import org.mitre.thor.input.formats.StandardInput;
import org.mitre.thor.analyses.Analysis;
import org.mitre.thor.output.LiteOutput;
import org.mitre.thor.output.NetworkOutput;
import org.mitre.thor.output.Output;
import org.mitre.thor.output.OutputForm;

import java.io.File;

import java.util.List;
import java.util.logging.Level;

/**
 * A process represents the processing of one or multiple analysis with one or multiple options.
 * When processing analyses, this class first creates an input. It then reads all the information from the input file.
 * Tt runs through the analysis code, and finally it creates an output.
 * Note that this all takes place in a separate thread.
 */
public class Process {

    public static int counter = 0;
    public static final boolean INCLUDE_ID_MATHEMATICA = true;
    public static final boolean INCLUDE_LITE_OUTPUT = true;

    public final int ID;
    public boolean isSelected = false;

    public Input input;
    Output output;

    public Process(){
        Process.counter++;
        this.ID = counter;
    }

    public void runProcess(App app, File selectedFile, List<Analysis> analyses, InputForm inputType, OutputForm outputType){
        Thread runThread = new Thread(() -> {
            long startTime = System.nanoTime();
            InputConfiguration config = new InputConfiguration(selectedFile.getAbsolutePath(), this, analyses);
            config.includeIDMathematica = INCLUDE_ID_MATHEMATICA;

            //create and read input
            if(inputType == InputForm.STANDARD){
                input = new StandardInput(config, app);
            }else if(inputType == InputForm.INCIDENCE){
                input = new IncidenceInput(config, app);
            }

            boolean successRead = false;
            try{
                successRead = input.read();
            }catch (Exception e){
                input.close();
                CoreLogger.logger.log(Level.SEVERE, e.getMessage());
            }

            if(successRead){
                if(outputType == OutputForm.FULL){
                    output = new NetworkOutput(input);
                }else if(outputType == OutputForm.LITE){
                    output = new LiteOutput(input, false);
                }
                boolean successWrite = output.write(true);
                if(successWrite){
                    output.close();
                    if(INCLUDE_LITE_OUTPUT && outputType == OutputForm.FULL){
                        output = new LiteOutput(input, true);
                        successWrite = output.write(false);
                        if(successWrite){
                            output.close();
                        }else{
                            input.close();
                        }
                    }
                }else{
                    input.close();
                }
            }else{
                input.close();
                String msg = "Failed to read input";
                app.GAE(msg, false);
            }
            input.close();
            long endTime = System.nanoTime();
            CoreLogger.logger.log(Level.WARNING, "Total Runtime: " + ((endTime - startTime) / 1000000) + "ms");
            app.GAS("All operations are finished. You can now open the file", true);
        });
        runThread.start();
    }
}
