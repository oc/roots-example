package bekkopen.jetty;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.ProtectionDomain;

public class Main {

    private final int port;
    private final String contextPath;
    private final String workPath;
    private final String secret;

    public static void main(String[] args) throws Exception {
        Main m = new Main();

        if (args.length != 1)             m.start();
        else if ("stop".equals(args[0]))  m.stop();
        else if ("start".equals(args[0])) m.start();
        else                              m.usage();
    }

    public Main() {
        try {
            String configFile = System.getProperty("config", "jetty.properties");
            System.getProperties().load(new FileInputStream(configFile));
        } catch (Exception ignored) {}

        port = Integer.parseInt(System.getProperty("jetty.port", "8080"));
        contextPath = System.getProperty("jetty.contextPath", "/");
        workPath = System.getProperty("jetty.workDir", null);
        secret = System.getProperty("jetty.secret", "eb27fb2e61ed603363461b3b4e37e0a0");
    }

    private void start() {
        // Start a Jetty server with some sensible(?) defaults
        try {
            Server srv = new Server();
            srv.setStopAtShutdown(true);

            // Increase thread pool
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setMaxThreads(100);
            srv.setThreadPool(threadPool);

            // Ensure using the non-blocking connector (NIO)
            Connector connector = new SelectChannelConnector();
            connector.setPort(port);
            connector.setMaxIdleTime(30000);
            srv.setConnectors(new Connector[]{connector});

            // Get the war-file
            ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
            String warFile = protectionDomain.getCodeSource().getLocation().toExternalForm();
            String currentDir = new File(protectionDomain.getCodeSource().getLocation().getPath()).getParent();

            // Add the warFile (this jar)
            WebAppContext context = new WebAppContext(warFile, contextPath);
            context.setServer(srv);
            resetTempDirectory(context, currentDir);
            context.setSessionHandler(new SessionHandler());

            // Add the handlers
            HandlerList handlers = new HandlerList();
            handlers.addHandler(context);
            handlers.addHandler(new ShutdownHandler(srv, context, secret));
            srv.setHandler(handlers);

            srv.start();
            srv.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        System.out.println(ShutdownHandler.shutdown(port, secret));
    }


    private void usage() {
        System.out.println("Usage: java -jar <file.jar> [start|stop|status|enable|disable]\n\t" +
                "start    Start the server (default)\n\t" +
                "stop     Stop the server gracefully\n\t"
        );
        System.exit(-1);
    }

    private void resetTempDirectory(WebAppContext context, String currentDir) throws IOException {
        File workDir;
        if (workPath != null) {
            workDir = new File(workPath);
        } else {
            workDir = new File(currentDir, "work");
        }
        FileUtils.deleteDirectory(workDir);
        context.setTempDirectory(workDir);
    }


}
