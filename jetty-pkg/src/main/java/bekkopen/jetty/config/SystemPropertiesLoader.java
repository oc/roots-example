package bekkopen.jetty.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

public final class SystemPropertiesLoader {
	private static final Set<String> APP_SERVERS = CollectionBuilder.newSet("node1", "node2");

	private static final String CONFIG = "config";

	private static LocalHostResolver localHostResolver = new LocalHostResolver();

	private SystemPropertiesLoader() {
	}

	public static void loadConfig() throws IOException {
		loadProperties(CONFIG);

	}

	private static void loadProperties(final String configKey) throws IOException {
		final String configValue = System.getProperty(configKey);
		if (configValue == null) {
			throw new RuntimeException("System property \""
					+ configKey + "\" er ikke satt");
		}
		File configFile = new File(configValue);
		Properties properties = new Properties();
		if (configFile.exists()) {
			FileInputStream configStream = new FileInputStream(configFile);
			try {
				properties.load(configStream);
			} finally {
				configStream.close();
			}

			String hostName = localHostResolver.getLocalHost();
			if (APP_SERVERS.contains(hostName)) {
				String propertyHostName = properties.getProperty("hostname");
				if (!hostName.equals(propertyHostName)) {
					String message = "System property hostname stemmer ikke overens med det virkelige hostnamet. "
							+ "Properties-fila angir " + propertyHostName + ", mens det virkelige er " + hostName + ".";
					throw new RuntimeException(message);
				}
			}

			setProperties(properties);
			if (CONFIG.equals(configKey)) {
				System.out.println("System properties fra " + configFile.getAbsolutePath() +": "+ properties.toString());
			}
		} else {
			throw new RuntimeException("Fant ikke "
					+ configFile.getAbsolutePath());
		}
	}

	private static void setProperties(final Properties properties) {
		Enumeration<Object> propEnum = properties.keys();
		while (propEnum.hasMoreElements()) {
			String property = (String) propEnum.nextElement();
			System.setProperty(property, properties.getProperty(property));
		}
	}

	static void setLocalHostResolver(final LocalHostResolver localHostResolver) {
		SystemPropertiesLoader.localHostResolver = localHostResolver;
	}

	static class LocalHostResolver {

		String getLocalHost() throws UnknownHostException {
			return InetAddress.getLocalHost().getHostName();
		}
	}

}
