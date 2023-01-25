package org.mitre.thor.output.data_holders;

import org.ejml.simple.SimpleMatrix;

public class CriticalityData {
    public CriticalityTrialData[] criticalityTrialData;
    public CriticalityTrialData averageData = new CriticalityTrialData();

    public void calculateAverage(EquationsEnum equation) {

        SimpleMatrix newAverage = new SimpleMatrix();
        for (CriticalityTrialData criticalityTrialDatum : criticalityTrialData) {
            if (equation == EquationsEnum.MATRIX1) {
                newAverage = new SimpleMatrix(criticalityTrialDatum.alpha1.numRows(), criticalityTrialDatum.alpha1.numCols());
                newAverage = newAverage.plus(criticalityTrialDatum.alpha1);
            } else if (equation == EquationsEnum.MATRIX2) {
                newAverage = new SimpleMatrix(criticalityTrialDatum.alpha2.numRows(), criticalityTrialDatum.alpha2.numCols());
                newAverage = newAverage.plus(criticalityTrialDatum.alpha2);
            } else if (equation == EquationsEnum.CASE1) {
                newAverage = new SimpleMatrix(criticalityTrialDatum.case1.numRows(), criticalityTrialDatum.case1.numCols());
                newAverage = newAverage.plus(criticalityTrialDatum.case1);
            } else if (equation == EquationsEnum.CASE2) {
                newAverage = new SimpleMatrix(criticalityTrialDatum.case2.numRows(), criticalityTrialDatum.case2.numCols());
                newAverage = newAverage.plus(criticalityTrialDatum.case2);
            } else if (equation == EquationsEnum.CASE3) {
                newAverage = new SimpleMatrix(criticalityTrialDatum.case3.numRows(), criticalityTrialDatum.case3.numCols());
                newAverage = newAverage.plus(criticalityTrialDatum.case3);
            } else if (equation == EquationsEnum.GROUPS) {
                newAverage = new SimpleMatrix(criticalityTrialDatum.specialGroup.numRows(), criticalityTrialDatum.specialGroup.numCols());
                newAverage = newAverage.plus(criticalityTrialDatum.specialGroup);
            }
        }
        newAverage.divide(criticalityTrialData.length);
        if (equation == EquationsEnum.MATRIX1) {
            averageData.alpha1 = newAverage;
        } else if (equation == EquationsEnum.MATRIX2) {
            averageData.alpha2 = newAverage;
        } else if (equation == EquationsEnum.CASE1) {
            averageData.case1 = newAverage;
        } else if (equation == EquationsEnum.CASE2) {
            averageData.case2 = newAverage;
        } else if (equation == EquationsEnum.CASE3) {
            averageData.case3 = newAverage;
        } else if (equation == EquationsEnum.GROUPS) {
            averageData.specialGroup = newAverage;
        }
    }
}

