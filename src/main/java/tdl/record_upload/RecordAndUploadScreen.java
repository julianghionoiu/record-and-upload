package tdl.record_upload;

import ch.qos.logback.classic.LoggerContext;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.record_upload.logging.LockableFileLoggingAppender;
import tdl.s3.credentials.AWSSecretProperties;
import tdl.s3.sync.destination.Destination;
import tdl.s3.sync.destination.DestinationOperationException;
import tdl.s3.sync.destination.S3BucketDestination;
import tdl.s3.sync.progress.UploadStatsProgressListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
public class RecordAndUploadScreen {

    private static class Params {
        @Parameter(names = {"-s", "--store"}, description = "The folder that will store the recordings")
        private String localStorageFolder = "./build/play/userX";

        @Parameter(names = {"-c", "--config"}, description = "The file containing the AWS parameters")
        private String configFile = ".private/aws-test-secrets";
    }


    public static void main(String[] args)  {
        log.info("Starting recording app");
        Params params = new Params();
        new JCommander(params, args);

        try {
            // Prepare source folder
            createMissingParentDirectories(params.localStorageFolder);
            removeOldLocks(params.localStorageFolder);
            startFileLogging(params.localStorageFolder);

            // Prepare remote destination
            AWSSecretProperties awsSecretProperties = AWSSecretProperties
                    .fromPlainTextFile(Paths.get(params.configFile));
            Destination s3BucketDestination =  S3BucketDestination.builder()
                    .awsClient(awsSecretProperties.createClient())
                    .bucket(awsSecretProperties.getS3Bucket())
                    .prefix(awsSecretProperties.getS3Prefix())
                    .build();

            // Validate destination
            log.info("Checking permissions");
            s3BucketDestination.testUploadPermissions();

            // Start processing
            run(params.localStorageFolder, s3BucketDestination);
        } catch (DestinationOperationException e) {
            log.error("User does not have enough permissions to upload. Reason: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Exception encountered. Stopping now.", e);
        }
    }

    private static final DateTimeFormatter fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static void run(String localStorageFolder, Destination remoteDestination) throws Exception {
        // Start the recording
        String timestamp = LocalDateTime.now().format(fileTimestampFormatter);
        File recordingFile = Paths.get(localStorageFolder, "recording_"+timestamp+".mp4").toFile();
        RecordingMetricsCollector recordingMetricsCollector = new RecordingMetricsCollector();
        VideoRecordingThread videoRecordingThread = new VideoRecordingThread(recordingFile, recordingMetricsCollector);
        videoRecordingThread.start();

        // Start sync folder
        UploadStatsProgressListener uploadStatsProgressListener = new UploadStatsProgressListener();
        BackgroundRemoteSyncTask remoteSyncTask = new BackgroundRemoteSyncTask(
                localStorageFolder, remoteDestination, uploadStatsProgressListener);
        remoteSyncTask.scheduleSyncEvery(Duration.of(5, ChronoUnit.MINUTES));

        // Start the metrics reporting
        MetricsReportingTask metricsReportingTask = new MetricsReportingTask(
                recordingMetricsCollector, uploadStatsProgressListener);
        metricsReportingTask.scheduleReportMetricsEvery(Duration.of(2, ChronoUnit.SECONDS));

        // Stop gracefully
        registerShutdownHook(videoRecordingThread);
        videoRecordingThread.join();
        stopFileLogging();
        remoteSyncTask.finalRun();
        metricsReportingTask.cancel();
    }

    private static void registerShutdownHook(final VideoRecordingThread videoRecordingThread) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("Shutdown signal received");
            videoRecordingThread.signalStop();
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                log.error("Could not join main thread.  Stopping now.", e);
            }
        }, "ShutdownHook"));
    }

    // ~~~~~ Helpers

    private static void startFileLogging(String localStorageFolder) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        LockableFileLoggingAppender.addToContext(loggerContext, localStorageFolder);
    }


    private static void stopFileLogging() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        LockableFileLoggingAppender.removeFromContext(loggerContext);
    }


    private static void createMissingParentDirectories(String storageFolder) throws IOException {
        File folder = new File(storageFolder);
        if (folder.exists()) {
            return;
        }

        boolean folderCreated = folder.mkdirs();
        if(!folderCreated) {
            throw new IOException("Failed to created storage folder");
        }
    }

    private static void removeOldLocks(String localStorageFolder) {
        Path rootPath = Paths.get(localStorageFolder);
        try {
            //noinspection ResultOfMethodCallIgnored
            Files.walk(rootPath)
                    .filter(path -> path.getFileName().toString().endsWith(".lock"))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.error("Failed to clean old locks", e);
        }
    }
}
