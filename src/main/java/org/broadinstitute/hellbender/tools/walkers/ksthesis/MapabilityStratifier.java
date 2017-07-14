package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.bed.BEDFeature;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

public class MapabilityStratifier extends FeatureStratifier<BEDFeature> {

    private final int binSize;
    private final FeatureInput<BEDFeature> featureInput;

    public MapabilityStratifier(final int binSize, final FeatureInput<BEDFeature> featureInput) {
        this.binSize = binSize;
        this.featureInput = featureInput;
    }

    @Override
    public String getColumnName() {
        return "MAPABILITY";
    }

    @Override
    public String getColumnFormat() {
        return "%d";
    }

    @Override
    public FeatureInput<BEDFeature> getFeatureInput() {
        return featureInput;
    }

    @Override
    public Object getStratification(List<BEDFeature> features) {
        if (features.size() == 0) {
            return -1;
        } else {
            final BEDFeature track = features.get(0);
            final int mapability = (int) Math.floor(100 * Double.valueOf(track.getName()));
            return bin(mapability, binSize);
        }
    }
}
