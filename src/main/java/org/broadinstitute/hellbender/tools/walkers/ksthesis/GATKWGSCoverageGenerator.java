package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFStandardHeaderLines;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.CoverageAnalysisProgramGroup;
import org.broadinstitute.hellbender.engine.AlignmentContext;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.LocusWalker;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;

import java.io.File;
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
 * ./gatk-launch GATKWGSCoverageGenerator \
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
public final class GATKWGSCoverageGenerator extends LocusWalker {

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME,
            doc = "Output file (if not provided, defaults to STDOUT).",
            optional = true)
    public File outFile = null;

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

    @Override
    public void onTraversalStart() {
        variantContextWriter = createVCFWriter(outFile);

        final Set<VCFHeaderLine> vcfHeaderLines = new HashSet<>();
        vcfHeaderLines.add(VCFStandardHeaderLines.getInfoLine(VCFConstants.DEPTH_KEY));
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

        final int pileupSize = context.getBasePileup().size();

        final VariantContext variantContext = new VariantContextBuilder()
                .chr(context.getContig())
                .start(context.getStart())
                .stop(context.getStart())
                .noID()
                .noGenotypes()
                .alleles(Collections.singleton(Allele.create(base, true)))
                .attribute(VCFConstants.DEPTH_KEY, pileupSize)
                .make();

        variantContextWriter.add(variantContext);
    }

    @Override
    public void closeTool() {
        if (variantContextWriter != null) {
            variantContextWriter.close();
        }
    }

}
