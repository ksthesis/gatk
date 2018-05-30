package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;

public class PredictGATKWGSMetricsIntegrationTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testPredictGATKWGSMetrics() throws Exception {
        final ArgumentsBuilder args = new ArgumentsBuilder();
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("testGATKWGSMetricsK100Umap1.txt"));
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("testGATKWGSMetricsK100Umap2.txt"));
        args.add("--pileup");
        args.add(50_000);
        args.add("--average");

        final File tempFile = BaseTest.createTempFile("testPredictGATKWGSMetrics.", ".txt");
        args.add("-O");
        args.add(tempFile);
        final Object res = this.runCommandLine(args.getArgsArray());

        Assert.assertEquals(res, 0);
        final Path expectedFile = TEST_DATA_DIR.resolve("testPredictGATKWGSMetrics.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }
}
