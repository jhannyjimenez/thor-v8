package org.mitre.thor.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AppUtil {
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static boolean vectorContainsNAN(double[] vector){
        boolean containsNAN = false;
        for(double v : vector){
            if (Double.isNaN(v)) {
                containsNAN = true;
                break;
            }
        }
        return containsNAN;
    }
}
