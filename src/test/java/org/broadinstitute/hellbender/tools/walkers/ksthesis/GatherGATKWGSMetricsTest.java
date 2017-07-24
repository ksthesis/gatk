package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;

public class GatherGATKWGSMetricsTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testGatherGATKWGSMetrics() throws Exception {
        final ArgumentsBuilder args = new ArgumentsBuilder();
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("expected.testGatherGATKWGSMetrics.1.table.txt"));
        args.add("--input");
        args.add(TEST_DATA_DIR.resolve("expected.testGatherGATKWGSMetrics.2.table.txt"));

        final File tmpFile = BaseTest.createTempFile("GatherGATKWGSMetrics.", "");
        final File outFile = tmpFile.toPath().resolveSibling(tmpFile.getName() + ".table.txt").toFile();
        args.add("-O");
        args.add(outFile);
        System.out.println(outFile);
        final Object res = this.runCommandLine(args.getArgsArray());
        Assert.assertEquals(res, 0);
    }

}