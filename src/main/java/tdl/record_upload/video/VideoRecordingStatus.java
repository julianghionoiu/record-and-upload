package tdl.record_upload.video;

import org.slf4j.Logger;
import tdl.record.screen.metrics.VideoRecordingMetricsCollector;
import tdl.record_upload.MonitoredSubject;

import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.slf4j.LoggerFactory.*;

class VideoRecordingStatus {
    private static final Logger log = getLogger(VideoRecordingStatus.class);
    private static final NumberFormat percentageFormatter = NumberFormat.getPercentInstance();
    private static final NumberFormat sizeFormatter = NumberFormat.getNumberInstance();
    private static final DateTimeFormatter durationFormatter = DateTimeFormatter.ofPattern("H'h'mm'm'ss's'");

    static {
        setFormatter(percentageFormatter, 1);
        setFormatter(sizeFormatter, 2);
    }

    private static void setFormatter(NumberFormat formatter, int digits) {
        formatter.setMinimumFractionDigits(digits);
        formatter.setMaximumFractionDigits(digits);
    }

    private static final double RENDERER_LOAD_THRESHOLD = 0.90;

    private int runCounter;
    private final VideoRecordingMetricsCollector videoRecordingMetricsCollector;

    VideoRecordingStatus(VideoRecordingMetricsCollector videoRecordingMetricsCollector) {
        this.videoRecordingMetricsCollector = videoRecordingMetricsCollector;
        this.runCounter = 0;
    }

    boolean isActive() {
        return videoRecordingMetricsCollector.isCurrentlyRecording();
    }

    void displayErrors(Logger log) {
        runCounter += 1;
        checkForRecordingDrift(log);
    }

    void displayMetrics(StringBuilder displayBuffer) {
        // Compute duration
        int fps = videoRecordingMetricsCollector.getInputFrameRate().getDenominator();
        long recordedSeconds = videoRecordingMetricsCollector.getTotalFrames() / fps;
        LocalTime recodedTime = LocalTime.MIDNIGHT.plus(Duration.ofSeconds(recordedSeconds));

        // Compute filesize
        long fileSize = 0;
        try {
            fileSize = Files.size(videoRecordingMetricsCollector.getDestinationPath());
        } catch (IOException e) {
            log.debug("Could not obtain filesize information");
        }

        displayBuffer.append(
                String.format("Recorded %8s, %3d frame%s, %4s MB",
                        durationFormatter.format(recodedTime),
                        videoRecordingMetricsCollector.getTotalFrames(),
                        maybePlural(videoRecordingMetricsCollector.getTotalFrames()),
                        sizeFormatter.format(bytes_to_mb(fileSize)))
        );
    }

    private void checkForRecordingDrift(Logger log) {
        if (onceEveryCoupleOfRuns(runCounter) &&
                videoRecordingMetricsCollector.isCurrentlyRecording() &&
                videoRecordingMetricsCollector.getRenderingTimeRatio() > RENDERER_LOAD_THRESHOLD) {
            log.warn(String.format("Renderer load is above %s, the recording will experience time drift",
                    percentageFormatter.format(RENDERER_LOAD_THRESHOLD)));
        }
    }

    //~~~ Helpers

    private static boolean onceEveryCoupleOfRuns(int runCounter) {
        return runCounter % 20 == 0;
    }

    private static String maybePlural(long value) {
        return value > 1 ? "s" : "";
    }

    private static double bytes_to_mb(double totalSize) {
        return totalSize/((double)1024*1024);
    }

}
