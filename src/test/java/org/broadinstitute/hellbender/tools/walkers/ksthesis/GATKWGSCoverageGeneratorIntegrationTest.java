package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class GATKWGSCoverageGeneratorIntegrationTest extends CommandLineProgramTest {

    private static final Path TEST_DATA_DIR = getTestDataDir().toPath().resolve("walkers/ksthesis");

    @Test
    public void testOnTraversalStart() throws Exception {
        final File tempFile = createTempFile("testGATKWGSMetricsGeneratorDefaults.", ".vcf");
        tempFile.createNewFile();
        IntegrationTestSpec testSpec = new IntegrationTestSpec(
                " -R " + b37_reference_20_21 +
                        " -I " + NA12878_20_21_WGS_bam +
                        " -L 20:10000001-10001000" +
                        " -O " + tempFile,
                Collections.emptyList()
        );
        testSpec.executeTest("testGATKWGSMetricsGeneratorDefaults", this);
        final Path expectedFile = TEST_DATA_DIR.resolve("testGATKWGSMetricsGeneratorDefaults.vcf");
        IntegrationTestSpec.assertEqualTextFiles(tempFile, expectedFile.toFile());
    }
}
