package org.broadinstitute.hellbender.tools.walkers.ksthesis;

public abstract class Stratifier {
    public abstract String getColumnName();

    public abstract String getColumnFormat();

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
