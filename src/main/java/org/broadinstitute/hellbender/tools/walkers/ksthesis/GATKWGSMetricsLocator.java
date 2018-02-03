package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.tribble.bed.BEDFeature;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.CoverageAnalysisProgramGroup;
import org.broadinstitute.hellbender.engine.*;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.pileup.PileupElement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

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
 * ./gatk-launch GATKWGSMetricsLocator \
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
public final class GATKWGSMetricsLocator extends LocusWalker {

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME,
            doc = "Output file (if not provided, defaults to STDOUT).",
            optional = true)
    public File outFile = null;

    @Argument(fullName = "countThreshold",
            shortName = "CT",
            doc = "Count threshold, default -1",
            optional = true)
    public int countThreshold = -1;

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

    @Argument(fullName = "gcFilter",
            shortName = "GCF",
            doc = "GC content filter, default -1",
            optional = true)
    public int gcFilter = -1;

    @Argument(fullName = "mapability", shortName = "M", doc = "mapability BED file", optional = true)
    public FeatureInput<BEDFeature> mapabilityBed;

    @Argument(fullName = "mapabilityBin",
            shortName = "MAPB",
            doc = "Mapability bin, default 20",
            optional = true)
    public int mapabilityBin = 20;

    @Argument(fullName = "emitZeroCoverageLocs",
            shortName = "EZCL",
            doc = "Emit locations with zero coverage.",
            optional = true)
    public boolean emitZeroCoverageLocs = false;

    @Override
    public List<ReadFilter> getDefaultReadFilters() {
        return GATKWGSMetrics.getMetricsReadFilters(super.getDefaultReadFilters());
    }

    private PrintStream outputStream;

    @Override
    public boolean emitEmptyLoci() {
        return true;
    }

    @Override
    public boolean requiresReference() {
        return true;
    }

    private GCStratifier gcStratifier;
    private MapabilityStratifier mapabilityStratifier;

    @Override
    public void onTraversalStart() {
        gcStratifier = new GCStratifier(gcBin, gcWindowLeadingBases, gcWindowTrailingBases);

        if (mapabilityBed != null)
            mapabilityStratifier = new MapabilityStratifier(mapabilityBin, mapabilityBed);

        try {
            outputStream = new PrintStream(outFile);
        } catch (final FileNotFoundException e) {
            throw new UserException.CouldNotReadInputFile(outFile, e);
        }
    }

    @Override
    public void apply(final AlignmentContext context, final ReferenceContext ref, final FeatureContext featureContext) {
        final byte base = ref.getBase();

        if ('N' == base)
            return;

        final int gcStrat = gcStratifier.getEnabledStratification(ref);
        if (gcFilter > -1 && gcStrat != gcFilter)
            return;

        final int mapStrat = mapabilityBed != null ? mapabilityStratifier.getEnabledStratification(featureContext) : -1;

        int allCount = 0;
        int count = 0;
        boolean printLocInfo = false;

        final int pileupSize = context.getBasePileup().size();

        // Don't even try to count if there are not enough reads to meet the threshold.
        if (pileupSize > countThreshold) {
            for (final PileupElement pileupElement : context.getBasePileup()) {
                allCount += 1;
                if (pileupElement.getQual() < GATKWGSMetrics.MINIMUM_BASE_QUALITY)
                    continue;
                count += 1;
            }

            if (count > countThreshold)
                printLocInfo = true;
        } else if (emitZeroCoverageLocs && pileupSize == 0) {
            printLocInfo = true;
        }

        if (printLocInfo) {
            outputStream.printf("%s\t%d\t%d\t%d\t%d\t%d%n",
                    context.getContig(), context.getStart(), allCount, count, gcStrat, mapStrat);
        }
    }

    @Override
    public Object onTraversalSuccess() {
        outputStream.close();
        return null;
    }
}
