package tdl.record_upload.sourcecode;

import lombok.extern.slf4j.Slf4j;
import tdl.record.sourcecode.content.CopyFromDirectorySourceCodeProvider;
import tdl.record.sourcecode.metrics.SourceCodeRecordingMetricsCollector;
import tdl.record.sourcecode.record.SourceCodeRecorder;
import tdl.record.sourcecode.record.SourceCodeRecorderException;
import tdl.record.sourcecode.time.SystemMonotonicTimeSource;
import tdl.record_upload.Stoppable;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SourceCodeRecordingThread extends Thread implements Stoppable {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);

    private final SourceCodeRecorder sourceCodeRecorder;

    public SourceCodeRecordingThread(Path sourceCodeFolder, Path sourceCodeRecordingFile, SourceCodeRecordingMetricsCollector recordingMetricsCollector) {
        super("SourceRec");

        CopyFromDirectorySourceCodeProvider sourceCodeProvider = new CopyFromDirectorySourceCodeProvider(
                sourceCodeFolder);
        sourceCodeRecorder = new SourceCodeRecorder.Builder(sourceCodeProvider, sourceCodeRecordingFile)
                .withTimeSource(new SystemMonotonicTimeSource())
                .withSnapshotEvery(3, TimeUnit.MINUTES)
                .withKeySnapshotSpacing(10)
                .withRecordingListener(recordingMetricsCollector)
                .build();
    }

    @Override
    public void run() {
        try {
            sourceCodeRecorder.start(MAX_RECORDING_DURATION);
            sourceCodeRecorder.close();
        } catch (Exception e) {
            log.error("SourceCode recorder encountered exception. Recording has been stopped.", e);
        }
    }

    public void tagCurrentState(String tag) {
        sourceCodeRecorder.tagCurrentState(tag);
    }

    @Override
    public void signalStop() {
        sourceCodeRecorder.stop();
    }
}
