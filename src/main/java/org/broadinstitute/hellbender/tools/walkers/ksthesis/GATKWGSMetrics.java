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

import java.io.File;
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
@CommandLineProgramProperties(
        summary = "TODO",
        oneLineSummary = "TODO",
        programGroup = QCProgramGroup.class)
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
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
    public int gcWindowLeadingBases = 50;

    @Argument(fullName = "gcTrailing",
            shortName = "GCT",
            doc = "GC window trailing bases, default 250",
            optional = true)
    public int gcWindowTrailingBases = 250;

    @Argument(fullName = "gcBin",
            shortName = "GCB",
            doc = "GC content bin, default 2",
            optional = true)
    public int gcBin = 2;

    @Argument(fullName = "insertSizeMax",
            shortName = "ISM",
            doc = "Insert size maximum, default 600",
            optional = true)
    public int insertSizeMax = 600;

    @Argument(fullName = "insertSizeBin",
            shortName = "ISB",
            doc = "Insert size bin, default 100",
            optional = true)
    public int insertSizeBin = 100;

    @Argument(fullName = "mapabilityBin",
            shortName = "MAPB",
            doc = "Mapability bin, default 20",
            optional = true)
    public int mapabilityBin = 20;

    @Argument(fullName = "mapability", shortName = "M", doc = "mapability BED file", optional = true)
    public FeatureInput<BEDFeature> mapabilityBed;

    @Argument(fullName = "readGroupSplits", shortName = "RGS", doc = "read group splits", optional = true)
    public Integer readGroupSplits = 0;

    // Constants from CollectWgsMetrics
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

    private GATKWGSMetricsReport gatkReport;
    private final List<ReferenceStratifier> referenceStratifiers = new ArrayList<>();
    private final List<FeatureStratifier<? extends Feature>> featureStratifiers = new ArrayList<>();
    private final List<ReadStratifier> readStratifiers = new ArrayList<>();

    @Override
    public void onTraversalStart() {
        referenceStratifiers.add(new GCStratifier(gcBin, gcWindowLeadingBases, gcWindowTrailingBases));

        featureStratifiers.add(new MapabilityStratifier(mapabilityBin, mapabilityBed));

        readStratifiers.add(new DebugReadGroupStratifier(readGroupSplits));
        readStratifiers.add(new AbsTLenStratifier(insertSizeBin, insertSizeMax));

        gatkReport = new GATKWGSMetricsReport(referenceStratifiers, featureStratifiers, readStratifiers);
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
            gatkReport.incrementReadCoverageCount(stratKey, stratDepth);
        }

        // Move on to the next location.
        // TODONE: Q: What to do about zeros coverage? A: What we're doing here.
        // 1) Create a count of locations that match some composite reference stratifiers
        // 2) Later, when counting the _read_ stratifiers, count how many locations had >=1 coverage.
        // 3) The difference between reference-stratifiers coverage and reference-stratifiers-plus-read-stratifiers with >= 1 coverage is the number of 0 coverage.
        gatkReport.incrementReferenceCount(referenceStratiferKey);

        histogramArray[depth]++;
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

        gatkReport.updateAggregateStats();

        final File outTableFile = outFile.toPath().resolveSibling(outFile.getName() + ".table.txt").toFile();
        System.out.println(outTableFile.getAbsolutePath());
        gatkReport.print(outTableFile);
        return null;
    }
}
