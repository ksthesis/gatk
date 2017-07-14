package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.Random;

public class DebugReadGroupStratifier extends ReadGroupStratifier {
    private static Random rand = new Random(0);

    private final int numSplits;

    public DebugReadGroupStratifier(final int numSplits) {
        this.numSplits = numSplits;
    }

    @Override
    public Object getStratification(GATKRead read) {
        if (numSplits <= 0) {
            return super.getStratification(read);
        } else {
            final String prefix = String.valueOf((char)('A' + rand.nextInt(numSplits)));
            return prefix + "-" + super.getStratification(read);
        }
    }
}
