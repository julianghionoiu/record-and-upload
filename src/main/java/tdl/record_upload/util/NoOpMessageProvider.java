package tdl.record_upload.util;

public interface NoOpMessageProvider {
    String messageFor(int tick, String lastReceivedExternalEvent);
}
