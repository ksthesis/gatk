package org.broadinstitute.hellbender.tools.walkers.ksthesis;

@SuppressWarnings("WeakerAccess")
public class GCStratifier extends ReferenceStratifier<Double> {

    private final double binSize;
    private final String columnFormat;

    public GCStratifier(final double binSize) {
        super(binSize > 0, -1D);
        this.binSize = binSize;
        this.columnFormat = getDecimalFormat(binSize);
    }

    @Override
    public String getColumnName() {
        return "gc_content";
    }

    @Override
    public String getColumnFormat() {
        return columnFormat;
    }

    @Override
    public Double getStratification(final byte[] bases) {
        int atContent = 0;
        int gcContent = 0;
        for (final byte base : bases) {
            if (base == 'A' || base == 'T') {
                atContent++;
            } else if (base == 'G' || base == 'C') {
                gcContent++;
            }
        }
        if (atContent + gcContent == 0) {
            return -1D;
        } else {
            final double pct = (100D * gcContent) / (atContent + gcContent);
            return binDouble(pct, binSize);
        }
    }
}
