package tdl.record_upload.sourcecode;

import tdl.record_upload.util.NoOpThread;

public class NoOpSourceCodeThread extends NoOpThread {

    public NoOpSourceCodeThread() {
        super((tick, event) -> String.format("frame no. %2d, source code recording disabled (%s)", tick, event));
    }


}
