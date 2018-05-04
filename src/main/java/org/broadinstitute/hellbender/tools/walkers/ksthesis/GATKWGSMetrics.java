package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.Feature;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.variant.variantcontext.VariantContext;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.CoverageAnalysisProgramGroup;
import org.broadinstitute.hellbender.engine.*;
import org.broadinstitute.hellbender.engine.filters.MappingQualityReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.utils.pileup.PileupElement;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.*;

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
        programGroup = CoverageAnalysisProgramGroup.class)
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess", "unused", "unchecked"})
public final class GATKWGSMetrics extends LocusWalker {

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME,
            doc = "Output report path")
    public String outputReport = null;

    @Argument(fullName = "outputCountPileup",
            shortName = "OCP",
            doc = "Output for the count of the pileup",
            optional = true)
    public String outputCountPileup = null;

    @Argument(fullName = "gcLeading",
            shortName = "GCL",
            doc = "GC window leading bases, default 75",
            optional = true)
    public int gcWindowLeadingBases = 75;

    @Argument(fullName = "gcTrailing",
            shortName = "GCT",
            doc = "GC window trailing bases, default 75",
            optional = true)
    public int gcWindowTrailingBases = 75;

    @Argument(fullName = "gcBin",
            shortName = "GCB",
            doc = "GC content bin, default 2",
            optional = true)
    public double gcBin = 2;

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

    @Argument(fullName = "coverage", shortName = "COV", doc = "coverage VCF file", optional = true)
    public FeatureInput<VariantContext> coverageVcf;

    @Argument(fullName = "coverageBin",
            shortName = "COVB",
            doc = "Coverage bin, default 50",
            optional = true)
    public int coverageBin = 50;

    @Argument(fullName = "coverageMax",
            shortName = "COVM",
            doc = "Coverage max, default 500",
            optional = true)
    public int coverageMax = 500;

    @Argument(fullName = "mapability", shortName = "M", doc = "mapability BED file", optional = true)
    public FeatureInput<BEDFeature> mapabilityBed;

    @Argument(fullName = "mapabilityBin",
            shortName = "MAPB",
            doc = "Mapability bin, default 20",
            optional = true)
    public int mapabilityBin = 20;

    @Argument(fullName = "regionLabels", shortName = "RL", doc = "region labels BED file", optional = true)
    public FeatureInput<BEDFeature> regionLabelsBed;

    @Argument(fullName = "disableRegionLabels", shortName = "DRL", doc = "disable region labels", optional = true)
    public boolean disableRegionLabels = false;

    @Argument(fullName = "flattenReadGroups",
            shortName = "FRG",
            doc = "Flatten read groups, default false",
            optional = true)
    public boolean flattenReadGroups = false;

    @Argument(fullName = "strandOrientationStratifier",
            shortName = "SOS",
            doc = "Stratify counts by strand orientation, default false",
            optional = true)
    public boolean strandOrientationStratifier = false;

    // Constants from CollectWgsMetrics
    public static final int MINIMUM_MAPPING_QUALITY = 20;
    public static final int MINIMUM_BASE_QUALITY = 20;
    public static final int COVERAGE_CAP = 250;

    @Override
    public List<ReadFilter> getDefaultReadFilters() {
        return getMetricsReadFilters(super.getDefaultReadFilters());
    }

    // These are the default read filters from CollectWgsMetrics,
    // minus the duplicate read filter & mapped pair filter,
    // plus the mate different strand & mate on same contig
    public static List<ReadFilter> getMetricsReadFilters(final List<ReadFilter> defaultFilters) {
        defaultFilters.add(ReadFilterLibrary.NOT_SECONDARY_ALIGNMENT);
        defaultFilters.add(ReadFilterLibrary.PASSES_VENDOR_QUALITY_CHECK);
        defaultFilters.add(new MappingQualityReadFilter(MINIMUM_MAPPING_QUALITY));
        defaultFilters.add(ReadFilterLibrary.MATE_DIFFERENT_STRAND);
        defaultFilters.add(ReadFilterLibrary.MATE_ON_SAME_CONTIG_OR_NO_MAPPED_MATE);
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
    private final List<ReferenceStratifier<?>> referenceStratifiers = new ArrayList<>();
    private final List<FeatureStratifier<? extends Feature, ?>> featureStratifiers = new ArrayList<>();
    private final List<ReadStratifier<?>> readStratifiers = new ArrayList<>();

    @Override
    public void onTraversalStart() {
        final GCStratifier gcStratifier = new GCStratifier(gcBin);
        gcStratifier.setLeadingBases(gcWindowLeadingBases);
        gcStratifier.setTrailingBases(gcWindowTrailingBases);
        referenceStratifiers.add(gcStratifier);

        if (coverageVcf != null)
            featureStratifiers.add(new PriorCoverageStratifier(coverageVcf, coverageBin, coverageMax));

        if (mapabilityBed != null)
            featureStratifiers.add(new MapabilityStratifier(mapabilityBed, mapabilityBin));

        if (regionLabelsBed != null)
            featureStratifiers.add(new RegionLabelStratifier(regionLabelsBed, !disableRegionLabels));

        readStratifiers.add(new ReadGroupStratifier(flattenReadGroups));
        readStratifiers.add(new AbsTLenStratifier(insertSizeBin, insertSizeMax));
        readStratifiers.add(new StrandOrientationStratifier(strandOrientationStratifier));

        gatkReport = new GATKWGSMetricsReport(referenceStratifiers, featureStratifiers, readStratifiers);
    }

    @Override
    public void apply(final AlignmentContext context, final ReferenceContext ref, final FeatureContext featureContext) {
        final byte base = ref.getBase();

        if ('N' == base)
            return;

        final StratifierKey referenceStratiferKey = new StratifierKey();

        for (final ReferenceStratifier<?> referenceStratifier : referenceStratifiers) {
            final Object stratification = referenceStratifier.getEnabledStratification(ref);
            referenceStratiferKey.add(stratification);
        }

        for (final FeatureStratifier<? extends Feature, ?> featureStratifier : featureStratifiers) {
            final Object stratification = featureStratifier.getEnabledStratification(featureContext);
            referenceStratiferKey.add(stratification);
        }

        // To calculate zero coverage:
        // 1) Create a count of locations that match the reference(+feature) stratifiers
        // 2) Later, when counting the _read_ stratifiers, count how many locations had >=1 coverage.
        // 3) The difference between reference-stratifiers coverage and reference-stratifiers-plus-read-stratifiers
        //    with >= 1 coverage is the number of 0 coverage.
        gatkReport.incrementReferenceCount(referenceStratiferKey);

        // For each read stratifier key, get a total number of entries for that read stratification within the pileup.
        final Map<StratifierKey, Integer> stratCounts = new LinkedHashMap<>();
        for (final PileupElement pileupElement : context.getBasePileup()) {
            if (pileupElement.getQual() < MINIMUM_BASE_QUALITY)
                continue;

            final GATKRead read = pileupElement.getRead();

            // Concat all the different strats.
            final StratifierKey readStratifierKey = new StratifierKey(referenceStratiferKey);
            for (final ReadStratifier<?> readStratifier : readStratifiers) {
                final Object stratification = readStratifier.getEnabledStratification(read);
                readStratifierKey.add(stratification);
            }

            // Add a new entry into the collection.
            stratCounts.merge(readStratifierKey, 1, (acc, inc) -> acc + inc);
        }

        // Find the histogram rows for that stratifier key.
        // For that histogram rows, increment the depth using the total number.
        // ex: 2 reads were found with GC 50-51 and TLEN 300-399
        for (final Map.Entry<StratifierKey, Integer> entry : stratCounts.entrySet()) {
            final StratifierKey stratKey = entry.getKey();
            final int stratDepth = Math.min(entry.getValue(), COVERAGE_CAP);
            gatkReport.incrementReadCoverageCount(stratKey, stratDepth);
        }
    }

    @Override
    public Object onTraversalSuccess() {
        gatkReport.updateAggregateStats();
        gatkReport.printReport(outputReport);
        if (outputCountPileup != null) {
            gatkReport.printCountPileup(outputCountPileup);
        }

        return null;
    }
}
