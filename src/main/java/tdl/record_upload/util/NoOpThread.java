package tdl.record_upload.util;

import org.slf4j.Logger;
import tdl.record_upload.MonitoredBackgroundTask;

public class NoOpThread extends Thread implements MonitoredBackgroundTask {
    private boolean isRunning;
    private int tick;
    private NoOpMessageProvider noOpMessageProvider;


    public NoOpThread(NoOpMessageProvider noOpMessageProvider) {
        this.noOpMessageProvider = noOpMessageProvider;
        isRunning = true;
        tick = 0;
    }


    @Override
    public void run() {
        while(isRunning) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isActive() {
        return isRunning;
    }

    @Override
    public void displayErrors(Logger log) {
        //No errors
    }

    @Override
    public void displayMetrics(StringBuilder displayBuffer) {
        tick += 1;
        displayBuffer.append(noOpMessageProvider.messageFor(tick));
    }

    @Override
    public void signalStop() {
        isRunning = false;
    }

    @Override
    public void onExternalEvent(String eventPayload) {
        // Do nothing
    }
}