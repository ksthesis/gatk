package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import java.math.BigDecimal;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Stratifier<IN, OUT> {

    private final boolean enabled;
    private final OUT disabledStratifier;

    protected Stratifier(final boolean enabled, final OUT disabledStratifer) {
        this.enabled = enabled;
        this.disabledStratifier = disabledStratifer;
    }

    public abstract String getColumnName();

    public abstract String getColumnFormat();

    public abstract OUT getStratification(final IN in);

    public final OUT getEnabledStratification(final IN in) {
        if (enabled) {
            return getStratification(in);
        } else {
            return disabledStratifier;
        }
    }

    protected static int binInt(int value, int binSize) {
        return (value / binSize) * binSize;
    }

    protected static int binInt(int value, int binSize, int max) {
        return Math.min(max, binInt(value, binSize));
    }

    protected static int binInt(double value, int binSize) {
        return binInt((int)value, binSize);
    }

    protected static int binInt(double value, int binSize, int max) {
        return binInt((int)value, binSize, max);
    }
    
    protected static double binDouble(double value, double binSize) {
        return Math.floor(value / binSize) * binSize;
    }

    protected static double binDouble(double value, double binSize, double max) {
        return Math.min(max, binDouble(value, binSize));
    }

    protected static String getDecimalFormat(final double value) {
        return "%." + getDecimalLength(value) + "f";
    }

    protected static int getDecimalLength(final double value) {
        final String[] decimalParts = BigDecimal.valueOf(value).toPlainString().split("\\.");
        return decimalParts.length == 2 && !"0".equals(decimalParts[1]) ? decimalParts[1].length() : 0;
    }
}
