package tdl.record_upload;

import lombok.extern.slf4j.Slf4j;
import tdl.record.image.input.InputFromScreen;
import tdl.record.image.input.ScaleToOptimalSizeImage;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.record.utils.ImageQualityHint;
import tdl.record.video.VideoRecorder;
import tdl.record.video.VideoRecorderException;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class VideoRecordingThread extends Thread {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);

    private final VideoRecorder videoRecorder;
    private File recordingFile;

    VideoRecordingThread(File recordingFile, RecordingMetricsCollector recordingMetricsCollector) {
        super("RecordThread");
        this.recordingFile = recordingFile;

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
        } catch (VideoRecorderException e) {
            log.error("Video recorder encountered exception. Recording has been stopped.", e);
        }
    }

    void signalStop() {
        videoRecorder.stop();
    }
}
