package tdl.record_upload;

import org.slf4j.Logger;

public interface MonitoredSubject {

    boolean isActive();

    void displayErrors(Logger log);
    void displayMetrics(StringBuilder displayBuffer);
}
