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
 * Server for navigating instrumented html pages
 */
public class JettyWebServer {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JettyLoggerServer.class);

    private Path instrumentedApplication;

    public JettyWebServer(Path instrumentedApplication) {
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
        handler.setMaxFormContentSize(100 /* mega */ * 1024  /* kilo */ * 1024 /* bytes */);
        handler.setBaseResource(Resource.newResource(instrumentedApplication.toFile()));
        handler.setContextPath("/");
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

        private boolean stopped = false;

        public RunningServer(Server server) {
            this.server = server;
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
    }
}
