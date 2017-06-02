package org.broadinstitute.hellbender.utils.reference;

import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import htsjdk.samtools.SAMSequenceDictionary;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.gcs.BucketUtils;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;
import java.util.*;

import java.io.*;


public class ReferenceUtilsUnitTest extends BaseTest {

    @Test
    public void testLoadFastaDictionaryFromFile() {
        final File referenceDictionaryFile = new File(ReferenceUtils.getFastaDictionaryFileName(hg19MiniReference));
        final SAMSequenceDictionary dictionary = ReferenceUtils.loadFastaDictionary(referenceDictionaryFile);

        Assert.assertNotNull(dictionary, "Sequence dictionary null after loading");
        Assert.assertEquals(dictionary.size(), 4, "Wrong sequence dictionary size after loading");
    }

    private static final class ClosingAwareFileInputStream extends FileInputStream {
        private boolean isClosed;

        public ClosingAwareFileInputStream( final File file ) throws FileNotFoundException {
            super(file);
            isClosed = false;
        }

        @Override
        public void close() throws IOException {
            super.close();
            isClosed = true;
        }

        public boolean isClosed() {
            return isClosed;
        }
    }

    @Test
    public void testLoadFastaDictionaryFromStream() throws IOException {
        try ( final ClosingAwareFileInputStream referenceDictionaryStream = new ClosingAwareFileInputStream(new File(ReferenceUtils.getFastaDictionaryFileName(hg19MiniReference))) ) {
            final SAMSequenceDictionary dictionary = ReferenceUtils.loadFastaDictionary(referenceDictionaryStream);

            Assert.assertNotNull(dictionary, "Sequence dictionary null after loading");
            Assert.assertEquals(dictionary.size(), 4, "Wrong sequence dictionary size after loading");

            Assert.assertFalse(referenceDictionaryStream.isClosed(), "InputStream was improperly closed by ReferenceUtils.loadFastaDictionary()");
        }
    }

    @DataProvider(name = "testData_testLoadWrongFormatFastaDictionary")
    public Object[][] testData_testLoadWrongFormatFastaDictionary(final Method testMethod) {

        Object[][] tests = {
                {hg19MiniReference, true},
                {hg19MiniReference, false}
        };

        return tests;
    }

    @Test(expectedExceptions = UserException.MalformedFile.class, dataProvider = "testData_testLoadWrongFormatFastaDictionary")
    public void testLoadWrongFormatFastaDictionary(final String fastaFileName, boolean doLoadAsStream ) throws IOException {

        File testFastaFile = new File(fastaFileName);

        if ( doLoadAsStream ) {
            // Test loadFastaDictionary with Stream argument:
            try ( final ClosingAwareFileInputStream referenceDictionaryStream = new ClosingAwareFileInputStream(testFastaFile) ) {
                ReferenceUtils.loadFastaDictionary(referenceDictionaryStream);
            }
        }
        else {
            // Test loadFastaDictionary with File argument:
            ReferenceUtils.loadFastaDictionary(testFastaFile);
        }
    }

    @Test(groups = {"bucket"})
    public void testLoadFastaDictionaryFromGCSBucket() throws IOException {
        final String bucketDictionary = getGCPTestInputPath() + "org/broadinstitute/hellbender/utils/ReferenceUtilsTest.dict";
        final PipelineOptions popts = getAuthenticatedPipelineOptions();

        try ( final InputStream referenceDictionaryStream = BucketUtils.openFile(bucketDictionary) ) {
            final SAMSequenceDictionary dictionary = ReferenceUtils.loadFastaDictionary(referenceDictionaryStream);

            Assert.assertNotNull(dictionary, "Sequence dictionary null after loading");
            Assert.assertEquals(dictionary.size(), 4, "Wrong sequence dictionary size after loading");
        }
    }
}
