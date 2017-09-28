package tdl.record_upload;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import tdl.record.sourcecode.record.SourceCodeRecorderException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class ExternalEventServerThread implements Stoppable {
    private static final int PORT = 41375;
    private Server server;

    ExternalEventServerThread(SourceCodeRecordingThread sourceCodeRecordingThread) {
        // Create the server
        QueuedThreadPool threadPool = new QueuedThreadPool(4, 1);
        threadPool.setName("ExEvent");
        server = new Server(threadPool);

        // Add the http connector
        ServerConnector http = new ServerConnector(server);
        http.setHost("localhost");
        http.setPort(PORT);
        server.addConnector(http);

        // Register the servlets
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(new ServletHolder(new StatusServlet()),
                "/status");
        handler.addServletWithMapping(new ServletHolder(new NotifyServlet(sourceCodeRecordingThread)),
                "/notify");

    }

    void start() throws Exception {
        server.start();
    }

    @Override
    public void join() throws InterruptedException {
        server.join();
    }

    @Override
    public void signalStop() throws Exception {
        server.stop();
    }

    //~~~~~~~~~ The commands that are being handled

    private class StatusServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("OK");
        }
    }

    private class NotifyServlet extends HttpServlet {
        private SourceCodeRecordingThread sourceCodeRecordingThread;

        NotifyServlet(SourceCodeRecordingThread sourceCodeRecordingThread) {
            this.sourceCodeRecordingThread = sourceCodeRecordingThread;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            try {
                sourceCodeRecordingThread.tagCurrentState(body.trim());
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println("ACK");
            } catch (SourceCodeRecorderException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println(e.getMessage());
            }
        }

    }
}
