package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.read.GATKRead;

@SuppressWarnings("WeakerAccess")
public class ReadGroupStratifier extends ReadStratifier<String> {

    public ReadGroupStratifier(final boolean flattenReadGroups) {
        super(!flattenReadGroups, "disabled");
    }

    @Override
    public String getColumnName() {
        return "read_group";
    }

    @Override
    public String getColumnFormat() {
        return "%s";
    }

    @Override
    public String getStratification(final GATKRead read) {
        return read.getReadGroup();
    }
}
