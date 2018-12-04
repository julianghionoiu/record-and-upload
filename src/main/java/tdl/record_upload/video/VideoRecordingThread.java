package tdl.record_upload.video;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import tdl.record.screen.image.input.InputFromScreen;
import tdl.record.screen.image.input.ScaleToOptimalSizeImage;
import tdl.record.screen.metrics.VideoRecordingMetricsCollector;
import tdl.record.screen.utils.ImageQualityHint;
import tdl.record.screen.video.VideoRecorder;
import tdl.record_upload.MonitoredBackgroundTask;
import tdl.record_upload.Stoppable;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
public class VideoRecordingThread extends Thread implements MonitoredBackgroundTask {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);

    private final VideoRecorder videoRecorder;
    private File recordingFile;
    private final VideoRecordingStatus videoRecordingStatus;

    public VideoRecordingThread(File videoRecordingFile) {
        super("VideoRec");
        this.recordingFile = videoRecordingFile;

        VideoRecordingMetricsCollector videoRecordingMetricsCollector = new VideoRecordingMetricsCollector();
        videoRecordingStatus = new VideoRecordingStatus(videoRecordingMetricsCollector);
        videoRecorder = new VideoRecorder
                .Builder(new ScaleToOptimalSizeImage(ImageQualityHint.MEDIUM, new InputFromScreen()))
                .withRecordingListener(videoRecordingMetricsCollector)
                .build();
    }

    @Override
    public void run() {
        try {
            videoRecorder.open(recordingFile.getAbsolutePath(), 4, 4);
            videoRecorder.start(MAX_RECORDING_DURATION);
            videoRecorder.close();
        } catch (Exception e) {
            log.error("Video recorder encountered exception. Recording has been stopped.", e);
        }
    }

    @Override
    public void signalStop() {
        videoRecorder.stop();
    }

    // ~~~~ Implement the monitoring interface

    @Override
    public boolean isActive() {
        return videoRecordingStatus.isActive();
    }

    @Override
    public void displayErrors(Logger log) {
        videoRecordingStatus.displayErrors(log);
    }

    @Override
    public void displayMetrics(StringBuilder displayBuffer) {
        videoRecordingStatus.displayMetrics(displayBuffer);
    }

    // ~~~~ Implement the external events interface

    @Override
    public void onExternalEvent(String eventPayload) {
        // Do nothing
    }
}
