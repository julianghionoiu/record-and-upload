package tdl.record_upload.video;

import tdl.record_upload.util.DummyThread;

public class NoVideoDummyThread  extends DummyThread {

    public NoVideoDummyThread() {
        super(tick -> String.format("tick %3d, video recoding disabled", tick));
    }

}
