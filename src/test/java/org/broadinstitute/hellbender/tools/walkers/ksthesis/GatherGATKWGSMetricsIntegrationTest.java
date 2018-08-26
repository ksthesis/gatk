package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.testutils.ArgumentsBuilder;
import org.broadinstitute.hellbender.testutils.BaseTest;
import org.broadinstitute.hellbender.testutils.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GatherGATKWGSMetricsIntegrationTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testGatherGATKWGSMetrics() throws Exception {
        final ArgumentsBuilder args = new ArgumentsBuilder();
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("testGATKWGSMetricsK100Umap1.txt"));
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("testGATKWGSMetricsK100Umap2.txt"));

        final File tempReportFile = BaseTest.createTempFile("testGatherGATKWGSMetrics.", ".txt");
        args.add("-O");
        args.add(tempReportFile);
        final File tempCountFile = BaseTest.createTempFile("testGatherGATKWGSMetrics.count.", ".txt");
        args.add("-OCP");
        args.add(tempCountFile);
        final Object res = this.runCommandLine(args.getArgsArray());

        Assert.assertEquals(res, 0);
        final Path expectedReportFile = TEST_DATA_DIR.resolve("testGatherGATKWGSMetrics.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempReportFile, expectedReportFile.toFile());
        final List<String> lines = Files.readAllLines(tempCountFile.toPath());
        Assert.assertEquals(lines.size(), 1);
        Assert.assertEquals(lines.get(0), "164297");
    }

}
