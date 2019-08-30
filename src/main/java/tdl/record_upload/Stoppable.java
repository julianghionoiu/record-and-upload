package tdl.record_upload;

public interface Stoppable {

    boolean isAlive();

    void join() throws InterruptedException;

    void signalStop() throws Exception;
}
