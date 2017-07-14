package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.read.GATKRead;

public abstract class ReadStratifier extends Stratifier {
    public abstract Object getStratification(final GATKRead read);
}
