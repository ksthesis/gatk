package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.samtools.util.IOUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.CoverageAnalysisProgramGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLineProgramProperties(
        summary = "TODO",
        oneLineSummary = "TODO",
        programGroup = CoverageAnalysisProgramGroup.class)
@SuppressWarnings({"unused", "WeakerAccess"})
public class GatherGATKWGSMetrics extends CommandLineProgram {
    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = "List of scattered report files")
    public final List<File> inputs = new ArrayList<>();

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME,
            doc = "Output report path")
    public String outputReport = null;

    @Argument(fullName = "outputCountPileup",
            shortName = "OCP",
            doc = "Output for the count of the pileup",
            optional = true)
    public String outputCountPileup = null;

    @Override
    protected Object doWork() {
        inputs.forEach(IOUtil::assertFileIsReadable);

        if (inputs.size() <= 0) {
            logger.info("No inputs specified.");
            return 0;
        }

        logger.info("Loading 1 of {}: {}", inputs.size(), inputs.get(0));
        final GATKWGSMetricsReport acc = new GATKWGSMetricsReport(inputs.get(0));

        for (int i = 1; i < inputs.size(); i++) {
            logger.info("Merging {}: {}", (i + 1), inputs.get(i));
            final GATKWGSMetricsReport inc = new GATKWGSMetricsReport(inputs.get(i));
            acc.combineCounts(inc);
        }

        logger.info("Generating aggregate statistics");
        acc.updateAggregateStats();

        logger.info("Writing report output: {}", outputReport);
        acc.printReport(outputReport);

        if (outputCountPileup != null) {
            logger.info("Writing pileup count: {}", outputCountPileup);
            acc.printCountPileup(outputCountPileup);
        }

        logger.info("Done");

        return 0;
    }
}
