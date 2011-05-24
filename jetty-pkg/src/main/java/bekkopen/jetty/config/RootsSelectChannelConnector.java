package bekkopen.jetty.config;

public class RootsSelectChannelConnector extends org.eclipse.jetty.server.nio.SelectChannelConnector {

	public RootsSelectChannelConnector(final int jettyPort) {
		super();
		setPort(jettyPort);
	}

}
