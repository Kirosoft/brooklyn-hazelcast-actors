package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
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

    public static final Attribute<Integer> HTTP_PORT = new Attribute<>("httpPort", 8080);
    public static final Attribute<Integer> SHUTDOWN_PORT = new Attribute<>("shutdownPort", 8005);
    public static final Attribute<Integer> JMX_PORT = new Attribute<>("jmxPort", 10000);
    public static final Attribute<String> VERSION = new Attribute<>("version", "7.0.32");
    public static final Attribute<ActorRef> CLUSTER = new Attribute<>("cluster");
    public static final Attribute<Long> USED_HEAP = new Attribute<>("usedHeap", 0L);
    public static final Attribute<Long> MAX_HEAP = new Attribute<>("maxHeap", 0L);

    public TomcatConfig() {
        super(Tomcat.class);
    }

    public TomcatConfig httpPort(int httpPort) {
        addProperty(HTTP_PORT, httpPort);
        return this;
    }

    public TomcatConfig shutdownPort(int shutdownPort) {
        addProperty(SHUTDOWN_PORT, shutdownPort);
        return this;
    }

    public TomcatConfig jmxPort(int jmxPort) {
        addProperty(JMX_PORT, jmxPort);
        return this;
    }

    public TomcatConfig version(String version) {
        addProperty(VERSION, version);
        return this;
    }

    public TomcatConfig cluster(ActorRef cluster) {
        addProperty(CLUSTER, cluster);
        return this;
    }
}
