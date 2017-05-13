package tdl.recordupload;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import tdl.record.image.input.InputFromScreen;
import tdl.record.image.input.ScaleToOptimalSizeImage;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.record.utils.ImageQualityHint;
import tdl.record.video.VideoRecorder;
import tdl.record.video.VideoRecorderException;
import tdl.s3.RemoteSync;
import tdl.s3.cli.ProgressStatus;
import tdl.s3.sync.Destination;
import tdl.s3.sync.Filters;
import tdl.s3.sync.Source;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RecordAndUploadScreen {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);

    @Parameter(names = {"-d", "--destination"}, description = "The folder that will store the recordings")
    private String destinationFolder = "./build/play";

    @Parameter(names = {"-c", "--config"}, description = "The file containing the AWS parameters")
    private String configFile = ".private/aws-test-secrets";


    public static void main(String[] args) throws VideoRecorderException {
        log.info("Starting recording app");
        RecordAndUploadScreen main = new RecordAndUploadScreen();
        new JCommander(main, args);
        main.run();
    }

    private Source fromFolder(String uploadsFolder) {
        Filters filters = Filters.getBuilder()
                .include(Filters.endsWith(".mp4"))
                .create();
        return Source.getBuilder(Paths.get(uploadsFolder))
                .setFilters(filters)
                .create();
    }

    private Destination toAwsS3() {
        return Destination.getBuilder()
                .loadFromPath(Paths.get(configFile))
                .create();
    }

    private void run() throws VideoRecorderException {
        RemoteSync remoteSync = new RemoteSync(fromFolder(destinationFolder), toAwsS3());

        ProgressStatus progressBar = new ProgressStatus();
        remoteSync.setListener(progressBar);


        RecordingMetricsCollector recordingMetricsCollector = new RecordingMetricsCollector();
        VideoRecorder videoRecorder = new VideoRecorder
                .Builder(new ScaleToOptimalSizeImage(ImageQualityHint.MEDIUM, new InputFromScreen()))
                .withRecordingListener(recordingMetricsCollector)
                .build();

        //Issue performance updates
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Recorded " + recordingMetricsCollector.getTotalFrames() + " frames"
                        + " at " + recordingMetricsCollector.getVideoFrameRate().getDenominator() + " fps"
                        + " with a load of " + recordingMetricsCollector.getRenderingTimeRatio());
            }
        }, 0, Duration.of(5, ChronoUnit.SECONDS).toMillis());

        ReentrantLock syncLock = new ReentrantLock();


        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean shouldSync = syncLock.tryLock();
                if (shouldSync) {
                    try {
                        System.out.println("Sync with remote");
                        remoteSync.run();
                    } finally {
                        syncLock.unlock();
                    }
                } else {
                    System.out.println("Sync already in progress.");
                }
            }
        }, 0, Duration.of(5, ChronoUnit.MINUTES).toMillis());
        registerShutdownHook(videoRecorder, timer);


        String recordingFile = Paths.get(destinationFolder, "recording.mp4").toAbsolutePath().toString();
        videoRecorder.open(recordingFile, 4, 4);
        videoRecorder.start(MAX_RECORDING_DURATION);
        videoRecorder.close();
        timer.cancel();

        // SYnc one more time before stopping
        System.out.println("Upload remaining parts and finalise video.");
        syncLock.lock();
        remoteSync.run();
    }


    private void registerShutdownHook(final VideoRecorder videoRecorder, Timer timer) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timer.cancel();
            videoRecorder.stop();
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                log.warn("Could not join main thread", e);
            }
        }));
    }
}
