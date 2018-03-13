package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.Feature;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.report.GATKReport;
import org.broadinstitute.hellbender.utils.report.GATKReportTable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class GATKWGSMetricsReport {

    private static final String GATK_REPORT_TABLE_READ_COUNTS = "ReadCounts";
    private static final String GATK_REPORT_TABLE_READ_AVERAGES = "ReadAverages";
    private static final String GATK_REPORT_TABLE_REFERENCE_COUNTS = "ReferenceCounts";

    private static final String GATK_REPORT_COLUMN_COVERAGE = "coverage";
    private static final String GATK_REPORT_COLUMN_COUNT = "count";
    private static final String GATK_REPORT_COLUMN_PROBABILITY = "probability";
    private static final String GATK_REPORT_COLUMN_EXPECTED = "expected";
    private static final String GATK_REPORT_COLUMN_POISSON = "poisson";
    private static final String GATK_REPORT_COLUMN_AVERAGE = "average";
    private static final String GATK_REPORT_COLUMN_MEDIAN = "median";
    private static final String GATK_REPORT_COLUMN_VARIANCE = "variance";
    private static final String GATK_REPORT_COLUMN_DISPERSION = "dispersion";

    private final GATKReport gatkReport;
    private final GATKReportTable readCountsReportTable;
    private final GATKReportTable readAveragesReportTable;
    private final GATKReportTable referenceCountsReportTable;

    private final Map<GATKReportTable, GATKReportIndex> tableIndexes;

    public GATKWGSMetricsReport(final List<ReferenceStratifier<?>> referenceStratifiers,
                                final List<FeatureStratifier<? extends Feature, ?>> featureStratifiers,
                                final List<ReadStratifier<?>> readStratifiers) {
        gatkReport = new GATKReport();

        readCountsReportTable =
                new GATKReportTable(GATK_REPORT_TABLE_READ_COUNTS, GATK_REPORT_TABLE_READ_COUNTS, 0);
        readAveragesReportTable =
                new GATKReportTable(GATK_REPORT_TABLE_READ_AVERAGES, GATK_REPORT_TABLE_READ_AVERAGES, 0);
        referenceCountsReportTable =
                new GATKReportTable(GATK_REPORT_TABLE_REFERENCE_COUNTS, GATK_REPORT_TABLE_REFERENCE_COUNTS, 0);

        gatkReport.addTable(readCountsReportTable);
        referenceStratifiers.forEach(strat -> addTableStratifier(readCountsReportTable, strat));
        featureStratifiers.forEach(strat -> addTableStratifier(readCountsReportTable, strat));
        readStratifiers.forEach(strat -> addTableStratifier(readCountsReportTable, strat));
        readCountsReportTable.addColumn(GATK_REPORT_COLUMN_COVERAGE, "%d");
        readCountsReportTable.addColumn(GATK_REPORT_COLUMN_COUNT, "%d");
        readCountsReportTable.addColumn(GATK_REPORT_COLUMN_EXPECTED, "%d");
        readCountsReportTable.addColumn(GATK_REPORT_COLUMN_PROBABILITY, "%.8f");
        readCountsReportTable.addColumn(GATK_REPORT_COLUMN_POISSON, "%.8f");

        gatkReport.addTable(readAveragesReportTable);
        referenceStratifiers.forEach(strat -> addTableStratifier(readAveragesReportTable, strat));
        featureStratifiers.forEach(strat -> addTableStratifier(readAveragesReportTable, strat));
        readStratifiers.forEach(strat -> addTableStratifier(readAveragesReportTable, strat));
        readAveragesReportTable.addColumn(GATK_REPORT_COLUMN_COUNT, "%d");
        readAveragesReportTable.addColumn(GATK_REPORT_COLUMN_MEDIAN, "%d");
        readAveragesReportTable.addColumn(GATK_REPORT_COLUMN_AVERAGE, "%.8f");
        readAveragesReportTable.addColumn(GATK_REPORT_COLUMN_VARIANCE, "%.8f");
        readAveragesReportTable.addColumn(GATK_REPORT_COLUMN_DISPERSION, "%.8f");

        gatkReport.addTable(referenceCountsReportTable);
        referenceStratifiers.forEach(strat -> addTableStratifier(referenceCountsReportTable, strat));
        featureStratifiers.forEach(strat -> addTableStratifier(referenceCountsReportTable, strat));
        referenceCountsReportTable.addColumn(GATK_REPORT_COLUMN_COUNT, "%d");

        tableIndexes = new HashMap<>();
        initIndexes();
    }

    // Loads counts, but updateAggregateStats must still be called.
    public GATKWGSMetricsReport(final File file) {
        final GATKReport inputReport = new GATKReport(file);

        gatkReport = inputReport.headerCopy(); // Use header copy. We'll make sure stratifier keys are set correctly.
        readCountsReportTable = gatkReport.getTable(GATK_REPORT_TABLE_READ_COUNTS);
        readAveragesReportTable = gatkReport.getTable(GATK_REPORT_TABLE_READ_AVERAGES);
        referenceCountsReportTable = gatkReport.getTable(GATK_REPORT_TABLE_REFERENCE_COUNTS);

        tableIndexes = new HashMap<>();
        initIndexes();

        // Copy over to stratifier keys.
        combineCounts(readCountsReportTable, inputReport.getTable(GATK_REPORT_TABLE_READ_COUNTS));
        combineCounts(referenceCountsReportTable, inputReport.getTable(GATK_REPORT_TABLE_REFERENCE_COUNTS));
    }

    private void initIndexes() {
        initIndex(readCountsReportTable);
        initIndex(readAveragesReportTable);
        initIndex(referenceCountsReportTable);
    }

    private void initIndex(final GATKReportTable table) {
        final int countColumnIndex = table.getColumnIndex(GATK_REPORT_COLUMN_COUNT);
        final GATKReportIndex index = GATKReportIndex.newInstance(table, countColumnIndex - 1);
        tableIndexes.put(table, index);
    }

    // Combines counts, but updateAggregateStats must still be called.
    public void combineCounts(final GATKWGSMetricsReport report) {
        combineCounts(readCountsReportTable, report.readCountsReportTable);
        combineCounts(referenceCountsReportTable, report.referenceCountsReportTable);
    }

    public void incrementReferenceCount(final StratifierKey key) {
        increment(referenceCountsReportTable, key, GATK_REPORT_COLUMN_COUNT, 1);
    }

    public void incrementReadCoverageCount(final StratifierKey key, final int coverage) {
        final StratifierKey coverageKey = new StratifierKey(key);
        coverageKey.add((long) coverage); // When integers are read report files, they are expanded into longs
        increment(readCountsReportTable, coverageKey, GATK_REPORT_COLUMN_COUNT, 1);
    }

    public void updateAggregateStats() {
        final int referenceCountsColumnIndex = referenceCountsReportTable.getColumnIndex(GATK_REPORT_COLUMN_COUNT);
        final int coverageColumnIndex = readCountsReportTable.getColumnIndex(GATK_REPORT_COLUMN_COVERAGE);
        final int countColumnIndex = readCountsReportTable.getColumnIndex(GATK_REPORT_COLUMN_COUNT);
        final int expectedColumnIndex = readCountsReportTable.getColumnIndex(GATK_REPORT_COLUMN_EXPECTED);
        final int probabilityColumnIndex = readCountsReportTable.getColumnIndex(GATK_REPORT_COLUMN_PROBABILITY);
        final int poissonColumnIndex = readCountsReportTable.getColumnIndex(GATK_REPORT_COLUMN_POISSON);
        final int totalColumnIndex = readAveragesReportTable.getColumnIndex(GATK_REPORT_COLUMN_COUNT);
        final int medianColumnIndex = readAveragesReportTable.getColumnIndex(GATK_REPORT_COLUMN_MEDIAN);
        final int averageColumnIndex = readAveragesReportTable.getColumnIndex(GATK_REPORT_COLUMN_AVERAGE);
        final int varianceColumnIndex = readAveragesReportTable.getColumnIndex(GATK_REPORT_COLUMN_VARIANCE);
        final int dispersionColumnIndex = readAveragesReportTable.getColumnIndex(GATK_REPORT_COLUMN_DISPERSION);
        final Map<StratifierKey, SortedSet<StratifierKey>> readCoverageKeys = getReadCoverageKeys();

        for (final Map.Entry<StratifierKey, SortedSet<StratifierKey>> stratifierKeySetEntry
                : readCoverageKeys.entrySet()) {

            final StratifierKey readStratifierKey = stratifierKeySetEntry.getKey();

            final SortedSet<StratifierKey> coverageStratifierKeys = stratifierKeySetEntry.getValue();

            final StratifierKey referenceStratifierKey = new StratifierKey();
            for (int i = 0; i < referenceCountsColumnIndex; i++) {
                referenceStratifierKey.add(readStratifierKey.get(i));
            }

            final int referenceRowIndex = getRowIndex(referenceCountsReportTable, referenceStratifierKey);
            final long referenceBaseCount =
                    getLong(referenceCountsReportTable, referenceRowIndex, referenceCountsColumnIndex);

            long totalPileCount = 0;
            for (final StratifierKey coverageStratifierKey : coverageStratifierKeys) {
                final int rowIndex = getRowIndex(readCountsReportTable, coverageStratifierKey);
                final long coverage = getLong(readCountsReportTable, rowIndex, coverageColumnIndex);
                final long count = getLong(readCountsReportTable, rowIndex, countColumnIndex);

                totalPileCount += count * coverage;
            }

            final double averageCoverage = safeDivide(totalPileCount, referenceBaseCount);
            final PoissonDistribution poissonDistribution =
                    new PoissonDistribution(null, averageCoverage,
                            PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);

            long readBaseCount = 0;
            double sumSquaredDiffCoverage = 0;
            long medianCoverageCounter = referenceBaseCount;
            long medianCoverage = -1;
            for (final StratifierKey coverageStratifierKey : coverageStratifierKeys) {
                final int rowIndex = getRowIndex(readCountsReportTable, coverageStratifierKey);
                final long coverage = getLong(readCountsReportTable, rowIndex, coverageColumnIndex);
                final long count = getLong(readCountsReportTable, rowIndex, countColumnIndex);

                final double probability = safeDivide(count, referenceBaseCount);
                final double poisson = poissonDistribution.probability((int)coverage);
                final long expected = (long)(poisson * referenceBaseCount);
                readCountsReportTable.set(rowIndex, probabilityColumnIndex, probability);
                readCountsReportTable.set(rowIndex, poissonColumnIndex, poisson);
                readCountsReportTable.set(rowIndex, expectedColumnIndex, expected);

                if (coverage != 0) {
                    // Recalculate zero coverage count later below.
                    readBaseCount += count;

                    // Add zero coverage count later below.
                    sumSquaredDiffCoverage += count * Math.pow(coverage - averageCoverage, 2);
                }

                // The coverage stratifier keys will be in reverse order. Keep subtracting until we pass the median.
                medianCoverageCounter -= count;
                if (medianCoverage < 0 && medianCoverageCounter <= (referenceBaseCount / 2))
                    medianCoverage = coverage;
            }

            if (medianCoverage < 0)
                medianCoverage = 0;

            // All bases accounted for? Then don't add a row that says zero coverage has a zero count.
            if (referenceBaseCount != readBaseCount) {
                final StratifierKey zeroStratifierKey = new StratifierKey(readStratifierKey);
                zeroStratifierKey.add(0L); // When integers are read report files, they are expanded into longs
                final int zeroRowIndex = getRowIndex(readCountsReportTable, zeroStratifierKey);
                final long zeroBaseCount = referenceBaseCount - readBaseCount;
                final double zeroProbability = safeDivide(zeroBaseCount, referenceBaseCount);
                final double zeroPoisson = poissonDistribution.probability(0);
                final long zeroExpected = (long)(zeroPoisson * referenceBaseCount);
                readCountsReportTable.set(zeroRowIndex, countColumnIndex, zeroBaseCount);
                readCountsReportTable.set(zeroRowIndex, expectedColumnIndex, zeroExpected);
                readCountsReportTable.set(zeroRowIndex, probabilityColumnIndex, zeroProbability);
                readCountsReportTable.set(zeroRowIndex, poissonColumnIndex, zeroPoisson);

                sumSquaredDiffCoverage += zeroBaseCount * Math.pow(averageCoverage, 2);
            }

            final double varianceCoverage = safeDivide(sumSquaredDiffCoverage, referenceBaseCount);
            final double dispersionCoverage = safeDivide(varianceCoverage, averageCoverage);

            final int averageRowIndex = getRowIndex(readAveragesReportTable, readStratifierKey);
            readAveragesReportTable.set(averageRowIndex, totalColumnIndex, totalPileCount);
            readAveragesReportTable.set(averageRowIndex, medianColumnIndex, medianCoverage);
            readAveragesReportTable.set(averageRowIndex, averageColumnIndex, averageCoverage);
            readAveragesReportTable.set(averageRowIndex, varianceColumnIndex, varianceCoverage);
            readAveragesReportTable.set(averageRowIndex, dispersionColumnIndex, dispersionCoverage);
        }
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

    /**
     * Returns the coverage keys stratified by coverage.
     * The values are sets of stratifier keys IN REVERSE ORDER. The reverse order is useful for counting down to zero
     * coverage while calculating the median.
     */
    private Map<StratifierKey, SortedSet<StratifierKey>> getReadCoverageKeys() {
        final Map<StratifierKey, SortedSet<StratifierKey>> coverageKeys = new LinkedHashMap<>();
        final int rowCount = readCountsReportTable.getNumRows();
        final int coverageColumnIndex = readCountsReportTable.getColumnIndex(GATK_REPORT_COLUMN_COVERAGE);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final StratifierKey readStratifierKey = new StratifierKey();

            for (int columnIndex = 0; columnIndex < coverageColumnIndex; columnIndex++) {
                readStratifierKey.add(readCountsReportTable.get(rowIndex, columnIndex));
            }

            final StratifierKey coverageStratifierKey = new StratifierKey(readStratifierKey);
            coverageStratifierKey.add(readCountsReportTable.get(rowIndex, coverageColumnIndex));

            final SortedSet<StratifierKey> initial = new TreeSet<>(Collections.reverseOrder());
            initial.add(coverageStratifierKey);
            coverageKeys.merge(readStratifierKey, initial, (acc, inc) -> {
                acc.addAll(inc);
                return acc;
            });
        }
        return coverageKeys;
    }

    private void combineCounts(final GATKReportTable acc, final GATKReportTable inc) {
        if (!acc.isSameFormat(inc)) {
            throw new GATKException(
                    String.format("Tables are not the same format: %s / %s", acc.getTableName(), inc.getTableName()));
        }
        final int countColumnIndex = inc.getColumnIndex(GATK_REPORT_COLUMN_COUNT);
        final int numRows = inc.getNumRows();
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            final StratifierKey key = new StratifierKey();
            for (int columnIndex = 0; columnIndex < countColumnIndex; columnIndex++) {
                key.add(inc.get(rowIndex, columnIndex));
            }
            final long amount = getLong(inc, rowIndex, countColumnIndex);
            increment(acc, key, GATK_REPORT_COLUMN_COUNT, amount);
        }
    }

    private int getRowIndex(final GATKReportTable table, final StratifierKey key) {
        final Object[] columnValues = key.toArray();
        final GATKReportIndex index = tableIndexes.get(table);
        return index.findRowByData(key, columnValues);
    }

    public void increment(final GATKReportTable table,
                          final StratifierKey key,
                          final String columnName,
                          final long amount) {
        final int rowIndex = getRowIndex(table, key);
        final int columnIndex = table.getColumnIndex(columnName);
        final Object count = table.get(rowIndex, columnIndex);
        if (count == null) {
            table.set(rowIndex, columnIndex, amount);
        } else {
            table.set(rowIndex, columnIndex, ((Number) count).longValue() + amount);
        }
    }

    private static void addTableStratifier(final GATKReportTable table, final Stratifier<?, ?> stratifier) {
        table.addColumn(stratifier.getColumnName(), stratifier.getColumnFormat());
    }

    private static long getLong(final GATKReportTable table, final int rowIndex, final int columnIndex) {
        return ((Number) table.get(rowIndex, columnIndex)).longValue();
    }

    private static double safeDivide(final double numerator, final double denominator) {
        return denominator == 0 ? 0 : numerator / denominator;
    }
}
