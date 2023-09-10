package tdl.record_upload;

import com.mashape.unirest.http.Unirest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class RecordAndUploadAppTest {

    @TempDir
    Path tempFolder;

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
        // Prepare output folder
        String storagePath = tempFolder.toString();
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

        // Assert on the generated log
        List<File> logFiles = Files.list(tempFolder).filter(path -> path.endsWith(".log")).map(path -> path.toFile()).collect(Collectors.toList());
        assertThat("Logs are generated and rotated before final upload",
                Objects.requireNonNull(logFiles).size(), is(2));

        String logContents = readFile(logFiles.get(0)) + readFile(logFiles.get(1));
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

    private String readFile(File logFile) throws IOException {
        return new String(Files.readAllBytes(logFile.toPath()));
    }

    //~~~ Helpers

    private static int secondsToMillis(int secondsToInitialize) {
        return secondsToInitialize * 1000;
    }
}
