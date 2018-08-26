package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.testutils.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class GATKWGSMetricsIntegrationTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testGATKWGSMetricsDefaults() throws IOException {
        final File tempReportFile = createTempFile("testGATKWGSMetricsDefaults.", ".txt");
        final File tempCountFile = createTempFile("testGATKWGSMetricsDefaults.count.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -O " + tempReportFile +
                        " -OCP " + tempCountFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsDefaults", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsDefaults.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempReportFile, expectedFile.toFile());
        final List<String> lines = Files.readAllLines(tempCountFile.toPath());
        Assert.assertEquals(lines.size(), 1);
        Assert.assertEquals(lines.get(0), "81255");
    }

    @Test
    public void testGATKWGSMetricsGcDecimal() throws IOException {
        final File tempReportFile = createTempFile("testGATKWGSMetricsGcDecimal.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCB 33.333" +
                        " -O " + tempReportFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsGcDecimal", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsGcDecimal.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempReportFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsFilterDuplicates() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsFilterDuplicates.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -RF NotDuplicateReadFilter" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsFilterDuplicates", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsFilterDuplicates.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsRgOnly() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsRgOnly.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCB 101" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -ISM 0" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsRgOnly", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsRgOnly.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsStrandOrientation() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsStrandOrientation.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -SOS" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsStrandOrientation", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsStrandOrientation.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsRgFilter() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsRgFilter.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCL 50" +
                        " -GCT 50" +
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
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -FRG" +
                        " --read-filter ReadGroupBlackListReadFilter" +
                        " --read-group-black-list RG:20GAV.8" +
                        " --read-group-black-list RG:20GAV.7" +
                        " --read-group-black-list RG:20GAV.6" +
                        " --read-group-black-list RG:20GAV.5" +
                        " --read-group-black-list RG:20GAV.4" +
                        " --read-group-black-list RG:20GAV.3" +
                        " --read-group-black-list RG:20GAV.2" +
                        " --read-group-black-list RG:20GAV.1" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsRgFlatten", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsRgFlatten.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsK100Umap1() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsK100Umap1.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -M " + TEST_DATA_DIR.resolve("hg19.k100.umap.20_10000001-10002000.bed") +
                        " -L 20:10000001-10001000" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsK100Umap1", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsK100Umap1.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsK100Umap2() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsK100Umap2.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -M " + TEST_DATA_DIR.resolve("hg19.k100.umap.20_10000001-10002000.bed") +
                        " -L 20:10001001-10002000" +
                        " -GCL 50" +
                        " -GCT 50" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsK100Umap2", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsK100Umap2.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsLabelsEnabled() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsLabelsEnabled.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -RL " + TEST_DATA_DIR.resolve("region_labels_20_10000001-10001000.bed") +
                        " -L 20:10000001-10001000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsLabelsEnabled", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsLabelsEnabled.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsLabelsDisabled() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsLabelsDisabled.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -RL " + TEST_DATA_DIR.resolve("region_labels_20_10000001-10001000.bed") +
                        " -DRL" +
                        " -L 20:10000001-10001000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsLabelsDisabled", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsLabelsDisabled.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsCoverageEnabled() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsCoverageEnabled.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -COV " + TEST_DATA_DIR.resolve("testGATKWGSMetricsGeneratorDefaults.vcf") +
                        " -COVB 10" +
                        " -COVM 90" +
                        " -L 20:10000001-10001000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsCoverageEnabled", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsCoverageEnabled.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }

    @Test
    public void testGATKWGSMetricsCoverageDisabled() throws IOException {
        final File tempFile = createTempFile("testGATKWGSMetricsCoverageDisabled.", ".txt");
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -COV " + TEST_DATA_DIR.resolve("testGATKWGSMetricsGeneratorDefaults.vcf") +
                        " -COVB 0" +
                        " -COVM 0" +
                        " -L 20:10000001-10001000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsCoverageDisabled", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsCoverageDisabled.txt");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }
}
