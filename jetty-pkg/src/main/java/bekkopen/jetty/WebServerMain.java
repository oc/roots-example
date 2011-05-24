package bekkopen.jetty;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.RolloverFileOutputStream;

import bekkopen.jetty.config.RootsQueuedThreadPool;
import bekkopen.jetty.config.RootsSelectChannelConnector;
import bekkopen.jetty.config.RootsWebAppContext;
import bekkopen.jetty.config.SystemPropertiesLoader;

public class WebServerMain {

	private static final String SESSION_PATH = "/";

	// Graceful shutdown timeout
	private static final int GRACEFUL_SHUTDOWN = 1000;
	// org.eclipse.jetty.server.Request.maxFormContentSize
	private static final String MAX_FORM_CONTENT_SIZE = "300000";
	// Jetty server
	private static Server jettyServer;
	private static int port;

	public static void main(final String[] args) throws Exception {
		start();
	}

	private static void start() {
		konfigurerLogging();
		if (jettyServer != null && jettyServer.isRunning()) {
			System.out.println("JettyServer.start() ble kalt, men serveren er allerede startet.");
			return;
		}
		configure();
		try {
			jettyServer.start();
		} catch (final Exception e) {
			throw new RuntimeException("Kan ikke starte", e);
		}
		port = jettyServer.getConnectors()[0].getLocalPort();
		System.out.println(
				"JettyServer startet paa http://" + System.getProperty("hostname", "localhost") + ":" + port
						+ SESSION_PATH);
	}

	public static void stop() {
		if (jettyServer != null) {
			try {
				jettyServer.stop();
				System.out.println(
						"JettyServer stoppet paa http://" + System.getProperty("hostname", "localhost") + ":" + port
								+ SESSION_PATH);
			} catch (final Exception e) {
				System.err.println("Klarte ikke stoppe Jetty server.");
				throw new RuntimeException("Klarte ikke stoppe", e);
			}
		}
	}

	private static void configure() {
		try {
			SystemPropertiesLoader.loadConfig();
		} catch (final IOException e) {
			throw new RuntimeException("Kan ikke starte", e);
		}

		System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize", MAX_FORM_CONTENT_SIZE);

		jettyServer = new Server();

		jettyServer.setSendServerVersion(false);

		jettyServer.setThreadPool(new RootsQueuedThreadPool());

		final String jettyPort = System.getProperty("jetty.port");
		if (jettyPort != null) {
			jettyServer.setConnectors(new Connector[] { new RootsSelectChannelConnector(Integer.parseInt(jettyPort)) });
		} else {
			throw new RuntimeException("Kan ikke starte: Systemvariabelen 'jetty.port' er ikke satt.");
		}

		List<Handler> handlerList = new ArrayList<Handler>();

		handlerList.add(new RootsWebAppContext(findPathToWarFile(new File(System.getProperty("basedir",
				"target/appassembler/repo"))), SESSION_PATH));

		final HandlerCollection handlers = new HandlerCollection();
		handlers.setHandlers(handlerList.toArray(new Handler[handlerList.size()]));

		jettyServer.setHandler(handlers);
		jettyServer.setGracefulShutdown(GRACEFUL_SHUTDOWN);
		jettyServer.setStopAtShutdown(true);
	}

	private static void konfigurerLogging() {
		try {
			File logDir = new File(System.getProperty("jetty.home", "../logs"));
			logDir.mkdir();
			final String logName = logDir.getAbsolutePath() + "/stderrout.yyyy_mm_dd.log";
			RolloverFileOutputStream logFile = new RolloverFileOutputStream(logName, false, 90,
					TimeZone.getTimeZone("GMT+1"));
			final PrintStream serverLog = new PrintStream(logFile);
			System.setOut(serverLog);
			System.setErr(serverLog);
		} catch (final Exception e) {
			throw new RuntimeException("Kan ikke starte: Kunne ikke konfigurere logging: " + e);
		}

	}

	private static String findPathToWarFile(final File repoDir) {
		if (repoDir.canRead()) {
			final Collection<File> files = FileUtils.listFiles(repoDir, new String[] { "war" }, true);
			if (1 == files.size()) {
				for (final File warFile : files) {
					return warFile.getAbsolutePath();
				}
			} else {
				final StringBuilder melding = new StringBuilder("Forventet 1 webapplikasjon (.war) i: ");
				melding.append(repoDir.getAbsolutePath());
				melding.append(". Fant " + files.size());
				throw new RuntimeException("Kan ikke starte: " + melding.toString());
			}
		}
		System.err.println("Kan ikke lese: " + repoDir.getAbsolutePath() + ". Kan ikke starte.");
		throw new RuntimeException("Kan ikke lese: " + repoDir.getAbsolutePath() + ". Kan ikke starte.");
	}

	private WebServerMain() {
	}

	public static Server getJettyServer() {
		return jettyServer;
	}

}
