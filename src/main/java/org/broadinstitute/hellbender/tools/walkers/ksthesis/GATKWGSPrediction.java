package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.report.GATKReport;
import org.broadinstitute.hellbender.utils.report.GATKReportTable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class GATKWGSPrediction {

    private final GATKReport gatkReport;
    private final GATKReportTable coverageTable;
    private final GATKReportTable averageTable;

    private static final String GATK_REPORT_TABLE_COVERAGE = "Coverage";
    private static final String GATK_REPORT_TABLE_AVERAGE = "Average";
    private static final String GATK_REPORT_COLUMN_INPUT = "input";
    private static final String GATK_REPORT_COLUMN_COVERAGE = "coverage";
    private static final String GATK_REPORT_COLUMN_PROBABILITY = "probability";
    private static final String GATK_REPORT_COLUMN_REFERENCE_COUNT = "reference_count";
    private static final String GATK_REPORT_COLUMN_PREDICT_PILEUP = "predict_pileup";
    private static final String GATK_REPORT_COLUMN_PREDICT_AVERAGE = "predict_average";
    private static final String GATK_REPORT_COLUMN_ORIGINAL_PILEUP = "original_pileup";
    private static final String GATK_REPORT_COLUMN_ORIGINAL_AVERAGE = "original_average";
    private static final String GATK_REPORT_COLUMN_SCALE = "scale";
    private static final String AVERAGE_INPUT = "-average-";

    private final int coverageInputColumnIndex;
    private final int coverageColumnIndex;
    private final int probabilityColumnIndex;
    private final int averageInputColumnIndex;
    private final int referenceCountColumnIndex;
    private final int predictPileupColumnIndex;
    private final int predictAverageColumnIndex;
    private final int originalPileupColumnIndex;
    private final int originalAverageColumnIndex;
    private final int scaleColumnIndex;

    private final long pileup;
    private final int maxCoverage;

    private final List<CoveragePrediction> predictions = new ArrayList<>();

    public GATKWGSPrediction(final long pileup, final int maxCoverage) {
        this.pileup = pileup;
        this.maxCoverage = maxCoverage;

        gatkReport = new GATKReport();
        coverageTable = new GATKReportTable(GATK_REPORT_TABLE_COVERAGE, GATK_REPORT_TABLE_COVERAGE, 0);

        gatkReport.addTable(coverageTable);

        coverageTable.addColumn(GATK_REPORT_COLUMN_INPUT, "%s");
        coverageTable.addColumn(GATK_REPORT_COLUMN_COVERAGE, "%d");
        coverageTable.addColumn(GATK_REPORT_COLUMN_PROBABILITY, "%.8f");

        coverageInputColumnIndex = coverageTable.getColumnIndex(GATK_REPORT_COLUMN_INPUT);
        coverageColumnIndex = coverageTable.getColumnIndex(GATK_REPORT_COLUMN_COVERAGE);
        probabilityColumnIndex = coverageTable.getColumnIndex(GATK_REPORT_COLUMN_PROBABILITY);

        averageTable = new GATKReportTable(GATK_REPORT_TABLE_AVERAGE, GATK_REPORT_TABLE_AVERAGE, 0);

        gatkReport.addTable(averageTable);

        averageTable.addColumn(GATK_REPORT_COLUMN_INPUT, "%s");
        averageTable.addColumn(GATK_REPORT_COLUMN_REFERENCE_COUNT, "%d");
        averageTable.addColumn(GATK_REPORT_COLUMN_PREDICT_PILEUP, "%d");
        averageTable.addColumn(GATK_REPORT_COLUMN_PREDICT_AVERAGE, "%.8f");
        averageTable.addColumn(GATK_REPORT_COLUMN_ORIGINAL_PILEUP, "%d");
        averageTable.addColumn(GATK_REPORT_COLUMN_ORIGINAL_AVERAGE, "%.8f");
        averageTable.addColumn(GATK_REPORT_COLUMN_SCALE, "%.8f");

        averageInputColumnIndex = averageTable.getColumnIndex(GATK_REPORT_COLUMN_INPUT);
        referenceCountColumnIndex = averageTable.getColumnIndex(GATK_REPORT_COLUMN_REFERENCE_COUNT);
        predictPileupColumnIndex = averageTable.getColumnIndex(GATK_REPORT_COLUMN_PREDICT_PILEUP);
        predictAverageColumnIndex = averageTable.getColumnIndex(GATK_REPORT_COLUMN_PREDICT_AVERAGE);
        originalPileupColumnIndex = averageTable.getColumnIndex(GATK_REPORT_COLUMN_ORIGINAL_PILEUP);
        originalAverageColumnIndex = averageTable.getColumnIndex(GATK_REPORT_COLUMN_ORIGINAL_AVERAGE);
        scaleColumnIndex = averageTable.getColumnIndex(GATK_REPORT_COLUMN_SCALE);
    }

    public void addMetrics(final File file) {
        final GATKWGSMetricsReport report = new GATKWGSMetricsReport(file);
        report.updateAggregateStats();
        final CoveragePrediction coveragePrediction = report.getPrediction(pileup, maxCoverage);
        predictions.add(coveragePrediction);
        final String input = file.getName();
        addPrediction(input, coveragePrediction);
    }

    public void addAverageMetrics() {
        final int predictionCount = predictions.size();
        if (predictionCount == 0) {
            return;
        }

        final CoveragePrediction firstPrediction = predictions.get(0);
        final int maxCoverage = firstPrediction.getMaxCoverage();

        final double[] averageProbabilities = new double[maxCoverage];
        long totalPileup = 0;
        for (final CoveragePrediction prediction: predictions) {
            for (int coverage = 0; coverage < maxCoverage; coverage++) {
                averageProbabilities[coverage] += prediction.getProbability(coverage) / predictionCount;
            }
            totalPileup += prediction.getOriginalPileup();
        }
        final long referenceCount = firstPrediction.getReferenceCount();
        final long predictPileup = firstPrediction.getPredictPileup();
        final long averagePileup = totalPileup / predictionCount;
        final CoveragePrediction averagePrediction =
                new CoveragePrediction(referenceCount, predictPileup, averagePileup, averageProbabilities);
        addPrediction(AVERAGE_INPUT, averagePrediction);
    }

    public void print(final File file) {
        try {
            try (final PrintStream outTable = new PrintStream(file)) {
                gatkReport.print(outTable, GATKReportTable.Sorting.SORT_BY_ROW);
            }
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void addPrediction(final String input, final CoveragePrediction coveragePrediction) {
        addAverage(input, coveragePrediction);

        for (int coverage = 0; coverage < coveragePrediction.getMaxCoverage(); coverage++) {
            addCoverage(input, coverage, coveragePrediction.getProbability(coverage));
        }
        addCoverage(input, coveragePrediction.getMaxCoverage(), coveragePrediction.getResidualProbability());
    }

    private void addAverage(final String input, final CoveragePrediction coveragePrediction) {
        final int rowIndex = averageTable.addRowID(input, false);
        averageTable.set(rowIndex, averageInputColumnIndex, input);
        averageTable.set(rowIndex, referenceCountColumnIndex, coveragePrediction.getReferenceCount());
        averageTable.set(rowIndex, predictPileupColumnIndex, coveragePrediction.getPredictPileup());
        averageTable.set(rowIndex, predictAverageColumnIndex, coveragePrediction.getPredictAverage());
        averageTable.set(rowIndex, originalPileupColumnIndex, coveragePrediction.getOriginalPileup());
        averageTable.set(rowIndex, originalAverageColumnIndex, coveragePrediction.getOriginalAverage());
        averageTable.set(rowIndex, scaleColumnIndex, coveragePrediction.getScale());
    }

    private void addCoverage(final String input, final int coverage, final double probability) {
        final StratifierKey stratifierKey = new StratifierKey();
        stratifierKey.add(input);
        stratifierKey.add(coverage);

        final int rowIndex = coverageTable.addRowID(stratifierKey, false);
        coverageTable.set(rowIndex, coverageInputColumnIndex, input);
        coverageTable.set(rowIndex, coverageColumnIndex, coverage);
        coverageTable.set(rowIndex, probabilityColumnIndex, probability);
    }
}
