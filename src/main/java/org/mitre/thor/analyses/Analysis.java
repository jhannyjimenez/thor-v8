package org.mitre.thor.analyses;

import org.mitre.thor.analyses.target.TargetType;
import org.mitre.thor.input.Input;
import org.mitre.thor.analyses.data_holders.NetworkAnalysisDataHolder;
import org.mitre.thor.analyses.data_holders.NodeAnalysisDataHolder;
import org.mitre.thor.analyses.rolluprules.RollUpEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for any analysis in the software. In this scope, an analysis takes in data from an
 * {@link Input Input} and does something with that data which will then be fed into an
 * {@link org.mitre.thor.output.Output Output}.
 *
 * In order to add an Analysis, first create a subclass which extends this class lets call it 'n'.
 * Then, create a variable 'i' for the analysis in the {@link AnalysesForm AnalysisForm}
 * enum class.In the constructor of the Analysis subclass, call the super method with 'i' as the sole parameter.
 * Now the Analysis has been created, but there is no functionality.
 * In order to add functionality, override the {@link Analysis#inputProcess(Input)}
 * function and do things with the input.How you handle the output of the analysis is up to you, but it is recommended
 * that you add any analysis output variable to the {@link NodeAnalysisDataHolder NodeRuleOutputHolder}
 * class which gets added to every node or the {@link NetworkAnalysisDataHolder NetworkRuleOutputHolder}
 * class which gets added to the network.
 *
 * Note that each input format gives a ton of freedom on what to read, so if the new analysis requires you to
 * read additional data that isn't node relationships (for example the A and B values in the criticality analysis),
 * that needs to be added to the input class in the 'readAnalysisSpecific' function. How you do so is up to you,
 * but there are already three examples in place there.
 *
 * Similarly, each output is free to use the Analysis and its data however it wants. That means that whenever you add a
 * new analysis, you must also add some way to handle it to each output class. Once again, how you do so is up to you,
 * and there are various examples
 *
 * Finally for the analysis to be available from the GUI, one must edit the 'processAnalysisQ' function in the GUI
 * Controller class. Add the Analysis enum 'i' to the switch expression, then add some way to to get input from the GUI
 * (that's completely up to you), then add an instance of 'n' to the analyses list in the Controller class. That's it.
 */

public abstract class Analysis {
    public AnalysesForm formEnum;
    public List<RollUpEnum> rollUpRules = new ArrayList<>();
    public TargetType targetType = TargetType.NODES;

    Analysis(AnalysesForm e){
        formEnum = e;
    }

    public abstract boolean inputProcess(Input input);

    @Override
    public String toString() {
        return formEnum.toString();
    }
}
