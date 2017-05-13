package tdl.recordupload;

import lombok.extern.slf4j.Slf4j;
import tdl.record.image.input.InputFromScreen;
import tdl.record.image.input.ScaleToOptimalSizeImage;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.record.utils.ImageQualityHint;
import tdl.record.video.VideoRecorder;
import tdl.record.video.VideoRecorderException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class VideoRecordingThread extends Thread {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);

    private final VideoRecorder videoRecorder;
    private String recordingFile;
    private final RecordingMetricsCollector recordingMetricsCollector;
    private final Timer timer;

    VideoRecordingThread(String recordingFile) {
        super("Recorder");
        this.recordingFile = recordingFile;

        recordingMetricsCollector = new RecordingMetricsCollector();
        videoRecorder = new VideoRecorder
                .Builder(new ScaleToOptimalSizeImage(ImageQualityHint.MEDIUM, new InputFromScreen()))
                .withRecordingListener(recordingMetricsCollector)
                .build();

        timer = new Timer("RecordTask");
    }

    private void scheduleMetricsTask() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Recorded " + recordingMetricsCollector.getTotalFrames() + " frames"
                        + " at " + recordingMetricsCollector.getVideoFrameRate().getDenominator() + " fps"
                        + " with a load of " + recordingMetricsCollector.getRenderingTimeRatio());
            }
        }, 0, Duration.of(5, ChronoUnit.SECONDS).toMillis());
    }

    @Override
    public void run() {
        scheduleMetricsTask();

        try {
            videoRecorder.open(recordingFile, 4, 4);
            videoRecorder.start(MAX_RECORDING_DURATION);
            videoRecorder.close();
            timer.cancel();
        } catch (VideoRecorderException e) {
            log.error("Video recorder encountered exception. Recording has been stopped.", e);
        }
    }

    void signalStop() {
        timer.cancel();
        videoRecorder.stop();
    }
}
