package tdl.record_upload;

import org.junit.Ignore;
import org.junit.Test;

public class RecordAndUploadScreenTest {

    /**
     * Start the app
     *
     * Check output, you should see:
     *  - recording metrics for frames, load should be below 1
     *  - once every 5 minutes you should see a SyncTask uploading files
     *
     * Check storage folder, you should see:
     *  - a video file plus a corresponding .lock file
     *  - a log file plus a corresponding .lock file
     *
     * Stop the recording (CTRL+C), you should see:
     *  - a log message saying that the recording is stopping
     *  - upload messages for the video and the log file
     *
     * Check storage folder, you should see:
     *  - no .lock file
     *
     * Check AWS S3, you should see:
     *  - a video file and a log file
     *
     * Compare local video with remote video:
     *  - compute the md5sum of local video
     *  - download remote video and compute md5sum
     *  - md5 should match
     *
     * Visually inspect the remote video, check if:
     *  - the video quality is ok
     *  - the playback speed is 4x
     *
     */
    @Ignore("Manual acceptance")
    @Test
    public void record_and_upload() throws Exception {
    }
}