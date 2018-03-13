package org.broadinstitute.hellbender.tools.walkers.ksthesis;

@SuppressWarnings("WeakerAccess")
public abstract class WindowedStratifier<IN, OUT> extends Stratifier<IN, OUT> {
    private int leadingBases = 0;
    private int trailingBases = 0;

    public WindowedStratifier(boolean enabled, OUT disabledStratifer) {
        super(enabled, disabledStratifer);
    }

    public int getLeadingBases() {
        return leadingBases;
    }

    public int getTrailingBases() {
        return trailingBases;
    }

    public void setLeadingBases(final int leadingBases) {
        this.leadingBases = leadingBases;
    }

    public void setTrailingBases(final int trailingBases) {
        this.trailingBases = trailingBases;
    }
}
