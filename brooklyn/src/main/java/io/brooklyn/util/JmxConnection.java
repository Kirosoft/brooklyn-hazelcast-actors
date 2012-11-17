package io.brooklyn.util;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.hazelcast.actors.utils.Util.notNull;

/**
 * The big difference between the JmxHelper and this class is that this JmxConnection is not going to retry
 * the connection. This is done to make calls as quickly as possible. So if the machine is not working, you
 * will not get data.
 *
 * But because the Entity can repeatedly call this method, on every call the connection can be checked and one
 * retry could be made to fix the issue.
 */
public class JmxConnection {

    private String url;
    private MBeanServerConnection connection;

    public JmxConnection() {
    }

    public boolean isConnected(){
        getConnection();

        if(connection == null){
            return false;
        }

        try {
            connection.getMBeanCount();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void init(String host, int port) {
        init(String.format("impl:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", notNull(host, "host"), port));
    }

    public void init(String url) {
        this.url = url;
    }

    public Object getAttribute(String objectName, String attribute) {
        MBeanServerConnection c = getConnection();
        if (c == null) {
            return null;
        }

        try {
            ObjectName operatingSystemMXBean = new ObjectName(objectName);
            return c.getAttribute(operatingSystemMXBean, attribute);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public MBeanServerConnection getConnection() {
        if (connection == null) {
            if (url == null) {
                return null;
            }
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
