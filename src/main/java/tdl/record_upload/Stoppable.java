package tdl.record_upload;

public interface Stoppable {

    void join() throws InterruptedException;

    void signalStop() throws Exception;
}
