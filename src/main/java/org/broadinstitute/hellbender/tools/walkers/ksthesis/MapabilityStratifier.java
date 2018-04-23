package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.bed.BEDFeature;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class MapabilityStratifier extends FeatureStratifier<BEDFeature, Integer> {

    private final int binSize;

    public MapabilityStratifier(final FeatureInput<BEDFeature> featureInput, final int binSize) {
        super(featureInput, binSize > 0, -1);
        this.binSize = binSize;
    }

    @Override
    public String getColumnName() {
        return "mapability";
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
            final int mapability = (int) Math.floor(100 * score);
            return bin(mapability, binSize);
        }
    }
}
