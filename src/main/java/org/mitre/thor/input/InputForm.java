package org.mitre.thor.input;

public enum InputForm {

    STANDARD("Links"),
    INCIDENCE("Links - Incidence"),
    TABLE("Phi");

    public final String mainSheetName;

    InputForm(String mSheetName){this.mainSheetName = mSheetName;}
}
