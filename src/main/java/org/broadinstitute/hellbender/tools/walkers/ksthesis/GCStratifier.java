package org.broadinstitute.hellbender.tools.walkers.ksthesis;

@SuppressWarnings("WeakerAccess")
public class GCStratifier extends ReferenceStratifier<Integer> {

    private final int binSize;

    public GCStratifier(final int binSize) {
        super(binSize > 0, -1);
        this.binSize = binSize;
    }

    @Override
    public String getColumnName() {
        return "gc_content";
    }

    @Override
    public String getColumnFormat() {
        return "%d";
    }

    @Override
    public Integer getStratification(final byte[] bases) {
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
