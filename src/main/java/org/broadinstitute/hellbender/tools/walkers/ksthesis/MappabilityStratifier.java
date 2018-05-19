package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.bed.BEDFeature;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class MappabilityStratifier extends FeatureStratifier<BEDFeature, Integer> {

    private final int binSize;

    public MappabilityStratifier(final FeatureInput<BEDFeature> featureInput, final int binSize) {
        super(featureInput, binSize > 0, -1);
        this.binSize = binSize;
    }

    @Override
    public String getColumnName() {
        return "mappability";
    }

    @Override
    public String getColumnFormat() {
        return "%d";
    }

    @Override
    public Integer getStratification(List<BEDFeature> features) {
        if (features.size() == 0) {
            return -1;
        } else {
            final BEDFeature track = features.get(0);
            final float score = track.getScore();
            if (Float.isNaN(score))
                return -1;
            final int mappability = (int) Math.floor(100 * score);
            return binInt(mappability, binSize);
        }
    }
}
