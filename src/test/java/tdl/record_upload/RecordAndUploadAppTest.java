package tdl.record_upload;

import com.mashape.unirest.http.Unirest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Objects;

import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RecordAndUploadAppTest {

    private PrintStream orgStream   = null;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @SuppressWarnings("SameParameterValue")
    public class MainAppThread extends Thread {
        static final String RECORDING_INTERFACE = "http://localhost:41375";
        private String storageDirPath;

        MainAppThread(String storageDirPath) {
            super("Main");
            this.storageDirPath = storageDirPath;
        }

        public void run() {
            RecordAndUploadApp.main(new String[]{
                    "--store", storageDirPath,
                    "--no-video", "--no-sourcecode", "--no-sync",
                    "--soft-stop"
            });
        }

        String getStatus() throws Exception {
            return Unirest.get(RECORDING_INTERFACE +"/status").asString().getBody();
        }

        void sendNotify(String payload) throws Exception {
            Unirest.post(RECORDING_INTERFACE +"/notify").body(payload).asString();
        }

        void sendStop() throws Exception {
            Unirest.post(RECORDING_INTERFACE +"/stop").asString();
        }
    }

    @Test
    public void orchestratesMultipleThreads() throws Exception {
        runRecordAndUploadApp();

        // Assert on the generated log
        File[] logFiles = tempFolder.getRoot().listFiles((dir, name) -> name.endsWith(".log"));
        assertThat("Logs are generated and rotated before final upload",
                Objects.requireNonNull(logFiles).length, is(2));

        String logContents = readFile(logFiles[0]) + readFile(logFiles[1]);
        assertThat("starts the Main thread", logContents, containsString("[Main]"));
        assertThat("starts the Upload thread", logContents, containsString("[Upload]"));
        assertThat("starts the Metrics thread", logContents, containsString("[Metrics]"));

        assertThat("syncs with remote", logContents, containsString("Sync local files with remote"));
        assertThat("captures video frame 1", logContents, containsString("tick   1, video recoding"));
        assertThat("captures video frame 2", logContents, containsString("tick   2, video recoding"));
        assertThat("captures source code 1", logContents, containsString("frame no.  1, source code"));
        assertThat("captures source code 2", logContents, containsString("frame no.  2, source code"));

        assertThat("receives external notify payload", logContents, containsString("TheExternalTag"));

        assertThat("uploads remaining parts on shutdown", logContents,
                containsString("Upload remaining parts and finalise recording session"));
    }

    private void runRecordAndUploadApp() throws Exception {
        // Prepare output folder
        String storagePath = tempFolder.getRoot().getPath();
        System.out.println("Writing logs to "+storagePath);

        // Start the recording process
        MainAppThread appThread = new MainAppThread(storagePath);
        appThread.start();

        int secondsToInitialize = 5;
        System.out.printf("Wait %d seconds for the threads to start%n", secondsToInitialize);
        Thread.sleep(secondsToMillis(secondsToInitialize));

        // Check if server is running
        assertThat(appThread.getStatus(), is("OK\n"));

        // Send some notifications
        appThread.sendNotify("TheExternalTag");

        int secondsToRun = 5;
        System.out.printf("Wait %d seconds before sending the kill signal%n", secondsToRun);
        Thread.sleep(secondsToMillis(secondsToRun));

        System.out.println("Stopping the test by sending the stop command");
        appThread.sendStop();
        appThread.join();
    }

    @Test
    public void startAppIfDiskSpaceRequirementPasses() {
        try {
            String stdoutputLogFilePath = redirectStdOutToFile(tempFolder.getRoot().getPath());

            runRecordAndUploadApp();

            String logContents = readFile(new File(stdoutputLogFilePath));
            assertThat("Did not perform the disk space requirement check", logContents, containsString("Available disk space on the"));
            assertThat("Did not meet the disk space requirement check", logContents, containsString("Start S3 Sync session"));
        } catch (Exception ex) {
            fail("Should have NOT thrown an exception, if disk space requirements are met. Error message: " + ex.getMessage());
        } finally {
            resetStdOut();
        }
    }

    @Test
    public void abortAppIfDiskSpaceRequirementFails() {
        String stdoutputLogFilePath = "";
        try {
            environmentVariables.set("RECORD_AND_UPLOAD_MINIMUM_DISKSPACE", "100");
            stdoutputLogFilePath = redirectStdOutToFile(tempFolder.getRoot().getPath());

            runRecordAndUploadApp();

            String logContents = readFile(new File(stdoutputLogFilePath));
            assertThat("Did not perform the disk space requirement check", logContents, containsString("Available disk space on the"));
            assertThat("Did not fail when the disk space requirement was not met", logContents, containsString("Start S3 Sync session"));
        } catch (Exception ex) {
            fail("Should have NOT thrown an exception, test is failing during to another reason.");
        } finally {
            environmentVariables.set("RECORD_AND_UPLOAD_MINIMUM_DISKSPACE", "1");
            resetStdOut();
        }
    }

    private String redirectStdOutToFile(String storagePath) throws FileNotFoundException {
        String stdoutputLogFilePath = storagePath + File.separator + "recordAndUploadAppTest-stdout.logs";
        System.out.println("Redirecting stdout to a file stream: " + stdoutputLogFilePath);
        System.out.println("All messages (including logs) meant for stdout will be redirected to this file.");

        orgStream = System.out;
        PrintStream fileStream = new PrintStream(new FileOutputStream(stdoutputLogFilePath, true));

        System.setOut(fileStream);

        return stdoutputLogFilePath;
    }

    private void resetStdOut() {
        System.setOut(orgStream);

        System.out.println("Setting stdout back to its original stream.");
    }

    private String readFile(File logFile) throws IOException {
        return new String(Files.readAllBytes(logFile.toPath()));
    }

    //~~~ Helpers

    private static int secondsToMillis(int secondsToInitialize) {
        return secondsToInitialize * 1000;
    }
}
