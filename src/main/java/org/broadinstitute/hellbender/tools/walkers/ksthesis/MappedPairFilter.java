package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.utils.read.GATKRead;

public class MappedPairFilter extends ReadFilter {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean test(final GATKRead read) {
        return read.isPaired() && !read.mateIsUnmapped();
    }
}
