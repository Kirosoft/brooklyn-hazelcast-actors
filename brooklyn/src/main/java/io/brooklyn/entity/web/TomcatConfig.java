package io.brooklyn.entity.web;

import brooklyn.location.PortRange;
import brooklyn.location.basic.PortRanges;
import io.brooklyn.attributes.Attribute;
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

    public static final Attribute<PortRange> HTTP_PORT = new Attribute<>("httpPort", PortRanges.fromString("8800+"));
    public static final Attribute<PortRange> SHUTDOWN_PORT = new Attribute<>("shutdownPort", PortRanges.fromString("9000+"));
    public static final Attribute<PortRange> JMX_PORT = new Attribute<>("jmxPort", PortRanges.fromString("10000+"));
    public static final Attribute<String> VERSION = new Attribute<>("version", "7.0.32");

    public static final Attribute<Long> USED_HEAP = new Attribute<>("usedHeap", 0L);
    public static final Attribute<Long> MAX_HEAP = new Attribute<>("maxHeap", 0L);

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
