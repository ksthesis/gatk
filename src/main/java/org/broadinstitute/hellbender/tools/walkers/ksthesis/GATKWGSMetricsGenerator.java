package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.vcf.*;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.CoverageAnalysisProgramGroup;
import org.broadinstitute.hellbender.engine.AlignmentContext;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.LocusWalker;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.utils.pileup.ReadPileup;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

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
 * ./gatk-launch GATKWGSMetricsGenerator \
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
public final class GATKWGSMetricsGenerator extends LocusWalker {

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME,
            doc = "Output file.")
    public File outFile = null;

    @Argument(fullName = "read-group-summary-metrics",
            shortName = "RGSM",
            doc = "If true then also generate read group summary metrics for each loci.")
    public boolean generateReadGroupSummaryMetrics = false;


    @Override
    public List<ReadFilter> getDefaultReadFilters() {
        return GATKWGSMetrics.getMetricsReadFilters(super.getDefaultReadFilters());
    }

    @Override
    public boolean emitEmptyLoci() {
        return true;
    }

    @Override
    public boolean requiresReference() {
        return true;
    }

    private VariantContextWriter variantContextWriter = null;

    private int numReadGroups = 0;
    private Mean mathMean = new Mean();
    private Variance mathVariance = new Variance();
    private static final String READ_GROUP_COUNT_KEY = "RGC";
    private static final String READ_GROUP_DEPTH_MEAN_KEY = "RGDM";
    private static final String READ_GROUP_DEPTH_VARIANCE_KEY = "RGDV";
    private static final String READ_GROUP_DEPTH_DISPERSION_KEY = "RGDD";

    @Override
    public void onTraversalStart() {
        variantContextWriter = createVCFWriter(outFile);

        final Set<VCFHeaderLine> vcfHeaderLines = new HashSet<>();
        vcfHeaderLines.add(VCFStandardHeaderLines.getInfoLine(VCFConstants.DEPTH_KEY));
        if (generateReadGroupSummaryMetrics) {
            final VCFInfoHeaderLine infoReadGroupCount = new VCFInfoHeaderLine(
                    READ_GROUP_COUNT_KEY,
                    1,
                    VCFHeaderLineType.Integer,
                    "Read group count"
            );
            final VCFInfoHeaderLine infoReadGroupDepthMean = new VCFInfoHeaderLine(
                    READ_GROUP_DEPTH_MEAN_KEY,
                    1,
                    VCFHeaderLineType.Float,
                    "Approximate depth mean per read group; some reads may have been filtered"
            );
            final VCFInfoHeaderLine infoReadGroupDepthVariance = new VCFInfoHeaderLine(
                    READ_GROUP_DEPTH_VARIANCE_KEY,
                    1,
                    VCFHeaderLineType.Integer,
                    "Approximate depth variance per read group; some reads may have been filtered"
            );
            final VCFInfoHeaderLine infoReadGroupDepthDispersion = new VCFInfoHeaderLine(
                    READ_GROUP_DEPTH_DISPERSION_KEY,
                    1,
                    VCFHeaderLineType.Float,
                    "Approximate depth dispersion per read group; some reads may have been filtered"
            );

            vcfHeaderLines.add(infoReadGroupCount);
            vcfHeaderLines.add(infoReadGroupDepthMean);
            vcfHeaderLines.add(infoReadGroupDepthVariance);
            vcfHeaderLines.add(infoReadGroupDepthDispersion);

            final List<SAMReadGroupRecord> allReadGroups =
                    Objects.requireNonNull(getHeaderForReads(), "getHeaderForReads return null").getReadGroups();
            numReadGroups = new HashSet<>(allReadGroups).size();
            logger.info("Total number of read groups: " + numReadGroups);
        }
        final VCFHeader header = new VCFHeader(vcfHeaderLines);

        final SAMSequenceDictionary sequenceDictionary = Objects.requireNonNull(getHeaderForReads()).getSequenceDictionary();
        header.setSequenceDictionary(sequenceDictionary);

        variantContextWriter.writeHeader(header);
    }

    @Override
    public void apply(final AlignmentContext context, final ReferenceContext ref, final FeatureContext featureContext) {
        final byte base = ref.getBase();

        if ('N' == base)
            return;

        VariantContextBuilder builder = new VariantContextBuilder();
        builder.chr(context.getContig())
                .start(context.getStart())
                .stop(context.getStart())
                .noID()
                .noGenotypes()
                .alleles(Collections.singleton(Allele.create(base, true)));

        final ReadPileup pileup = context.getBasePileup();

        if (generateReadGroupSummaryMetrics) {
            final int[] intCounts = pileup.getReads().stream()
                    .collect(Collectors.toMap(GATKRead::getReadGroup, count -> 1, Integer::sum))
                    .values()
                    .stream()
                    .mapToInt(v -> v)
                    .toArray();

            final int pileupSize = IntStream.of(intCounts).sum();
            final double[] doubleCounts = DoubleStream.concat(
                    IntStream.of(intCounts).mapToDouble(v -> v),
                    DoubleStream.of(0d).limit(numReadGroups - intCounts.length)
            ).toArray();
            final double variance = mathVariance.evaluate(doubleCounts);
            final double mean = mathMean.evaluate(doubleCounts);
            final double dispersion = mean == 0 ? 0 : variance / mean;
            builder.attribute(VCFConstants.DEPTH_KEY, pileupSize)
                    .attribute(READ_GROUP_COUNT_KEY, intCounts.length)
                    .attribute(READ_GROUP_DEPTH_MEAN_KEY, variance)
                    .attribute(READ_GROUP_DEPTH_VARIANCE_KEY, mean)
                    .attribute(READ_GROUP_DEPTH_DISPERSION_KEY, dispersion);
        } else {
            builder.attribute(VCFConstants.DEPTH_KEY, pileup.size());
        }

        variantContextWriter.add(builder.make());
    }

    @Override
    public void closeTool() {
        if (variantContextWriter != null) {
            variantContextWriter.close();
        }
    }

}
