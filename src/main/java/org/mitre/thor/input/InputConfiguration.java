package org.mitre.thor.input;

import org.mitre.thor.Process;
import org.mitre.thor.analyses.target.TargetType;
import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.Analysis;
import org.mitre.thor.analyses.CriticalityAnalysis;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;

import java.util.ArrayList;
import java.util.List;

//class that lists the settings of an input
public class InputConfiguration {
    public Process process;
    public String filePath;
    public List<InputQueue> inputQueues;
    public List<Analysis> analyses;
    public ArrayList<TargetType> targetTypes = new ArrayList<>();

    public boolean includeIDMathematica = false;
    public boolean checkNSP = false;
    public boolean checkFDNA = false;
    public boolean checkODINN = false;

    public InputConfiguration(String filePath, Process process, List<Analysis> analyses){
        this.filePath = filePath;
        this.analyses = analyses;
        this.process = process;

        inputQueues = new ArrayList<>();
        for(RollUpEnum rollUpEnum: RollUpEnum.values()) {
            InputQueue queue = new InputQueue(rollUpEnum);
            for(Analysis analysis : analyses){
                if(analysis.rollUpRules.contains(rollUpEnum)){
                    queue.targetAnalyses.add(analysis);
                }
                if(analysis.targetType == TargetType.FACTORS){
                    checkODINN = true;
                }
                if(!targetTypes.contains(analysis.targetType)){
                    targetTypes.add(analysis.targetType);
                }
            }
            if(!queue.targetAnalyses.isEmpty()){
                inputQueues.add(queue);
            }
        }

        if(inputQueues.isEmpty()){
            InputQueue queue = new InputQueue(null);
            queue.targetAnalyses.add(new CriticalityAnalysis(null, null , null, 0, 0,0, false, TargetType.NODES));
            inputQueues.add(queue);
        }

        for(InputQueue queue : inputQueues){
            if(queue.rollUpRule == RollUpEnum.CUSTOM){
                checkNSP = true;
            }
            if(queue.rollUpRule == RollUpEnum.FDNA){
                checkFDNA = true;
            }
            if(queue.rollUpRule == RollUpEnum.ODINN || queue.rollUpRule == RollUpEnum.ODINN_FTI){
                checkODINN = true;
            }
            if(checkNSP && checkFDNA && checkODINN){
                break;
            }
        }

        if(containsAnalysis(AnalysesForm.CRITICALITY)){
            CriticalityAnalysis analysis = (CriticalityAnalysis) getAnalysis(AnalysesForm.CRITICALITY);
            if(analysis.targetType == TargetType.FACTORS){
                checkODINN = true;
            }
        }
    }

    public boolean containsAnalysis(AnalysesForm analysesForm){
        boolean answer = false;
        for(Analysis analysis : analyses){
            if(analysis.formEnum == analysesForm){
                answer = true;
                break;
            }
        }
        return answer;
    }

    public Analysis getAnalysis(AnalysesForm analysisForm){
        Analysis outAnalysis = null;
        for(Analysis analysis : analyses){
            if(analysis.formEnum == analysisForm){
                outAnalysis = analysis;
                break;
            }
        }
        return outAnalysis;
    }
}
