package org.mitre.thor.input;

import org.mitre.thor.analyses.AnalysesForm;
import org.mitre.thor.analyses.Analysis;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;

import java.util.ArrayList;
import java.util.List;

public class InputQueue {
    public final RollUpEnum rollUpRule;
    public final List<Analysis> targetAnalyses = new ArrayList<>();
    public boolean hasBeenCalculated = false;

    public InputQueue(RollUpEnum rollUpRule){
        this.rollUpRule = rollUpRule;
    }

    public boolean containsTargetAnalysis(AnalysesForm analysesForm){
        boolean answer = false;
        for(Analysis analysis : targetAnalyses){
            if(analysis.formEnum == analysesForm){
                answer = true;
                break;
            }
        }
        return answer;
    }

    public Analysis getAnalysis(AnalysesForm analysisForm){
        Analysis outAnalysis = null;
        for(Analysis analysis : targetAnalyses){
            if(analysis.formEnum == analysisForm){
                outAnalysis = analysis;
                break;
            }
        }
        return outAnalysis;
    }
}
