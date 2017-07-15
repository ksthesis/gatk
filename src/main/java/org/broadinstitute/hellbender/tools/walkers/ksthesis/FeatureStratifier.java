package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.Feature;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class FeatureStratifier<T extends Feature> extends Stratifier {
    public abstract FeatureInput<T> getFeatureInput();

    public Object getStratification(final FeatureContext featureContext) {
        final List<T> mapabilityFeatures = featureContext.getValues(
                getFeatureInput(),
                getLeadingBases(),
                getTrailingBases());
        //noinspection UnnecessaryLocalVariable
        final Object stratification = getStratification(mapabilityFeatures);
        return stratification;
    }

    public abstract Object getStratification(final List<T> features);

    public int getLeadingBases() {
        return 0;
    }

    public int getTrailingBases() {
        return 0;
    }
}
