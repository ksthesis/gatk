package org.broadinstitute.hellbender.tools;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.IOUtil;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

public final class SplitReadsIntegrationTest extends CommandLineProgramTest {

    private static final String TEST_DATA_GOOD_READS_PREFIX = "split_reads";
    private static final String TEST_DATA_MISSING_LIB__PREFIX = "split_reads_missing_lib";


    private String getReferenceSequenceName(final String baseName) { return baseName + ".fasta"; }

    private boolean isReferenceRequired(final SamReader.Type type) {
        return type == SamReader.Type.CRAM_TYPE;
    }

    @DataProvider(name = "splitReadsData")
    public Object[][] getSplitReadsData() {
        final Map<String, Integer> byNone = new TreeMap<>();
        byNone.put("", 19);

        final Map<String, Integer> bySample = new TreeMap<>();
        bySample.put(".Momma", 17);
        bySample.put(".Poppa", 2);

        final Map<String, Integer> byRG = new TreeMap<>();
        byRG.put(".0", 17);
        byRG.put(".1", 2);

        final Map<String, Integer> byLibrary = new TreeMap<>();
        byLibrary.put(".whatever", 19);

        final Map<String, Integer> bySampleAndRG = new TreeMap<>();
        bySampleAndRG.put(".Momma.0", 17);
        bySampleAndRG.put(".Poppa.1", 2);

        final Map<String, Integer> bySampleAndRGAndLibrary = new TreeMap<>();
        bySampleAndRGAndLibrary.put(".Momma.0.whatever", 17);

        // test that reads from RGs with no library attribute are output to "unknown"
        final Map<String, Integer> byUnknown = new TreeMap<>();
        byUnknown.put(".whatever", 2);
        byUnknown.put("."  + SplitReads.UNKNOWN_OUT_PREFIX, 17);

        final Function<SamReader.Type[], Stream<Object[]>> argTests = types -> {
            final SamReader.Type typeIn = types[0];
            final SamReader.Type typeOut = types[1];
            return Stream.of(
                    new Object[]{typeIn, typeOut, TEST_DATA_GOOD_READS_PREFIX, Collections.<String>emptyList(),
                            byNone
                    },
                    new Object[]{typeIn, typeOut, TEST_DATA_GOOD_READS_PREFIX, Collections.singletonList(
                            SplitReads.SAMPLE_SHORT_NAME),
                            bySample
                    },
                    new Object[]{typeIn, typeOut, TEST_DATA_GOOD_READS_PREFIX, Collections.singletonList(
                            SplitReads.READ_GROUP_SHORT_NAME),
                            byRG
                    },
                    new Object[]{typeIn, typeOut, TEST_DATA_GOOD_READS_PREFIX, Collections.singletonList(
                            SplitReads.LIBRARY_NAME_SHORT_NAME),
                            byLibrary
                    },
                    new Object[]{typeIn, typeOut, TEST_DATA_GOOD_READS_PREFIX, Arrays.asList(
                            SplitReads.SAMPLE_SHORT_NAME,
                            SplitReads.READ_GROUP_SHORT_NAME),
                            bySampleAndRG
                    },
                    new Object[]{typeIn, typeOut, TEST_DATA_GOOD_READS_PREFIX, Arrays.asList(
                            SplitReads.SAMPLE_SHORT_NAME,
                            SplitReads.READ_GROUP_SHORT_NAME,
                            SplitReads.LIBRARY_NAME_SHORT_NAME),
                            bySampleAndRGAndLibrary
                    },
                    new Object[]{typeIn, typeOut, TEST_DATA_MISSING_LIB__PREFIX, Collections.singletonList(
                            SplitReads.LIBRARY_NAME_SHORT_NAME),
                            byUnknown
                    }
            );
        };

        return getInputOutputTypes()
                .map(argTests)
                .flatMap(Function.identity())
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "splitReadsData")
    public void testSplitReadsByReadGroup(final SamReader.Type typeIn,
                                          final SamReader.Type typeOut,
                                          final String baseName,
                                          final List<String> splitArgs,
                                          final Map<String, Integer> splitCounts) throws Exception {
        final String inFileExtension = "." + typeIn.fileExtension();
        final String outFileExtension =
                "." + Optional.ofNullable(typeOut).map(SamReader.Type::fileExtension).orElse("default");
        final List<String> args = new ArrayList<>();

        Path outputDir = Files.createTempDirectory(
                splitArgs.stream().reduce(baseName, (acc, arg) -> acc + "." + arg)
                        + inFileExtension + outFileExtension+ "."
        );
        outputDir.toFile().deleteOnExit();

        args.add("-"+ StandardArgumentDefinitions.INPUT_SHORT_NAME);
        args.add(getTestDataDir() + "/" + baseName + inFileExtension);

        args.add("-"+ StandardArgumentDefinitions.OUTPUT_SHORT_NAME );
        args.add(outputDir.toString());

        if (isReferenceRequired(typeIn) || isReferenceRequired(typeOut)) {
            args.add("-" + StandardArgumentDefinitions.REFERENCE_SHORT_NAME );
            args.add(getTestDataDir()+ "/" + getReferenceSequenceName(baseName));
        }

        final String expectedFileExtension;
        if (typeOut == null) {
            expectedFileExtension = inFileExtension;
        } else {
            args.add("-" + SplitReads.OUTPUT_EXTENSION_SHORT_NAME);
            args.add(typeOut.fileExtension());
            expectedFileExtension = "." + typeOut.fileExtension();
        }

        splitArgs.forEach(arg -> {
            args.add("-" + arg);
        });

        Assert.assertNull(runCommandLine(args));

        for (final Map.Entry<String, Integer> splitCount: splitCounts.entrySet()) {
            final String outputFileName = baseName + splitCount.getKey() + expectedFileExtension;
            Assert.assertEquals(
                    getReadCounts(outputDir, baseName, outputFileName),
                    (int)splitCount.getValue(),
                    "unexpected read count for " + outputFileName);
        }
    }

    private int getReadCounts(final Path tempDirectory, final String baseName, final String fileName) {
        final File path = tempDirectory.resolve(fileName).toFile();
        IOUtil.assertFileIsReadable(path);
        final SamReader in = SamReaderFactory.makeDefault().referenceSequence(new File(getTestDataDir(), getReferenceSequenceName(baseName))).open(path);
        int count = 0;
        for (@SuppressWarnings("unused") final SAMRecord rec : in) {
            count++;
        }
        CloserUtil.close(in);
        return count;
    }

    private static Stream<SamReader.Type> getSamReaderTypes() {
        return Stream
                .of(SamReader.Type.class.getFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> f.getType().isAssignableFrom(SamReader.Type.class))
                .map(f -> orNull(() -> f.get(null)))
                .filter(v -> v instanceof SamReader.Type)
                .map(v -> (SamReader.Type) v)
                .filter(v -> !v.fileExtension().equals("sra")); // exclude SRA file types until we have tests
    }

    private static Stream<SamReader.Type> getSamReaderTypesWithNull() {
        final SamReader.Type nullType = null;
        return Stream.concat(getSamReaderTypes(), Stream.of(nullType));
    }

    private static Stream<SamReader.Type[]> getInputOutputTypes() {
        return getSamReaderTypes().flatMap(typeIn ->
                getSamReaderTypesWithNull().map(typeOut -> new SamReader.Type[]{typeIn, typeOut})
        );
    }

    private static <V> V orNull(final Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            return null;
        }
    }
}
