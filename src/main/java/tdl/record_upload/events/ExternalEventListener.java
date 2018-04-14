package tdl.record_upload.events;

public interface ExternalEventListener {
    void process(String eventPayload) throws Exception;
}
