package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.Feature;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

public abstract class FeatureStratifier<T extends Feature> extends Stratifier {
    public abstract FeatureInput<T> getFeatureInput();

    public abstract Object getStratification(final List<T> features);

    public int getLeadingBases() {
        return 0;
    }

    public int getTrailingBases() {
        return 0;
    }
}
