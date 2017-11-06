package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.read.GATKRead;

@SuppressWarnings("WeakerAccess")
public class AbsTLenStratifier extends ReadStratifier<Integer> {

    private final int binSize;
    private final int insertSizeMax;

    public AbsTLenStratifier(final int binSize, final int insertSizeMax) {
        super(binSize > 0 && insertSizeMax > 0, -1);
        this.binSize = binSize;
        this.insertSizeMax = insertSizeMax;
    }

    @Override
    public String getColumnName() {
        return "insert_length";
    }

    @Override
    public String getColumnFormat() {
        return "%d";
    }

    @Override
    public Integer getStratification(final GATKRead read) {
        return bin(Math.abs(read.getFragmentLength()), binSize, insertSizeMax);
    }
}
