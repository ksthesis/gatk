package org.broadinstitute.hellbender.tools.walkers.ksthesis;

public class GCStratifier extends ReferenceStratifier {

    private final int binSize;
    private final int leadingBases;
    private final int trailingBases;

    public GCStratifier(final int binSize, final int leadingBases, final int trailingBases) {
        this.binSize = binSize;
        this.leadingBases = leadingBases;
        this.trailingBases = trailingBases;
    }

    @Override
    public String getColumnName() {
        return "GC_CONTENT";
    }

    @Override
    public String getColumnFormat() {
        return "%d";
    }

    @Override
    public int getLeadingBases() {
        return leadingBases;
    }

    @Override
    public int getTrailingBases() {
        return trailingBases;
    }

    @Override
    public Object getStratification(byte[] bases) {
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
            return 0;
        } else {
            final int pct = (100 * gcContent) / (atContent + gcContent);
            return bin(pct, binSize);
        }
    }
}
