package tdl.record_upload;

import lombok.extern.slf4j.Slf4j;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.s3.sync.progress.UploadStatsProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
class MetricsReportingTask {
    private static final NumberFormat percentageFormatter = NumberFormat.getPercentInstance();
    private static final NumberFormat sizeFormatter = NumberFormat.getNumberInstance();
    private static final NumberFormat uploadSpeedFormatter = NumberFormat.getNumberInstance();
    private static final DateTimeFormatter durationFormatter = DateTimeFormatter.ofPattern("H'h'mm'm'ss's'");

    static {
        setFormatter(percentageFormatter, 1);
        setFormatter(sizeFormatter, 2);
        setFormatter(uploadSpeedFormatter, 3);
    }
    private static final double RENDERER_LOAD_THRESHOLD = 0.90;

    private static void setFormatter(NumberFormat formatter, int digits) {
        formatter.setMinimumFractionDigits(digits);
        formatter.setMaximumFractionDigits(digits);
    }

    private final Timer metricsTimer;
    private final UploadStatsProgressListener uploadStatsProgressListener;
    private final RecordingMetricsCollector recordingMetricsCollector;
    private final StringBuilder displayBuffer;
    private final Formatter stringFormatter;
    private int runCounter;

    MetricsReportingTask(RecordingMetricsCollector recordingMetricsCollector,
                         UploadStatsProgressListener uploadStatsProgressListener) {
        this.runCounter = 0;
        this.recordingMetricsCollector = recordingMetricsCollector;
        this.uploadStatsProgressListener = uploadStatsProgressListener;
        this.metricsTimer = new Timer("MetricsTask");
        this.displayBuffer = new StringBuilder();
        this.stringFormatter = new Formatter(displayBuffer);
    }

    void scheduleReportMetricsEvery(Duration delayBetweenRuns) {
        metricsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runCounter++;
                checkForRecordingDrift();
                displayMetrics();
            }
        }, 0, delayBetweenRuns.toMillis());
    }

    private void checkForRecordingDrift() {
        if (onceEveryCoupleOfRuns() &&
                recordingMetricsCollector.isCurrentlyRecording() &&
                recordingMetricsCollector.getRenderingTimeRatio() > RENDERER_LOAD_THRESHOLD) {
            log.warn(String.format("Renderer load is above %s, the recording will experience time drift",
                    percentageFormatter.format(RENDERER_LOAD_THRESHOLD)));
        }
    }

    private void displayMetrics() {
        displayBuffer.setLength(0);

        if (recordingMetricsCollector.isCurrentlyRecording()) {
            // Compute duration
            int fps = recordingMetricsCollector.getInputFrameRate().getDenominator();
            long recordedSeconds = recordingMetricsCollector.getTotalFrames() / fps;
            LocalTime recodedTime = LocalTime.MIDNIGHT.plus(Duration.ofSeconds(recordedSeconds));

            // Compute filesize
            long fileSize = 0;
            try {
                fileSize = Files.size(recordingMetricsCollector.getDestinationPath());
            } catch (IOException e) {
                log.debug("Could not obtain filesize information");
            }

            stringFormatter.format("Recorded %8s, %5d frames, %6s MB",
                    durationFormatter.format(recodedTime),
                    recordingMetricsCollector.getTotalFrames(),
                    sizeFormatter.format(bytes_to_mb(fileSize)));
        }

        if (recordingMetricsCollector.isCurrentlyRecording() && uploadStatsProgressListener.isCurrentlyUploading()) {
            displayBuffer.append(" | ");
        }

        if (uploadStatsProgressListener.isCurrentlyUploading()) {
            uploadStatsProgressListener.getCurrentStats().ifPresent(fileUploadStat ->
                    stringFormatter.format("Uploaded %5s of %6s MB at %5s MB/sec",
                            percentageFormatter.format(fileUploadStat.getUploadRatio()),
                            sizeFormatter.format(bytes_to_mb(fileUploadStat.getTotalSize())),
                            uploadSpeedFormatter.format(fileUploadStat.getMBps())));
        }

        if(displayBuffer.length() > 0){
            log.info(displayBuffer.toString());
        }
    }

    //~~~~ Helpers

    private boolean onceEveryCoupleOfRuns() {
        return runCounter % 20 == 0;
    }

    private static double bytes_to_mb(double totalSize) {
        return totalSize/((double)1024*1024);
    }


    void cancel() {
        metricsTimer.cancel();
    }
}
