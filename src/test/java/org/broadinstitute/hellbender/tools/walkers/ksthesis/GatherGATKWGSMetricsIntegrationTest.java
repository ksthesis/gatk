package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;

public class GatherGATKWGSMetricsIntegrationTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testGatherGATKWGSMetrics() throws Exception {
        final ArgumentsBuilder args = new ArgumentsBuilder();
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("testGATKWGSMetricsEncode1.txt"));
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("testGATKWGSMetricsEncode2.txt"));

        final File tempFile = BaseTest.createTempFile("testGatherGATKWGSMetrics.", ".txt");
        args.add("-O");
        args.add(tempFile);
        final Object res = this.runCommandLine(args.getArgsArray());

        Assert.assertEquals(res, 0);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGatherGATKWGSMetrics.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

}
