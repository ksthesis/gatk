package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.read.GATKRead;

@SuppressWarnings("WeakerAccess")
public class StrandOrientationStratifier extends ReadStratifier<String> {

    public StrandOrientationStratifier(final boolean enabled) {
        super(enabled, "disabled");
    }

    @Override
    public String getColumnName() {
        return "strand";
    }

    @Override
    public String getColumnFormat() {
        return "%s";
    }

    @Override
    public String getStratification(final GATKRead read) {
        return read.isReverseStrand() ? read.isFirstOfPair() ? "R1" : "R2" : read.isFirstOfPair() ? "F1" : "F2";
    }
}
