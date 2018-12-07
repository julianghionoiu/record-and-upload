package tdl.record_upload.events;

public interface ExternalEventListener {
    void onExternalEvent(String eventPayload) throws Exception;
}
