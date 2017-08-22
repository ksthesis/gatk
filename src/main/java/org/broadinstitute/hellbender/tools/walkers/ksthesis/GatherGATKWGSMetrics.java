package org.broadinstitute.hellbender.tools.walkers.ksthesis;

import htsjdk.samtools.util.IOUtil;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.cmdline.programgroups.QCProgramGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLineProgramProperties(
        summary = "TODO",
        oneLineSummary = "TODO",
        programGroup = QCProgramGroup.class)
@SuppressWarnings("unused")
public class GatherGATKWGSMetrics extends CommandLineProgram {
    @Argument(fullName = StandardArgumentDefinitions.INPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.INPUT_SHORT_NAME, doc = "List of scattered BQSR report files")
    public final List<File> inputs = new ArrayList<>();

    @Argument(fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
            shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, doc = "File to output the gathered file to")
    public File output;

    @Override
    protected Object doWork() {
        inputs.forEach(IOUtil::assertFileIsReadable);

        if (inputs.size() <= 0) {
            dump("No inputs specified.");
            return 0;
        }

        IOUtil.assertFileIsWritable(output);

        dump(String.format("Loading 1 of %d: %s", inputs.size(), inputs.get(0)));
        final GATKWGSMetricsReport acc = new GATKWGSMetricsReport(inputs.get(0));

        for (int i = 1; i < inputs.size(); i++) {
            dump(String.format("Merging %d: %s", (i + 1), inputs.get(i)));
            final GATKWGSMetricsReport inc = new GATKWGSMetricsReport(inputs.get(i));
            acc.combineCounts(inc);
        }

        dump("Generating aggregate statistics");
        acc.updateAggregateStats();

        dump(String.format("Writing output: %s", output));
        acc.print(output);

        dump("Done");

        return 0;
    }

    // TODO: KSTHESIS: Stop using dump
    private void dump(final String s) {
        System.out.println(s);
    }
}
