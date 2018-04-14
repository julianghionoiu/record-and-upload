package tdl.record_upload.video;

import lombok.extern.slf4j.Slf4j;
import tdl.record.screen.image.input.InputFromScreen;
import tdl.record.screen.image.input.ScaleToOptimalSizeImage;
import tdl.record.screen.metrics.VideoRecordingMetricsCollector;
import tdl.record.screen.utils.ImageQualityHint;
import tdl.record.screen.video.VideoRecorder;
import tdl.record_upload.Stoppable;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
public class VideoRecordingThread extends Thread implements Stoppable {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);

    private final VideoRecorder videoRecorder;
    private File recordingFile;

    public VideoRecordingThread(File videoRecordingFile, VideoRecordingMetricsCollector recordingMetricsCollector) {
        super("VideoRec");
        this.recordingFile = videoRecordingFile;

        videoRecorder = new VideoRecorder
                .Builder(new ScaleToOptimalSizeImage(ImageQualityHint.MEDIUM, new InputFromScreen()))
                .withRecordingListener(recordingMetricsCollector)
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
}
