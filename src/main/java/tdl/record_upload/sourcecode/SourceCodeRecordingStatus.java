package tdl.record_upload.sourcecode;

import org.slf4j.Logger;
import tdl.record.sourcecode.metrics.SourceCodeRecordingMetricsCollector;
import tdl.record_upload.MonitoredSubject;

import java.util.concurrent.TimeUnit;

public class SourceCodeRecordingStatus implements MonitoredSubject {

    private SourceCodeRecordingMetricsCollector sourceCodeRecordingMetricsCollector;

    public SourceCodeRecordingStatus(SourceCodeRecordingMetricsCollector sourceCodeRecordingMetricsCollector) {
        this.sourceCodeRecordingMetricsCollector = sourceCodeRecordingMetricsCollector;
    }

    @Override
    public boolean isActive() {
        return sourceCodeRecordingMetricsCollector.isCurrentlyRecording();
    }

    @Override
    public void displayErrors(Logger log) {
        // No error
    }

    @Override
    public void displayMetrics(StringBuilder displayBuffer) {
        displayBuffer.append(
                String.format("%2d source capture%s, %3d ms/capture",
                        sourceCodeRecordingMetricsCollector.getTotalSnapshots(),
                        maybePlural(sourceCodeRecordingMetricsCollector.getTotalSnapshots()),
                        TimeUnit.NANOSECONDS.toMillis(sourceCodeRecordingMetricsCollector.getLastSnapshotProcessingTimeNano()))
        );
    }

    private static String maybePlural(long value) {
        return value > 1 ? "s" : "";
    }
}
