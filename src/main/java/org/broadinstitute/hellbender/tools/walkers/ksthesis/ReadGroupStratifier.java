package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.read.GATKRead;

public class ReadGroupStratifier extends ReadStratifier {

    @Override
    public String getColumnName() {
        return "read_group";
    }

    @Override
    public String getColumnFormat() {
        return "%s";
    }

    @Override
    public Object getStratification(GATKRead read) {
        return read.getReadGroup();
    }
}
