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
    public void testGATKWGSMetricsDefaults() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsDefaults.", ".txt");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsDefaults", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsDefaults.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsRgOnly() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsRgOnly.", ".txt");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCB 101" +
                        " -ISM 0" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsRgOnly", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsRgOnly.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsRgFilter() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsRgFilter.", ".txt");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -FRG" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsRgFilter", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsRgFilter.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsRgFlatten() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsRgFlatten.", ".txt");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -FRG" +
                        " --readFilter ReadGroupBlackListReadFilter" +
                        " --blackList RG:20GAV.8" +
                        " --blackList RG:20GAV.7" +
                        " --blackList RG:20GAV.6" +
                        " --blackList RG:20GAV.5" +
                        " --blackList RG:20GAV.4" +
                        " --blackList RG:20GAV.3" +
                        " --blackList RG:20GAV.2" +
                        " --blackList RG:20GAV.1" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsRgFlatten", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsRgFlatten.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsEncode1() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsEncode1.", ".txt");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -M " + TEST_DATA_DIR.resolve("wgEncodeCrgMapabilityAlign36mer.20_10000001-10002000.bed") +
                        " -L 20:10000001-10001000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsEncode1", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsEncode1.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsEncode2() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsEncode2.", ".txt");
        //noinspection ResultOfMethodCallIgnored
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -M " + TEST_DATA_DIR.resolve("wgEncodeCrgMapabilityAlign36mer.20_10000001-10002000.bed") +
                        " -L 20:10001001-10002000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsEncode2", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsEncode2.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }
}
