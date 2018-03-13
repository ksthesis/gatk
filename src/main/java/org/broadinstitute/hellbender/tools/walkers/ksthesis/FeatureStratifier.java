package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.Feature;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "UnnecessaryLocalVariable"})
public abstract class FeatureStratifier<F extends Feature, T> extends WindowedStratifier<FeatureContext, T> {
    public abstract FeatureInput<F> getFeatureInput();

    public FeatureStratifier(final boolean enabled, final T disabledStratifier) {
        super(enabled, disabledStratifier);
    }

    public T getStratification(final FeatureContext featureContext) {
        final List<F> mapabilityFeatures = featureContext.getValues(
                getFeatureInput(),
                getLeadingBases(),
                getTrailingBases());
        final T stratification = getStratification(mapabilityFeatures);
        return stratification;
    }

    public abstract T getStratification(final List<F> features);
}
