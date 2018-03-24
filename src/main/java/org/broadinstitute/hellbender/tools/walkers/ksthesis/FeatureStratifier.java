package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.Feature;
import htsjdk.tribble.bed.BEDFeature;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "UnnecessaryLocalVariable"})
public abstract class FeatureStratifier<F extends Feature, T> extends WindowedStratifier<FeatureContext, T> {
    private final FeatureInput<F> featureInput;

    public FeatureStratifier(final FeatureInput<F> featureInput,
                             final boolean enabled,
                             final T disabledStratifier) {
        super(enabled, disabledStratifier);
        this.featureInput = featureInput;
    }

    public T getStratification(final FeatureContext featureContext) {
        final List<F> mapabilityFeatures = featureContext.getValues(
                getFeatureInput(),
                getLeadingBases(),
                getTrailingBases());
        final T stratification = getStratification(mapabilityFeatures);
        return stratification;
    }

    public FeatureInput<F> getFeatureInput() {
        return featureInput;
    }

    public abstract T getStratification(final List<F> features);
}
