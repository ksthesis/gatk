package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.read.GATKRead;

@SuppressWarnings("WeakerAccess")
public abstract class ReadStratifier<T> extends Stratifier<GATKRead, T> {

    public ReadStratifier(final boolean enabled, final T disabledStratifier) {
        super(enabled, disabledStratifier);
    }
}
