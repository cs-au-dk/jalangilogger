package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Servlet-based server for receiving information from value logger clients.
 */
public class JettyLoggerServer {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JettyLoggerServer.class);

    private Path instrumentedApplication;

    public JettyLoggerServer(Path instrumentedApplication) {
        assert Files.exists(instrumentedApplication);
        this.instrumentedApplication = instrumentedApplication;
    }

    public RunningServer startServer() {
        int port = 0;
        Server server = new Server(port);
        RunningServer runningServer = new RunningServer(server);
        server.setStopAtShutdown(true);
        server.setStopTimeout(0);
        WebAppContext handler = new WebAppContext();
        handler.setMaxFormContentSize(50 /* mega */ * 1024  /* kilo */ * 1024 /* bytes */);
        handler.setBaseResource(Resource.newResource(instrumentedApplication.toFile()));
        handler.setContextPath("/");

        handler.addServlet(new ServletHolder(new SendEntriesServlet(runningServer)), "/logger-server-api/sendEntries");
        handler.addServlet(new ServletHolder(new DoneServlet(runningServer)), "/logger-server-api/done");

        server.setHandler(handler);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        URI uri = server.getURI();
        log.debug(String.format("Started logger-server at %s, serving %s", uri, instrumentedApplication.toAbsolutePath().toString()));

        return runningServer;
    }

    public static class RunningServer {

        private final Server server;

        private final Gson gson = new Gson();

        private Set<JsonElement> entries;

        private boolean stopped = false;

        public RunningServer(Server server) {
            this.server = server;
            this.entries = new HashSet<>();
        }

        public boolean isStopped() {
            return stopped;
        }

        public URI getURI() {
            return server.getURI();
        }

        public void stop() {
            try {
                server.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                this.stopped = true;
                log.debug("Logger-server has stopped");
            }
        }

        public void persistEntries(Path file) {
            Iterable<String> lines = entries.stream().map(e -> gson.toJson(e)).distinct()::iterator;
            try {
                Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void registerRawEntries(String rawEntries) {
            gson.fromJson(rawEntries, JsonArray.class)
                    .forEach(entry -> entries.add(entry));
        }
    }

    private class SendEntriesServlet extends DefaultServlet {

        private final RunningServer runningServer;

        public SendEntriesServlet(RunningServer runningServer) {
            this.runningServer = runningServer;
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setStatus(204); // no content in response
            String rawEntries = request.getParameter("entries");
            runningServer.registerRawEntries(rawEntries);
        }
    }

    private class DoneServlet extends DefaultServlet {

        private final RunningServer runningServer;

        public DoneServlet(RunningServer runningServer) {
            this.runningServer = runningServer;
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            log.debug("Stopping logger-server (post-request)");
            new Thread(() -> { // must stop in other thread
                runningServer.stop();
            }).start();
        }
    }
}
