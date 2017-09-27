package tdl.record_upload;

import lombok.extern.slf4j.Slf4j;
import tdl.record.sourcecode.content.CopyFromDirectorySourceCodeProvider;
import tdl.record.sourcecode.metrics.SourceCodeRecordingMetricsCollector;
import tdl.record.sourcecode.record.SourceCodeRecorder;
import tdl.record.sourcecode.record.SourceCodeRecorderException;
import tdl.record.sourcecode.time.SystemMonotonicTimeSource;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SourceCodeRecordingThread extends Thread {
    private static final Duration MAX_RECORDING_DURATION = Duration.of(12, ChronoUnit.HOURS);

    private final SourceCodeRecorder sourceCodeRecorder;

    SourceCodeRecordingThread(Path sourceCodeFolder, Path sourceCodeRecordingFile, SourceCodeRecordingMetricsCollector recordingMetricsCollector) {
        super("SourceCodeRecThread");

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
        } catch (SourceCodeRecorderException e) {
            log.error("SourceCode recorder encountered exception. Recording has been stopped.", e);
        }
    }

    void signalStop() {
        sourceCodeRecorder.stop();
    }
}
