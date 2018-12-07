package tdl.record_upload;

import ch.qos.logback.classic.LoggerContext;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import tdl.record_upload.events.ExternalEventServerThread;
import tdl.record_upload.logging.LockableFileLoggingAppender;
import tdl.record_upload.sourcecode.NoOpSourceCodeThread;
import tdl.record_upload.sourcecode.SourceCodeRecordingThread;
import tdl.record_upload.upload.BackgroundRemoteSyncTask;
import tdl.record_upload.upload.NoOpDestination;
import tdl.record_upload.upload.UploadStatsProgressStatus;
import tdl.record_upload.video.NoOpVideoThread;
import tdl.record_upload.video.VideoRecordingThread;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class RecordAndUploadApp {

    private static class Params {
        @Parameter(names = {"--store"}, description = "The folder that will store the recordings")
        private String localStorageFolder = "./build/play/userX";

        @Parameter(names = {"--config"}, description = "The file containing the AWS parameters")
        private String configFile = ".private/aws-test-secrets";

        @Parameter(names = {"--sourcecode"}, description = "The folder that contains the source code that needs to be tracked")
        private String localSourceCodeFolder = ".";

        @Parameter(names = "--no-video", description = "Disable video recording")
        private boolean doNotRecordVideo = false;

        @Parameter(names = "--no-sourcecode", description = "Disable source code recording")
        private boolean doNotRecordSourceCode = false;

        @Parameter(names = "--no-sync", description = "Do not sync target folder")
        private boolean doNotSync = false;

        //~~ Test only

        @Parameter(names = "--soft-stop", description = "Attempt to stop without killing the JVM")
        private boolean doSoftStop = false;
    }


    public static void main(String[] args)  {
        log.info("Starting recording app");
        Params params = new Params();
        JCommander jCommander = new JCommander(params);
        jCommander.parse(args);

        try {
            // Prepare source folder
            createMissingParentDirectories(params.localStorageFolder);
            removeOldLocks(params.localStorageFolder);
            startFileLogging(params.localStorageFolder);

            // Prepare remote destination
            AWSSecretProperties awsSecretProperties = AWSSecretProperties
                    .fromPlainTextFile(Paths.get(params.configFile));

            boolean syncFolder = !params.doNotSync;
            Destination uploadDestination;
            if (syncFolder) {
                uploadDestination = S3BucketDestination.builder()
                        .awsClient(awsSecretProperties.createClient())
                        .bucket(awsSecretProperties.getS3Bucket())
                        .prefix(awsSecretProperties.getS3Prefix())
                        .build();
            } else {
                uploadDestination = new NoOpDestination();
            }


            // Validate destination
            log.info("Checking permissions");
            uploadDestination.testUploadPermissions();

            // Timestamp
            String timestamp = LocalDateTime.now().format(fileTimestampFormatter);

            // Video recording
            boolean recordVideo = !params.doNotRecordVideo;
            MonitoredBackgroundTask videoRecordingTask;
            if (recordVideo) {
                File screenRecordingFile = Paths.get(
                        params.localStorageFolder,
                        String.format("screencast_%s.mp4", timestamp)
                ).toFile();
                videoRecordingTask = new VideoRecordingThread(screenRecordingFile);
            } else {
                videoRecordingTask = new NoOpVideoThread();
            }

            // Source code recording
            boolean recordSourceCode = !params.doNotRecordSourceCode;
            MonitoredBackgroundTask sourceCodeRecordingTask;
            if (recordSourceCode) {
                Path sourceCodeFolder = Paths.get(params.localSourceCodeFolder);
                Path sourceCodeRecordingFile = Paths.get(
                        params.localStorageFolder,
                        String.format("sourcecode_%s.srcs", timestamp)
                );
                sourceCodeRecordingTask = new SourceCodeRecordingThread(sourceCodeFolder, sourceCodeRecordingFile);
            } else {
                sourceCodeRecordingTask = new NoOpSourceCodeThread();
            }

            // Start processing
            run(params.localStorageFolder,
                    uploadDestination,
                    videoRecordingTask,
                    sourceCodeRecordingTask
            );
        } catch (DestinationOperationException e) {
            log.error("User does not have enough permissions to upload. Reason: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Exception encountered. Stopping now.", e);
        } finally {
            boolean hardStop = !params.doSoftStop;
            if (hardStop) {
                // Forcefully stop. A problem with Jetty finalisation might prevent the JVM from stopping
                Runtime.getRuntime().halt(0);
            }
        }
    }

    private static final DateTimeFormatter fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static void run(String localStorageFolder,
                            Destination remoteDestination,
                            MonitoredBackgroundTask videoRecordingTask, MonitoredBackgroundTask sourceCodeRecordingTask) throws Exception {
        List<Stoppable> serviceThreadsToStop = new ArrayList<>();
        List<MonitoredSubject> monitoredSubjects = new ArrayList<>();
        ExternalEventServerThread externalEventServerThread = new ExternalEventServerThread();

        // Start video and source code recording
        for (MonitoredBackgroundTask monitoredBackgroundTask:
                Arrays.asList(videoRecordingTask, sourceCodeRecordingTask)) {
            monitoredBackgroundTask.start();
            serviceThreadsToStop.add(monitoredBackgroundTask);
            monitoredSubjects.add(monitoredBackgroundTask);
            externalEventServerThread.addNotifyListener(monitoredBackgroundTask);
            externalEventServerThread.addStopListener(monitoredBackgroundTask);
        }

        // Start sync folder
        UploadStatsProgressListener uploadStatsProgressListener = new UploadStatsProgressListener();
        BackgroundRemoteSyncTask remoteSyncTask = new BackgroundRemoteSyncTask(
                localStorageFolder, remoteDestination, uploadStatsProgressListener);
        remoteSyncTask.scheduleSyncEvery(Duration.of(5, ChronoUnit.MINUTES));
        monitoredSubjects.add(new UploadStatsProgressStatus(uploadStatsProgressListener));

        // Start the metrics reporting
        MetricsReportingTask metricsReportingTask = new MetricsReportingTask(monitoredSubjects);
        metricsReportingTask.scheduleReportMetricsEvery(Duration.of(2, ChronoUnit.SECONDS));

        // Start the event server
        externalEventServerThread.start();

        // Wait for the stop signal and trigger a graceful shutdown
        registerShutdownHook(serviceThreadsToStop);
        for (Stoppable stoppable : serviceThreadsToStop) {
            stoppable.join();
        }

        // If all are joined, signal the event thread to stop
        externalEventServerThread.signalStop();

        // Finalise the upload and cancel tasks
        forceLoggingFileRotation(localStorageFolder);
        remoteSyncTask.finalRun();
        metricsReportingTask.cancel();

        // Join the event thread
        externalEventServerThread.join();
        log.info("~~~~~~ Stopped ~~~~~~");
        stopFileLogging();
    }

    private static void registerShutdownHook(List<Stoppable> servicesToStop) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("Shutdown signal received");
            try {
                for (Stoppable stoppable : servicesToStop) {
                    stoppable.signalStop();
                }
            } catch (Exception e) {
                log.error("Error sending the stop signals.", e);
            }

            try {
                mainThread.join();
            } catch (InterruptedException e) {
                log.error("Could not join main thread.  Stopping now.", e);
            }
        }, "Shutdown"));
    }

    // ~~~~~ Helpers

    private static void startFileLogging(String localStorageFolder) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        LockableFileLoggingAppender.addToContext(loggerContext, localStorageFolder);
    }

    private static void forceLoggingFileRotation(String localStorageFolder) {
        stopFileLogging();
        startFileLogging(localStorageFolder);
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
