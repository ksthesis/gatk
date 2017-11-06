package org.broadinstitute.hellbender.tools.walkers.ksthesis;

@SuppressWarnings("WeakerAccess")
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

    protected static int bin(int value, int binSize) {
        return (value / binSize) * binSize;
    }

    protected static int bin(int value, int binSize, int max) {
        return Math.min(max, bin(value, binSize));
    }

    protected static int bin(double value, int binSize) {
        return bin((int)value, binSize);
    }

    protected static int bin(double value, int binSize, int max) {
        return bin((int)value, binSize, max);
    }
}
