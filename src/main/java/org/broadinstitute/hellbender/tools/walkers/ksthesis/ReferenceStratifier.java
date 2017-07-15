package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.engine.ReferenceContext;

// TODO: Enable a sliding reference stratifier?
@SuppressWarnings("WeakerAccess")
public abstract class ReferenceStratifier extends Stratifier {
    public Object getStratification(final ReferenceContext ref) {
        ref.setWindow(getLeadingBases(), getTrailingBases());
        final byte[] bases = ref.getBases();
        //noinspection UnnecessaryLocalVariable
        final Object stratification = getStratification(bases);
        return stratification;
    }

    public abstract Object getStratification(final byte[] bases);

    public int getLeadingBases() {
        return 0;
    }

    public int getTrailingBases() {
        return 0;
    }
}
