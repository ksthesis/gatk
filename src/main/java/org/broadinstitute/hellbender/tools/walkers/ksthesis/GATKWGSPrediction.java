package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.utils.report.GATKReport;
import org.broadinstitute.hellbender.utils.report.GATKReportTable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

@SuppressWarnings("WeakerAccess")
public class GATKWGSPrediction {

    private final GATKReport gatkReport;
    private final GATKReportTable predictionTable;

    private static final String GATK_REPORT_TABLE_PREDICTION = "Predictions";
    private static final String GATK_REPORT_COLUMN_INPUT = "input";
    private static final String GATK_REPORT_COLUMN_COVERAGE = "coverage";
    private static final String GATK_REPORT_COLUMN_PROBABILITY = "probability";
    private static final String AVERAGE_INPUT = "-average-";

    private final int inputColumnIndex;
    private final int coverageColumnIndex;
    private final int probabilityColumnIndex;

    private final long pileup;
    private final int maxCoverage;
    private final double[] probabilityDistributions;

    private int numDistributions = 0;

    public GATKWGSPrediction(final long pileup, final int maxCoverage) {
        this.pileup = pileup;
        this.maxCoverage = maxCoverage;
        this.probabilityDistributions = new double[maxCoverage];

        gatkReport = new GATKReport();
        predictionTable = new GATKReportTable(GATK_REPORT_TABLE_PREDICTION, GATK_REPORT_TABLE_PREDICTION, 0);

        gatkReport.addTable(predictionTable);

        predictionTable.addColumn(GATK_REPORT_COLUMN_INPUT, "%s");
        predictionTable.addColumn(GATK_REPORT_COLUMN_COVERAGE, "%d");
        predictionTable.addColumn(GATK_REPORT_COLUMN_PROBABILITY, "%.8f");

        inputColumnIndex = predictionTable.getColumnIndex(GATK_REPORT_COLUMN_INPUT);
        coverageColumnIndex = predictionTable.getColumnIndex(GATK_REPORT_COLUMN_COVERAGE);
        probabilityColumnIndex = predictionTable.getColumnIndex(GATK_REPORT_COLUMN_PROBABILITY);
    }

    public void addMetrics(final File file) {
        final GATKWGSMetricsReport report = new GATKWGSMetricsReport(file);
        report.updateAggregateStats();
        final double[] prediction = report.getPrediction(pileup, maxCoverage);

        final String input = file.getName();

        double cumulative = 0;
        for (int coverage = 0; coverage < maxCoverage; coverage++) {
            final double probability = prediction[coverage];
            addMetric(input, coverage, probability);
            probabilityDistributions[coverage] += probability;
            cumulative += probability;
        }
        addMetric(input, maxCoverage, Math.max(0, 1 - cumulative));

        numDistributions += 1;
    }

    public void addAverageMetrics() {
        double cumulative = 0;
        for (int coverage = 0; coverage < maxCoverage; coverage++) {
            final double probability = probabilityDistributions[coverage] / numDistributions;
            addMetric(AVERAGE_INPUT, coverage, probability);
            cumulative += probability;
        }
        addMetric(AVERAGE_INPUT, maxCoverage, Math.max(0, 1 - cumulative));
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

    private void addMetric(String input, int coverage, double probability) {
        final StratifierKey stratifierKey = new StratifierKey();
        stratifierKey.add(input);
        stratifierKey.add(coverage);

        final int rowIndex = predictionTable.addRowID(stratifierKey, false);
        predictionTable.set(rowIndex, inputColumnIndex, input);
        predictionTable.set(rowIndex, coverageColumnIndex, coverage);
        predictionTable.set(rowIndex, probabilityColumnIndex, probability);
    }
}
