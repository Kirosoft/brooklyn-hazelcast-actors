package io.brooklyn.util;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JmxConnection {

    private final String url;
    private volatile MBeanServerConnection connection;

    public JmxConnection(String host, int port) {
        url = String.format("service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", host, port);
    }

    public Object read(String property, Object defaultValue){
        //getConnection().getAttribute()
        return null;
    }

    public MBeanServerConnection getConnection() {
        if (connection == null) {

            try {
                JMXServiceURL serviceUrl = new JMXServiceURL(url);
                Map env = new HashMap();

                JMXConnector connector = JMXConnectorFactory.connect(serviceUrl, env);
                connection = connector.getMBeanServerConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
