package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.Histogram;
import htsjdk.tribble.Feature;
import htsjdk.tribble.bed.BEDFeature;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.QCProgramGroup;
import org.broadinstitute.hellbender.engine.*;
import org.broadinstitute.hellbender.engine.filters.MappingQualityReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.utils.pileup.PileupElement;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.report.GATKReport;
import org.broadinstitute.hellbender.utils.report.GATKReportTable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * TODO
 * <p>
 * <p>
 * TODO
 * </p>
 * <p>
 * <h3>Input</h3>
 * <p>
 * A BAM file.
 * </p>
 * <p>
 * <h3>Output</h3>
 * <p>
 * A text file.
 * </p>
 * <p>
 * <h3>Usage example</h3>
 * <pre>
 * ./gatk-launch GATKWGSMetrics \
 *   -R reference.fasta \
 *   -I your_data.bam \
 *   -L chr1:257-275 \
 *   -O output_file_name
 * </pre>
 */
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
@CommandLineProgramProperties(
        summary = "TODO",
        oneLineSummary = "TODO",
        programGroup = QCProgramGroup.class)
public final class GATKWGSMetrics extends LocusWalker {

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME,
            doc = "Output file (if not provided, defaults to STDOUT).",
            optional = true)
    public File outFile = null;

    @Argument(fullName = "gcLeading",
            shortName = "GCL",
            doc = "GC window leading bases, default 50",
            optional = true)
    private final int gcWindowLeadingBases = 50;

    @Argument(fullName = "gcTrailing",
            shortName = "GCT",
            doc = "GC window trailing bases, default 250",
            optional = true)
    private final int gcWindowTrailingBases = 250;

    @Argument(fullName = "gcBin",
            shortName = "GCB",
            doc = "GC content bin, default 2",
            optional = true)
    private final int gcBin = 2;

    @Argument(fullName = "insertSizeMax",
            shortName = "ISM",
            doc = "Insert size maximum, default 600",
            optional = true)
    private final int insertSizeMax = 600;

    @Argument(fullName = "insertSizeBin",
            shortName = "ISB",
            doc = "Insert size bin, default 100",
            optional = true)
    private final int insertSizeBin = 100;

    @Argument(fullName = "mapabilityBin",
            shortName = "MAPB",
            doc = "Mapability bin, default 20",
            optional = true)
    private final int mapabilityBin = 20;

    @Argument(fullName = "mapability", shortName = "M", doc = "mapability BED file", optional = true)
    public FeatureInput<BEDFeature> mapabilityBed;

    @Argument(fullName = "readGroupSplits", shortName = "RGS", doc = "read group splits", optional = true)
    public Integer readGroupSplits = 0;

    private final int MINIMUM_MAPPING_QUALITY = 20;
    private final int MINIMUM_BASE_QUALITY = 20;
    private final int COVERAGE_CAP = 250;
    private final long[] histogramArray = new long[COVERAGE_CAP + 1];

    // These are the default read filters from samtools
    @Override
    public List<ReadFilter> getDefaultReadFilters() {
        final List<ReadFilter> defaultFilters = super.getDefaultReadFilters();
        defaultFilters.add(ReadFilterLibrary.MAPPED);
        defaultFilters.add(ReadFilterLibrary.NOT_DUPLICATE);
        defaultFilters.add(ReadFilterLibrary.NOT_SECONDARY_ALIGNMENT);
        defaultFilters.add(ReadFilterLibrary.PASSES_VENDOR_QUALITY_CHECK);
        defaultFilters.add(new MappingQualityReadFilter(MINIMUM_MAPPING_QUALITY));
        defaultFilters.add(new MappedPairFilter());
        return defaultFilters;
    }

    @Override
    public boolean emitEmptyLoci() {
        return true;
    }

    @Override
    public boolean requiresReference() {
        return true;
    }

    private final GATKReport gatkReport = new GATKReport();
    private final GATKReportTable histogramReportTable =
            new GATKReportTable("Histogram", "Histogram", 0, GATKReportTable.Sorting.SORT_BY_ROW);
    private final GATKReportTable averageReportTable =
            new GATKReportTable("Averages", "Averages", 0, GATKReportTable.Sorting.SORT_BY_ROW);
    private final GATKReportTable referenceReportTable =
            new GATKReportTable("ReferenceCounts", "ReferenceCounts", 0, GATKReportTable.Sorting.SORT_BY_ROW);

    private static final String GATK_REPORT_COLUMN_COVERAGE = "coverage";
    private static final String GATK_REPORT_COLUMN_COUNT = "count";
    private static final String GATK_REPORT_COLUMN_PROBABILITY = "probability";
    private static final String GATK_REPORT_COLUMN_AVERAGE = "average";

    private final List<ReferenceStratifier> referenceStratifiers = new ArrayList<>();
    private final List<FeatureStratifier<? extends Feature>> featureStratifiers = new ArrayList<>();
    private final List<ReadStratifier> readStratifiers = new ArrayList<>();

    private final Map<StratifierKey, Set<StratifierKey>> coverageKeys = new LinkedHashMap<>();

    @Override
    public void onTraversalStart() {
        referenceStratifiers.add(new GCStratifier(gcBin, gcWindowLeadingBases, gcWindowTrailingBases));

        featureStratifiers.add(new MapabilityStratifier(mapabilityBin, mapabilityBed));

        readStratifiers.add(new DebugReadGroupStratifier(readGroupSplits));
        readStratifiers.add(new AbsTLenStratifier(insertSizeBin, insertSizeMax));

        gatkReport.addTable(histogramReportTable);
        referenceStratifiers.forEach(strat -> addTableStratifier(histogramReportTable, strat));
        featureStratifiers.forEach(strat -> addTableStratifier(histogramReportTable, strat));
        readStratifiers.forEach(strat -> addTableStratifier(histogramReportTable, strat));
        histogramReportTable.addColumn(GATK_REPORT_COLUMN_COVERAGE, "%d");
        histogramReportTable.addColumn(GATK_REPORT_COLUMN_COUNT, "%d");
        histogramReportTable.addColumn(GATK_REPORT_COLUMN_PROBABILITY, "%f");

        gatkReport.addTable(averageReportTable);
        referenceStratifiers.forEach(strat -> addTableStratifier(averageReportTable, strat));
        featureStratifiers.forEach(strat -> addTableStratifier(averageReportTable, strat));
        readStratifiers.forEach(strat -> addTableStratifier(averageReportTable, strat));
        averageReportTable.addColumn(GATK_REPORT_COLUMN_AVERAGE, "%f");

        gatkReport.addTable(referenceReportTable);
        referenceStratifiers.forEach(strat -> addTableStratifier(referenceReportTable, strat));
        featureStratifiers.forEach(strat -> addTableStratifier(referenceReportTable, strat));
        referenceReportTable.addColumn(GATK_REPORT_COLUMN_COUNT, "%d");
    }

    private static void addTableStratifier(final GATKReportTable table, final Stratifier stratifier) {
        table.addColumn(stratifier.getColumnName(), stratifier.getColumnFormat());
    }

    @Override
    public void apply(final AlignmentContext context, final ReferenceContext ref, final FeatureContext featureContext) {
        final byte base = ref.getBase();

        if ('N' == base)
            return;

        final Iterable<PileupElement> iterable = () -> context.getBasePileup().iterator();
        final Set<String> readNames = StreamSupport.stream(iterable.spliterator(), false)
                .filter(pe -> pe.getQual() >= MINIMUM_BASE_QUALITY)
                .map(pe -> pe.getRead().getName())
                .collect(Collectors.toSet());
        final int depth = Math.min(readNames.size(), COVERAGE_CAP);

        final StratifierKey referenceStratiferKey = new StratifierKey();

        for (final ReferenceStratifier referenceStratifier : referenceStratifiers) {
            final Object stratification = referenceStratifier.getStratification(ref);
            referenceStratiferKey.add(stratification);
        }

        for (final FeatureStratifier<? extends Feature> featureStratifier : featureStratifiers) {
            final Object stratification = featureStratifier.getStratification(featureContext);
            referenceStratiferKey.add(stratification);
        }

        // Make an empty collection here.
        final List<StratifierKey> stratPileup = new ArrayList<>();
        for (final PileupElement pileupElement : context.getBasePileup()) {
            final GATKRead read = pileupElement.getRead();

            // Concat all the different strats.
            final StratifierKey readStratifierKey = new StratifierKey(referenceStratiferKey);
            for (final ReadStratifier readStratifier : readStratifiers) {
                final Object stratification = readStratifier.getStratification(read);
                readStratifierKey.add(stratification);
            }

            // Add a new entry into the collection.
            stratPileup.add(readStratifierKey);
        }

        // For each concat, get a total number of entries for that concat within the collection.
        final Map<StratifierKey, Integer> stratCounts = new LinkedHashMap<>();
        for (final StratifierKey strat : stratPileup) {
            stratCounts.merge(strat, 1, (acc, inc) -> acc + inc);
        }

        // Find the histogram for that concat.
        // For that histogram, increment the depth using the total number. ex: 2 reads were found with GC 50-51 and TLEN 300-399
        for (final Map.Entry<StratifierKey, Integer> entry : stratCounts.entrySet()) {
            final StratifierKey stratKey = entry.getKey();
            final int stratDepth = Math.min(entry.getValue(), COVERAGE_CAP);

            final StratifierKey coverageKey = new StratifierKey(stratKey);
            coverageKey.add(stratDepth);

            final Set<StratifierKey> coverageKeySet = new LinkedHashSet<>();
            coverageKeySet.add(coverageKey);
            coverageKeys.merge(stratKey, coverageKeySet, (acc, inc) -> {
                acc.addAll(inc);
                return acc;
            });
            increment(histogramReportTable, coverageKey, GATK_REPORT_COLUMN_COUNT);
        }

        // Move on to the next location.
        // TODONE: Q: What to do about zeros coverage? A: What we're doing here.
        // 1) Create a count of locations that match some composite reference stratifiers
        // 2) Later, when counting the _read_ stratifiers, count how many locations had >=1 coverage.
        // 3) The difference between reference-stratfiers coverage and reference-stratfiers-plus-read-stratifiers with >= 1 coverage is the number of 0 coverage.
        increment(referenceReportTable, referenceStratiferKey, GATK_REPORT_COLUMN_COUNT);

        histogramArray[depth]++;
    }

    private static int getRowIndex(final GATKReportTable table, final StratifierKey key) {
        final Object[] columnValues = key.toArray();
        int rowIndex = table.findRowByData(columnValues);
        if (rowIndex < 0) {
            rowIndex = table.addRowID(key, false);
            for (int i = 0; i < key.size(); i++) {
                table.set(rowIndex, i, key.get(i));
            }
        }
        return rowIndex;
    }

    public static void increment(final GATKReportTable table, final StratifierKey key, final String columnName) {
        final int rowIndex = getRowIndex(table, key);
        final int colIndex = table.getColumnIndex(columnName);
        final Object count = table.get(rowIndex, colIndex);
        if (count == null) {
            table.set(rowIndex, colIndex, 1);
        } else {
            table.set(rowIndex, colIndex, ((int) count) + 1);
        }
    }

    private void finalizeTables() {
        final int referenceStratCount = referenceStratifiers.size() + featureStratifiers.size();
        for (final Map.Entry<StratifierKey, Set<StratifierKey>> stratifierKeySetEntry : coverageKeys.entrySet()) {
            final StratifierKey readStratifier = stratifierKeySetEntry.getKey();
            final Set<StratifierKey> coverageKeys = stratifierKeySetEntry.getValue();

            final StratifierKey referenceKey = new StratifierKey();
            for (int i = 0; i < referenceStratCount; i++) {
                referenceKey.add(readStratifier.get(i));
            }

            int seenRefBases = (int) referenceReportTable.get(referenceKey, GATK_REPORT_COLUMN_COUNT);

            int totalPileCount = 0;
            int coveredRefBases = 0;
            for (final StratifierKey coverageKey : coverageKeys) {
                final int rowIndex = getRowIndex(histogramReportTable, coverageKey);
                final int count = (int) histogramReportTable.get(rowIndex, GATK_REPORT_COLUMN_COUNT);
                final int coverageColIndex = histogramReportTable.getColumnIndex(GATK_REPORT_COLUMN_COVERAGE);
                final int probabilityColIndex = histogramReportTable.getColumnIndex(GATK_REPORT_COLUMN_PROBABILITY);
                final double probability = (double)count / seenRefBases;
                histogramReportTable.set(rowIndex, probabilityColIndex, probability);
                final int coverage = (int) histogramReportTable.get(rowIndex, coverageColIndex);
                totalPileCount += count * coverage;
                coveredRefBases += count;
            }

            int averageRowIndex = getRowIndex(averageReportTable, readStratifier);
            int averageColIndex = averageReportTable.getColumnIndex(GATK_REPORT_COLUMN_AVERAGE);
            final double averageCoverage = (double) totalPileCount / coveredRefBases;
            averageReportTable.set(averageRowIndex, averageColIndex, averageCoverage);

            final StratifierKey zeroKey = new StratifierKey(readStratifier);
            zeroKey.add(0);
            final int zeroIndex = getRowIndex(histogramReportTable, zeroKey);
            final int countColIndex = histogramReportTable.getColumnIndex(GATK_REPORT_COLUMN_COUNT);
            final int probabilityColIndex = histogramReportTable.getColumnIndex(GATK_REPORT_COLUMN_PROBABILITY);
            final int zeroCount = seenRefBases - coveredRefBases;
            final double probability = (double) zeroCount / seenRefBases;
            histogramReportTable.set(zeroIndex, countColIndex, zeroCount);
            histogramReportTable.set(zeroIndex, probabilityColIndex, probability);
        }
    }

    @Override
    public Object onTraversalSuccess() {
        final Histogram<Integer> histo = new Histogram<>("coverage", "count");
        for (int i = 0; i < histogramArray.length; ++i) {
            histo.increment(i, histogramArray[i]);
        }
        final MetricsFile<?, Integer> out = getMetricsFile();
        out.addHistogram(histo);
        System.out.println(outFile.getAbsolutePath());
        out.write(outFile);

        finalizeTables();

        final File outTableFile = outFile.toPath().resolveSibling(outFile.getName() + ".table.txt").toFile();
        System.out.println(outTableFile.getAbsolutePath());
        try {
            try (final PrintStream outTable = new PrintStream(outTableFile)) {
                gatkReport.print(outTable);
            }
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return null;
    }

    @Override
    public void closeTool() {
    }
}
