package org.mitre.thor;

//TODO: add system to easily verify that the argument answers are supported not just the arguments

/**
 * The list of all supported arguments.
 */
public enum ConsoleArgument {
    HELP("help", false),
    FILEPATH("filepath", true),
    LITE("lite", false),
    ROLL_UP_RULE("rule", true),
    ANALYSIS("analysis", true),
    CALCULATION_METHOD("calcmethod", false),
    MAX_MINUTES("maxminutes", false),
    THREADS("threads", false),
    TRIALS("trials", false),
    GROUP("group", false),
    CG_ORDERING("ordering", false),
    CG_SAVE_IMG("saveimg", false),
    GROUPING("grouping", false),
    ANALYSIS_TARGET("target", false),
    BUDGET("budget", false),
    SIMULATIONS("simulations", false);

    public final String name;
    public final boolean required;
    ConsoleArgument(String name, boolean required){
        this.name = name;
        this.required = required;
    }
}
