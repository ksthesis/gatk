package org.broadinstitute.hellbender.tools.walkers.ksthesis;

@SuppressWarnings("WeakerAccess")
public class CoveragePrediction {
    private final long referenceCount;
    private final long predictPileup;
    private final double predictAverage;
    private final long originalPileup;
    private final double originalAverage;
    private final double scale;
    private final double[] probabilities;
    private final double residualProbability;

    public CoveragePrediction(
            final long referenceCount,
            final long predictPileup,
            final long originalPileup,
            final double[] probabilities) {
        this.referenceCount = referenceCount;
        this.predictPileup = predictPileup;
        this.predictAverage = predictPileup / (double) referenceCount;
        this.originalPileup = originalPileup;
        this.originalAverage = originalPileup / (double) referenceCount;
        this.scale = predictPileup / (double) originalPileup;
        this.probabilities = probabilities;
        double cumulative = 0;
        for (final double probability : probabilities) {
            cumulative += probability;
        }
        residualProbability = Math.max(0, 1 - cumulative);
    }

    public long getReferenceCount() {
        return referenceCount;
    }

    public long getPredictPileup() {
        return predictPileup;
    }

    public double getPredictAverage() {
        return predictAverage;
    }

    public long getOriginalPileup() {
        return originalPileup;
    }

    public double getOriginalAverage() {
        return originalAverage;
    }

    public double getScale() {
        return scale;
    }

    public int getMaxCoverage() {
        return probabilities.length;
    }

    public double getProbability(final int coverage) {
        return probabilities[coverage];
    }

    public double getResidualProbability() {
        return residualProbability;
    }
}
