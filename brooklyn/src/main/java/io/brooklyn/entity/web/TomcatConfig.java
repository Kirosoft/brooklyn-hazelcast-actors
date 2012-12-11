package io.brooklyn.entity.web;

import brooklyn.location.PortRange;
import brooklyn.location.basic.PortRanges;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.entity.softwareprocess.SoftwareProcessConfig;

/**
 * The EntityConfig for the Tomcat entity.
 * <p/>
 * This EntityConfig shows how specific 'setter' methods for attributes can be added. This is not mandatory, just
 * a demonstration that it can be done and how the api would feel.
 *
 * @author Peter Veentjer.
 */
public class TomcatConfig extends SoftwareProcessConfig<Tomcat> {

    public static final AttributeType<PortRange> HTTP_PORT = new AttributeType<>("httpPort", PortRanges.fromString("8800+"));
    public static final AttributeType<PortRange> SHUTDOWN_PORT = new AttributeType<>("shutdownPort", PortRanges.fromString("9000+"));
    public static final AttributeType<PortRange> JMX_PORT = new AttributeType<>("jmxPort", PortRanges.fromString("10000+"));
    public static final AttributeType<String> VERSION = new AttributeType<>("version", "7.0.32");

    public TomcatConfig() {
        super(Tomcat.class);
    }

    public TomcatConfig httpPort(String httpPort) {
        addProperty(HTTP_PORT, PortRanges.fromString(httpPort));
        return this;
    }

    public TomcatConfig shutdownPort(String shutdownPort) {
        addProperty(SHUTDOWN_PORT, PortRanges.fromString(shutdownPort));
        return this;
    }

    public TomcatConfig jmxPort(String jmxPort) {
        addProperty(JMX_PORT, PortRanges.fromString(jmxPort));
        return this;
    }

    public TomcatConfig version(String version) {
        addProperty(VERSION, version);
        return this;
    }

}
