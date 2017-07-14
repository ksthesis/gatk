package org.broadinstitute.hellbender.tools.walkers.ksthesis;

// TODO: Enable a sliding reference stratifier?
public abstract class ReferenceStratifier extends Stratifier {
    public abstract Object getStratification(final byte[] bases);

    public int getLeadingBases() {
        return 0;
    }

    public int getTrailingBases() {
        return 0;
    }
}
