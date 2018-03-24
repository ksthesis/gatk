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
public class PredictGATKWGSMetrics extends CommandLineProgram {
    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = "List of scattered report files")
    public final List<File> inputs = new ArrayList<>();

    @Argument(fullName = "pileup", doc = "Prediction pileup size")
    public Long pileup = null;

    @Argument(fullName = "doNotAverage", doc = "Do not generate an average")
    public boolean doNotAverage = false;

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "File to output the gathered file to")
    public File output;

    @Override
    protected Object doWork() {
        inputs.forEach(IOUtil::assertFileIsReadable);

        if (inputs.size() <= 0) {
            logger.info("No inputs specified.");
            return 0;
        }

        IOUtil.assertFileIsWritable(output);

        final GATKWGSPrediction acc = new GATKWGSPrediction(pileup, GATKWGSMetrics.COVERAGE_CAP);

        for (int i = 0; i < inputs.size(); i++) {
            logger.info("Predicting {}: {}", (i + 1), inputs.get(i));
            //final GATKWGSMetricsReport inc = new GATKWGSMetricsReport(inputs.get(i));
            acc.addMetrics(inputs.get(i));
        }
        if (!doNotAverage) {
            logger.info("Averaging the predictions");
            acc.addAverageMetrics();
        }

        logger.info("Writing output: {}", output);
        acc.print(output);

        logger.info("Done");

        return 0;
    }
}
