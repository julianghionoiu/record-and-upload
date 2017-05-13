package tdl.record_upload;

import tdl.s3.RemoteSync;
import tdl.s3.cli.ProgressStatus;
import tdl.s3.sync.Destination;
import tdl.s3.sync.Filters;
import tdl.s3.sync.Source;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BackgroundRemoteSyncTask {
    private final Timer timer;
    private Lock syncLock;
    private final RemoteSync remoteSync;

    BackgroundRemoteSyncTask(String configFilePath, String localStorageFolder) {
        Filters filters = Filters.getBuilder()
                .include(Filters.endsWith(".mp4"))
                .include(Filters.endsWith(".log"))
                .create();
        Source localFolder = Source.getBuilder(Paths.get(localStorageFolder))
                .setFilters(filters)
                .create();
        Destination s3Bucket = Destination.getBuilder()
                .loadFromPath(Paths.get(configFilePath))
                .create();

        remoteSync = new RemoteSync(localFolder, s3Bucket);
        ProgressStatus progressBar = new ProgressStatus();
        remoteSync.setListener(progressBar);

        timer = new Timer("SyncTask");
        syncLock = new ReentrantLock();
    }

    void scheduleSyncEvery(Duration delayBetweenRuns) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean shouldSync = syncLock.tryLock();
                if (shouldSync) {
                    try {
                        System.out.println("Sync with remote");
                        remoteSync.run();
                    } finally {
                        syncLock.unlock();
                    }
                } else {
                    System.out.println("Sync already in progress.");
                }
            }
        }, 0, delayBetweenRuns.toMillis());
    }

    void finalRun() {
        System.out.println("Upload remaining parts and finalise video.");
        syncLock.lock();
        timer.cancel();
        remoteSync.run();
    }
}
