package org.mitre.thor.analyses;

import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.input.Input;
import org.mitre.thor.math.Equations;
import org.mitre.thor.analyses.grouping.Grouping;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.analyses.crit.PhiCalculationEnum;
import org.mitre.thor.analyses.crit.PhiRunnable;
import org.mitre.thor.network.nodes.Node;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;
import org.mitre.thor.analyses.target.TargetType;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class CriticalityAnalysis extends Analysis{
    public PhiCalculationEnum calculationMethod;
    public double maxMinutes;
    public int threads;
    public int trials;
    public boolean groupByStrength;
    public Grouping grouping;

    public CriticalityAnalysis(PhiCalculationEnum calculationMethod, List<RollUpEnum> rollUpRules, Grouping grouping, double maxMinutes, int threads, int trials, boolean groupByStrength, TargetType targetType){
        super(AnalysesForm.CRITICALITY);
        this.rollUpRules = rollUpRules;
        this.calculationMethod = calculationMethod;
        this.grouping = grouping;
        this.maxMinutes = maxMinutes;
        this.threads = threads;
        this.trials = trials;
        this.groupByStrength = groupByStrength;
        this.targetType = targetType;
    }

    @Override
    public boolean inputProcess(Input input) {
        return calculatePhis(input);
    }

    //calculates phis using the methods defined in 'iConfig'
    protected boolean calculatePhis(Input input){
        if(input.app.controller != null){
            input.app.controller.showProcess(input.iConfig.process);
        }else{
            input.iConfig.process.isSelected = true;
        }

        input.network.removeGroups("crit");
        if(grouping == Grouping.PAIRS){
            input.network.generatePairs("crit", targetType);
        }else if(grouping == Grouping.PAIRS_AND_TRIPLES){
            input.network.generatePairs("crit", targetType);
            input.network.generateTriples("crit", targetType);
        }

        if(grouping == Grouping.PAIRS || grouping == Grouping.PAIRS_AND_TRIPLES){
            Activity global_node = input.network.createActivity("GLOBAL", 888888, 888888, false);
            global_node.isReal = false;
            global_node.isPhysical = false;
            input.network.addNode(global_node);
        }

        ArrayList<Node> nodes = targetType.getTargetNodes(input.network);
        ArrayList<Node> allNodes = new ArrayList<>(nodes);
        allNodes.addAll(input.network.getGroups("crit"));

        /*
        for(NetworkAnalysisDataHolder dataHolder : input.network.analysisDataHolders){
            for(NetworkCriticalityTrial criticalityTrial : dataHolder.networkCriticalityTrials){
                criticalityTrial.E = new SimpleMatrix(allNodes.size() + 1, allNodes.size() + 1);
            }
        }
         */

        input.livePossibilitiesCount = BigInteger.ZERO;

        BigInteger poss;

        poss = Equations.powerN(new BigInteger("2"), nodes.size());

        if(poss.equals(BigInteger.ZERO)){
            input.app.GAE("Total amount of node possibilities cannot be zero", true);
            return false;
        }else{
            CoreLogger.logger.log(Level.WARNING, "Total Binaries: " +  poss);
        }

        if(threads >= poss.doubleValue()){
            input.app.GAE("Number of threads cannot be higher or equal to the network's total possibilities " +
                    "- Please lower the # of threads", true);
            return false;
        }

        int numberOfThreads = threads;
        if(numberOfThreads == 0){
            input.app.GAE("Number of threads can not be equal to 0", true);
            return false;
        }

        for(Activity activity : input.network.getActivities()){
            activity.isOn = true;
        }

        input.network.networkOrder.clear();
        input.network.clearIteratedNodes();
        input.network.findNetworkOrder(input.network.startActivity);

        //create a manager to keep track of all threads
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        Instant startInstant = Instant.now();

        //calculates all possibilities by assigning a section of the total possibilities to each thread
        if(calculationMethod == PhiCalculationEnum.ALL) {
            BigInteger possibilitiesPerThread = poss.divide(BigInteger.valueOf(numberOfThreads));
            BigInteger start = BigInteger.ZERO;
            BigInteger end = possibilitiesPerThread;
            BigInteger additionalPoss = poss.remainder(BigInteger.valueOf(numberOfThreads));

            if(end.equals(BigInteger.ZERO)){
                input.app.GAE("Calculation Runnable end cannot be zero", false);
                return false;
            }

            PhiRunnable runnable;
            for(int i = 0; i < numberOfThreads; i++){
                if (i == numberOfThreads - 1) {
                    runnable = new PhiRunnable(input, calculationMethod, input.iConfig.inputQueues, maxMinutes, start, end.add(additionalPoss));
                } else {
                    runnable = new PhiRunnable(input, calculationMethod, input.iConfig.inputQueues, maxMinutes, start, end);
                }
                executor.execute(runnable);
                start = start.add(possibilitiesPerThread);
                end = end.add(possibilitiesPerThread);
            }
        }else{
            PhiRunnable runnable;
            for(int i = 0; i < numberOfThreads; i++) {
                runnable = new PhiRunnable(input, calculationMethod, input.iConfig.inputQueues, maxMinutes, null, null);
                executor.execute(runnable);
            }
        }

        //wait for all threads to finish
        executor.shutdown();
        double maxMinutes = this.maxMinutes;
        PhiCalculationEnum method = this.calculationMethod;
        while(!executor.isTerminated()){
            if(input.iConfig.process.isSelected){
                input.updateLoadScreen(startInstant, maxMinutes, poss, method);
            }

            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(input.iConfig.process.isSelected){
            input.updateLoadScreen(startInstant, maxMinutes, poss, method);
        }

        if(input.app.controller != null){
            input.app.controller.hideProcess(input.iConfig.process);
        }else{
            input.iConfig.process.isSelected = false;
        }

        if(input.phiForceStop && calculationMethod != null && calculationMethod == PhiCalculationEnum.ALL){
            input.app.GAE("When using the all possibilities method, please do not stop the calculation", true);
            input.app.GAE("switch to a different calculation method or let the loading process finish", true);
            return false;
        }

        return true;
    }
}
