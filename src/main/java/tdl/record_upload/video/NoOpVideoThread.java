package tdl.record_upload.video;

import tdl.record_upload.util.NoOpThread;

public class NoOpVideoThread extends NoOpThread {

    public NoOpVideoThread() {
        super((tick, event) -> String.format("tick %3d, video recoding disabled", tick));
    }

}
