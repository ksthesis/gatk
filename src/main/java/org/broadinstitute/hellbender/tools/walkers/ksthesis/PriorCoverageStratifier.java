package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFConstants;
import org.broadinstitute.hellbender.engine.FeatureInput;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PriorCoverageStratifier extends FeatureStratifier<VariantContext, Integer> {

    private final int binSize;
    private final int depthMax;

    public PriorCoverageStratifier(final FeatureInput<VariantContext> featureInput,
                                   final int binSize,
                                   final int depthMax) {
        super(featureInput, binSize > 0 && depthMax > 0, -1);
        this.binSize = binSize;
        this.depthMax = depthMax;
    }

    @Override
    public String getColumnName() {
        return "pcov";
    }

    @Override
    public String getColumnFormat() {
        return "%d";
    }

    @Override
    public Integer getStratification(List<VariantContext> features) {
        if (features.size() == 0) {
            return -1;
        } else {
            final VariantContext track = features.get(0);
            final int depth = track.getAttributeAsInt(VCFConstants.DEPTH_KEY, -1);
            return binInt(depth, binSize, depthMax);
        }
    }
}
