package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.engine.ReferenceContext;

// TODO: Enable a sliding reference stratifier?
@SuppressWarnings({"WeakerAccess", "UnnecessaryLocalVariable"})
public abstract class ReferenceStratifier<T> extends WindowedStratifier<ReferenceContext, T> {

    public ReferenceStratifier(final boolean enabled, final T disabledStratifier) {
        super(enabled, disabledStratifier);
    }

    public T getStratification(final ReferenceContext ref) {
        ref.setWindow(getLeadingBases(), getTrailingBases());
        final byte[] bases = ref.getBases();
        final T stratification = getStratification(bases);
        return stratification;
    }

    public abstract T getStratification(final byte[] bases);
}
