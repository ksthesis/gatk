package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.bed.BEDFeature;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class RegionLabelStratifier extends FeatureStratifier<BEDFeature, String> {

    private final String defaultStratifier;

    public RegionLabelStratifier(final FeatureInput<BEDFeature> featureInput, final boolean enabled) {
        this(featureInput, enabled,"default");
    }

    public RegionLabelStratifier(final FeatureInput<BEDFeature> featureInput,
                                 final boolean enabled,
                                 final String defaultStratifier) {
        super(featureInput,enabled, "disabled");
        this.defaultStratifier = defaultStratifier;
    }

    @Override
    public String getColumnName() {
        return "region";
    }

    @Override
    public String getColumnFormat() {
        return "%s";
    }

    @Override
    public String getStratification(List<BEDFeature> features) {
        if (features.size() == 0) {
            return defaultStratifier;
        } else {
            final BEDFeature track = features.get(0);
            return track.getName();
        }
    }
}
