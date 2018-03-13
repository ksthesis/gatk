package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class GATKWGSMetricsLocatorIntegrationTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testGATKWGSMetricsLocatorDefaults() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsLocatorDefaults.", ".txt");
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCF 30" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsLocatorDefaults", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsLocatorDefaults.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsLocatorCountThreshold() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsLocatorCountThreshold.", ".txt");
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCF 30" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -CT 99" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsLocatorCountThreshold", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsLocatorCountThreshold.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsLocatorEncode() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsLocatorEncode.", ".txt");
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -M " + TEST_DATA_DIR.resolve("wgEncodeCrgMapabilityAlign36mer.20_10000001-10002000.bed") +
                        " -L 20:10000001-10001000" +
                        " -GCF 30" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsEncode", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsLocatorEncode.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsLocatorCountZeros() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsLocatorCountZeros.", ".txt");
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:9999899-9999904" +
                        " -EZCL" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -CT 99999" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsLocatorCountZeros", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsLocatorCountZeros.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsLocatorCountThresholdAndZeros() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsLocatorCountThresholdAndZeros.", ".txt");
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:9999899-10001000" +
                        " -EZCL" +
                        " -GCF 34" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -CT 99" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsLocatorCountThresholdAndZeros", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsLocatorCountThresholdAndZeros.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

}
