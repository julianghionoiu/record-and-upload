package tdl.recordupload;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
public class RecordAndUploadScreen {
    @Parameter(names = {"-i", "--unique-id"}, description = "An ID to be used for this session", required = true)
    private String id;

    @Parameter(names = {"-s", "--store"}, description = "The folder that will store the recordings")
    private String localStorageFolder = "./build/play";

    @Parameter(names = {"-c", "--config"}, description = "The file containing the AWS parameters")
    private String configFile = ".private/aws-test-secrets";


    public static void main(String[] args) throws Exception {
        log.info("Starting recording app");
        RecordAndUploadScreen main = new RecordAndUploadScreen();
        new JCommander(main, args);
        main.run();
    }

    private void run() throws Exception {
        // Prepare the source folder


        // Configure the workers
        BackgroundRemoteSyncTask remoteSyncTask = new BackgroundRemoteSyncTask(configFile, localStorageFolder);
        String recordingFile = Paths.get(localStorageFolder, "recording.mp4").toAbsolutePath().toString();
        VideoRecordingThread videoRecordingThread = new VideoRecordingThread(recordingFile);

        // Start
        remoteSyncTask.scheduleSyncEvery(Duration.of(5, ChronoUnit.MINUTES));
        videoRecordingThread.start();

        // Stop gracefully
        registerShutdownHook(videoRecordingThread);
        videoRecordingThread.join();
        remoteSyncTask.finalRun();
    }


    private void registerShutdownHook(final VideoRecordingThread videoRecordingThread) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            videoRecordingThread.signalStop();
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                log.warn("Could not join main thread", e);
            }
        }, "ShutdownHook"));
    }
}
