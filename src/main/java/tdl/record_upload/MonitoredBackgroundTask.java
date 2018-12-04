package tdl.record_upload;

import tdl.record_upload.events.ExternalEventListener;

public interface MonitoredBackgroundTask extends Stoppable, MonitoredSubject, ExternalEventListener {

    void start();
}
