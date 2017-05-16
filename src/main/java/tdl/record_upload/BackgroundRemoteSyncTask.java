package tdl.record_upload;

import lombok.extern.slf4j.Slf4j;
import tdl.s3.sync.Filters;
import tdl.s3.sync.RemoteSync;
import tdl.s3.sync.RemoteSyncException;
import tdl.s3.sync.Source;
import tdl.s3.sync.destination.Destination;
import tdl.s3.sync.destination.S3BucketDestination;
import tdl.s3.sync.progress.UploadStatsProgressListener;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
class BackgroundRemoteSyncTask {
    private final Timer syncTimer;
    private Lock syncLock;
    private final RemoteSync remoteSync;

    BackgroundRemoteSyncTask(String configFilePath, String localStorageFolder,
                             UploadStatsProgressListener uploadStatsProgressListener) {
        Filters filters = Filters.getBuilder()
                .include(Filters.endsWith(".mp4"))
                .include(Filters.endsWith(".log"))
                .create();
        Source localFolder = Source.getBuilder(Paths.get(localStorageFolder))
                .setFilters(filters)
                .create();
        Destination s3Bucket = S3BucketDestination.getBuilder()
                .loadFromPath(Paths.get(configFilePath))
                .create();

        remoteSync = new RemoteSync(localFolder, s3Bucket);
        remoteSync.setListener(uploadStatsProgressListener);

        syncTimer = new Timer("UploadTask");
        syncLock = new ReentrantLock();
    }

    void scheduleSyncEvery(Duration delayBetweenRuns) {
        syncTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean shouldSync = syncLock.tryLock();
                if (shouldSync) {
                    try {
                        log.info("Sync local files with remote");
                        remoteSync.run();
                    } catch (Exception e) {
                        log.warn("Remote sync failed. Will retry later.", e);
                    } finally {
                        syncLock.unlock();
                    }
                } else {
                    log.info("Sync already in progress. Skipping");
                }
            }
        }, 0, delayBetweenRuns.toMillis());
    }

    void finalRun() {
        log.info("Upload remaining parts and finalise video");
        syncLock.lock();
        try {
            remoteSync.run();
        } catch (RemoteSyncException e) {
            log.error("File upload failed. Some files might not have been uploaded.");
        } finally {
            syncLock.unlock();
        }
    }
}
