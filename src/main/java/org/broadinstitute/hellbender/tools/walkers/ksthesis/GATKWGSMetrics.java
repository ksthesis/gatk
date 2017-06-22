package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.Histogram;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.QCProgramGroup;
import org.broadinstitute.hellbender.engine.AlignmentContext;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.LocusWalker;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.filters.MappingQualityReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.utils.pileup.PileupElement;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
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
public final class GATKWGSMetrics extends LocusWalker {

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME,
            doc = "Output file (if not provided, defaults to STDOUT).",
            optional = true)
    public File outFile = null;

    @SuppressWarnings("FieldCanBeLocal")
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

    public static class MappedPairFilter extends ReadFilter {
        @Override
        public boolean test(final GATKRead read) {
            return read.isPaired() && !read.mateIsUnmapped();
        }
    }

    @Override
    public boolean emitEmptyLoci() {
        return true;
    }

    @Override
    public boolean requiresReference() {
        return true;
    }

    @Override
    public void onTraversalStart() {
    }

    private String lastContig = null;

    // BEGIN TELOMERE SEARCH
    private static final byte[] TELOMERE_FORWARD = {'T', 'T', 'A', 'G', 'G', 'G'};
    private static final byte[] TELOMERE_REVERSE = {'C', 'C', 'C', 'T', 'A', 'A'};
    private static final int TELOMERE_WINDOW_LEADING_BASES = 3;
    private static final int TELOMERE_WINDOW_TRAILING_BASES = 3; // This is actually 1 too long, window size = 7.
    private static final int TELOMERE_SEARCH = 10_000_000;
    private long lastForward = 0;
    private long lastReverse = 0;
    // END TELOMERE SEARCH

    // BEGIN GC RATIO 1
    private static final int GC_WINDOW_LEADING_BASES = 50;
    private static final int GC_WINDOW_TRAILING_BASES = 50;
    private static final int GC_WINDOW_LENGTH_BASES = GC_WINDOW_LEADING_BASES + GC_WINDOW_TRAILING_BASES + 1;
    private long windowGC = 0;
    private long windowAT = 0;
    // END GC RATIO 1

    // BEGIN GC RATIO 2
    private static final int GC_QUEUE_SIZE = 100;
    private final Queue<Byte> queueGCRatio = new LinkedList<>();
    private long queueGC = 0;
    private long queueAT = 0;
    // END GC RATIO 2

    // BEGIN GC RATIO COMP
    private static final double GC_RATIO_DIFF = 0.5;
    // BEGIN GC RATIO COMP

    @Override
    public void apply(final AlignmentContext context, final ReferenceContext ref, final FeatureContext featureContext) {
        final byte base = ref.getBase();

        final String contig = context.getContig();
        final boolean resetContig;
        if (!Objects.equals(lastContig, contig)) {
            lastContig = contig;
            resetContig = true;
        } else {
            resetContig = false;
        }

        // BEGIN GC RATIO 1
        if (resetContig) {
            windowGC = 0;
            windowAT = 0;
        }
        ref.setWindow(GC_WINDOW_LEADING_BASES, GC_WINDOW_TRAILING_BASES);
        final byte[] gcWindow = ref.getBases();

        if (gcWindow.length == GC_WINDOW_LENGTH_BASES) {
            switch (gcWindow[0]) {
                case 'N':
                    break;
                case 'G':
                case 'C':
                    windowGC--;
                    break;
                case 'A':
                case 'T':
                    windowAT--;
                    break;
                default:
                    break;
            }
        }
        switch (gcWindow[gcWindow.length - 1]) {
            case 'N':
                break;
            case 'G':
            case 'C':
                windowGC++;
                break;
            case 'A':
            case 'T':
                windowAT++;
                break;
            default:
                break;
        }
        final double gcRatio1Total = windowGC + windowAT;
        final double gcRatio1 = gcRatio1Total == 0 ? 0 : windowGC / gcRatio1Total;

        /*
        Result:

        Uses a window over the reference to calculate GC. Only the first and last positions in the window are examined
        as the sliding GC is calculated.

        Pluses:
        - Can look ahead some number of bases.

        Minuses:
        - While the entire window of bases is not traversed, it still must be copied into a block of memory.
        - Current implementation should also ensure that it reads the entire window's contents if the current sum of
          base contents does not equal the expected size. It is (is it?) possible for the walker to start not at the
          chromosome start positions.
         */

        // END GC RATIO 1

        // BEGIN GC RATIO 2
        if (resetContig) {
            queueGCRatio.clear();
            queueGC = 0;
            queueAT = 0;
        }

        final byte removedBase;
        if (queueGCRatio.size() == GC_QUEUE_SIZE) {
            removedBase = queueGCRatio.remove();
            switch (removedBase) {
                case 'N':
                    break;
                case 'G':
                case 'C':
                    queueGC--;
                    break;
                case 'A':
                case 'T':
                    queueAT--;
                    break;
                default:
                    break;
            }
        } else {
            removedBase = '?';
        }
        queueGCRatio.add(base);
        switch ((char) base) {
            case 'N':
                break;
            case 'G':
            case 'C':
                queueGC++;
                break;
            case 'A':
            case 'T':
                queueAT++;
                break;
            default:
                System.out.println("Unexpected base " + (char) base);
                break;
        }
        final double gcRatio2Total = queueGC + queueAT;
        final double gcRatio2 = gcRatio2Total == 0 ? 0 : queueGC / gcRatio2Total;

        /*
        Result:

        Uses only the current base, plus a linked list to store the bases in the current window. No window over the
        reference is required.

        Pluses:
        - Less copying of a window of bases into memory.

        Minuses:
        - Cannot look ahead some trailing number of bases, so only leading bases.
        - Stateful queue of bases assumes that this instance of the walker will always be called sequentially for each
          position. If not, then the queue of bases will not accurately reflect the leading gc content.
         */

        // END GC RATIO 2

        // BEGIN GC RATIO COMP
        if (gcRatio1Total > 0 && gcRatio2Total > 0 && Math.abs(gcRatio1 - gcRatio2) > GC_RATIO_DIFF) {
            final String windowContent = new String(gcWindow);
            final String queueContent = queueGCRatio.stream()
                    .map(b -> "" + ((char) (byte) b))
                    .reduce("" + (char) removedBase, (a, b) -> a + b);

            System.out.printf("GC DIFF AT %s:%d %f/%f (%d %d %s) (%d %d %s)%n",
                    context.getContig(), context.getStart(),
                    gcRatio1, gcRatio2,
                    windowGC, windowAT, windowContent,
                    queueGC, queueAT, queueContent);
        }
        // END GC RATIO COMP

        // BEGIN TELOMERE SEARCH
        if (resetContig) {
            lastForward = 0;
            lastReverse = 0;
        }

        ref.setWindow(TELOMERE_WINDOW_LEADING_BASES, TELOMERE_WINDOW_TRAILING_BASES);
        final byte[] telomereWindow = ref.getBases();

        if (context.getStart() < TELOMERE_SEARCH ||
                context.getEnd() > (ref.getContigLength(context.getContig()) - TELOMERE_SEARCH)) {
            if (Arrays.equals(TELOMERE_FORWARD, Arrays.copyOf(telomereWindow, TELOMERE_FORWARD.length))) {
                final long distanceSinceLastForward = context.getPosition() - lastForward - TELOMERE_FORWARD.length;
                lastForward = context.getPosition();
                System.out.printf(
                        "FORWARD MATCH = %s:%d %d%n",
                        context.getContig(), context.getStart(), distanceSinceLastForward);
            }
            if (Arrays.equals(TELOMERE_REVERSE, Arrays.copyOf(telomereWindow, TELOMERE_REVERSE.length))) {
                final long distanceSinceLastReverse = context.getPosition() - lastReverse - TELOMERE_REVERSE.length;
                lastReverse = context.getPosition();
                System.out.printf("                                " +
                                "REVERSE MATCH = %s:%d %d %f%n",
                        context.getContig(), context.getStart(), distanceSinceLastReverse, gcRatio1);
            }
        }

        /*
        Result:
        The telomere positions are in the reference, but of the two ends of the contigs, they only appeared at the
        ends.

        @HD	VN:1.5	SO:unsorted
        @SQ	SN:20	LN:63025520	M5:0dec9660ec1efaaf33281c0d5ea2560f	UR:file:human_g1k_v37.20.21.fasta
        @SQ	SN:21	LN:48129895	M5:2979a6085bfe28e3ad6f552f361ed74d	UR:file:human_g1k_v37.20.21.fasta
        FORWARD MATCH = 20:62914278 3386
        FORWARD MATCH = 20:62916416 2132
        FORWARD MATCH = 20:62918060 1638
        FORWARD MATCH = 20:62918066 0
        FORWARD MATCH = 20:62918072 0
        FORWARD MATCH = 20:62918079 1
        FORWARD MATCH = 20:62918098 13
        FORWARD MATCH = 20:62918104 0
        FORWARD MATCH = 20:62918110 0
        FORWARD MATCH = 20:62918121 5
        ...snip...
        FORWARD MATCH = 20:62918924 13
        FORWARD MATCH = 20:62918930 0
        FORWARD MATCH = 20:62918942 6
        FORWARD MATCH = 20:62918962 14
        FORWARD MATCH = 20:62918968 0
        FORWARD MATCH = 20:62918980 6
        FORWARD MATCH = 20:62929765 10779
        ...and similar on chr21.

        As the positions are reference based, we can pre-process their locations and have a separate track specifying
        the position. That would allow searching for distance-to-the-telomeres.

        Otherwise, I'm currently not sure how to dynamically know that we're "approaching a telomere". Perhaps we could
        look ahead a certain distance for a series of telomeric repeats?

        Can also make a note about how allowing zero mismatches. It's possible that more mismatches would provide better
        telomeric matching, but could increase computational burden.
         */

        // END TELOMERE SEARCH

        if ('N' == base)
            return;

        final Iterable<PileupElement> iterable = () -> context.getBasePileup().iterator();
        final Set<String> readNames = StreamSupport.stream(iterable.spliterator(), false)
                .filter(pe -> pe.getQual() >= MINIMUM_BASE_QUALITY)
                .map(pe -> pe.getRead().getName())
                .collect(Collectors.toSet());
        final int depth = Math.min(readNames.size(), COVERAGE_CAP);
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
        return null;
    }

    @Override
    public void closeTool() {
    }
}
