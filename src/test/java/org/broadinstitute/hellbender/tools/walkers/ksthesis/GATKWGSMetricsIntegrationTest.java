package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

public class GATKWGSMetricsIntegrationTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testGATKWGSMetrics() throws IOException {
        final File tempFile = createTempFile("GATKWGSMetrics.", ".metrics");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetrics", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("expected.testGATKWGSMetrics.metrics");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile(), "#");
    }

    @Test
    public void testGATKWGSMetricsEncode() throws IOException {
        final File tempFile = createTempFile("GATKWGSMetrics.", ".metrics");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -M " + "wgEncodeCrgMapabilityAlign36mer.subset.bed" +
                        " -L 20:10000000-11000000 " +
                        " -RGS 2" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetrics", this);
    }

    /**
     * Produces a different result if reading into adaptors is allowed using a replacement method:
     *
     * <pre>
     * private boolean dontIncludeReadInPileup(final GATKRead rec, final long pos) {
     *     boolean readOverlapsPos = (rec.getStart() <= pos && pos <= rec.getEnd());
     *     return !readOverlapsPos;
     * }
     * </pre>
     */
    @Test
    public void testGATKWGSMetricsAdaptorCoverage() throws IOException {
        final File tempFile = createTempFile("GATKWGSMetrics.", ".metrics");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + TEST_DATA_DIR.resolve("CEUTrio.HiSeq.WGS.b37.NA12878.4_bases_of_adaptor.bam") +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsAdaptorCoverage", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("expected.testGATKWGSMetrics.adaptor.metrics");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile(), "#");
    }
}
