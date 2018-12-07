package tdl.record_upload.sourcecode;

import tdl.record_upload.util.NoOpThread;

public class NoOpSourceCodeThread extends NoOpThread {

    public NoOpSourceCodeThread() {
        super(tick -> String.format("source capture no. %2d", tick));
    }


}
