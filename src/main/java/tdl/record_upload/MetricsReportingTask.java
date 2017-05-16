package tdl.record_upload;

import lombok.extern.slf4j.Slf4j;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.s3.sync.progress.UploadStatsProgressListener;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
class MetricsReportingTask {
    private static final NumberFormat percentageFormatter = NumberFormat.getPercentInstance();
    private static final NumberFormat uploadSpeedFormatter = NumberFormat.getNumberInstance();
    static {
        percentageFormatter.setMinimumFractionDigits(1);
        uploadSpeedFormatter.setMinimumFractionDigits(1);
    }


    private final Timer metricsTimer;
    private final UploadStatsProgressListener uploadStatsProgressListener;
    private final RecordingMetricsCollector recordingMetricsCollector;
    private final StringBuilder displayBuffer;

    MetricsReportingTask(RecordingMetricsCollector recordingMetricsCollector,
                         UploadStatsProgressListener uploadStatsProgressListener) {
        this.recordingMetricsCollector = recordingMetricsCollector;
        this.uploadStatsProgressListener = uploadStatsProgressListener;
        this.metricsTimer = new Timer("MetricsTask");
        this.displayBuffer = new StringBuilder();
    }

    void scheduleReportMetricsEvery(Duration delayBetweenRuns) {
        metricsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                displayMetrics();
            }
        }, 0, delayBetweenRuns.toMillis());
    }

    private void displayMetrics() {
        displayBuffer.setLength(0);

        if (recordingMetricsCollector.isCurrentlyRecording()) {
            displayBuffer.append("Recorded ").append(recordingMetricsCollector.getTotalFrames()).append(" frames");
            displayBuffer.append(", load is ").append(percentageFormatter.format(recordingMetricsCollector.getRenderingTimeRatio()));
            displayBuffer.append(" | ");
        }

        if (uploadStatsProgressListener.isCurrentlyUploading()) {
            uploadStatsProgressListener.getCurrentStats().ifPresent(fileUploadStat -> displayBuffer.append("Uploaded: ").append(percentageFormatter.format(fileUploadStat.getUploadRatio())).append(". ")
                    .append(fileUploadStat.getUploadedSize()).append("/").append(fileUploadStat.getTotalSize()).append(" bytes. ")
                    .append(uploadSpeedFormatter.format(fileUploadStat.getMBps())).append(" Mbps"));
        }

        if(displayBuffer.length() > 0){
            log.info(displayBuffer.toString());
        }
    }


    void cancel() {
        metricsTimer.cancel();
    }
}
