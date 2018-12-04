package tdl.record_upload.sourcecode;

import tdl.record_upload.util.DummyThread;

public class NoSourceCodeDummyThread extends DummyThread {

    public NoSourceCodeDummyThread() {
        super(tick -> String.format("source capture no. %2d", tick));
    }


}
