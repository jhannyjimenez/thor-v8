package org.mitre.thor.analyses.crit;

public enum PhiCalculationEnum {
    ALL(new AllPossibilities()),
    RANDOM(new RandomPossibilities()),
    FDNA(new FDNAPossibilities());

    private final CalculationMethod function;

    PhiCalculationEnum(CalculationMethod calculationMethod){
        this.function = calculationMethod;
    }

    void run(PhiRunnable runnable){
        function.run(runnable);
    }
}