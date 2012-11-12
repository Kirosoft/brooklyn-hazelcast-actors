package io.brooklyn.web;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.SoftwareProcessDriver;
import io.brooklyn.SoftwareProcessEntity;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.util.JmxConnection;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.io.Serializable;

public class Tomcat extends SoftwareProcessEntity<TomcatDriver> {

    public static final Attribute<Integer> HTTP_PORT = new Attribute<Integer>("httpPort", 8080);
    public static final Attribute<Integer> SHUTDOWN_PORT = new Attribute<Integer>("shutdownPort", 8005);
    public static final Attribute<String> STATE = new Attribute<String>("state", "stopped");
    public static final Attribute<ActorRef> CLUSTER = new Attribute<ActorRef>("cluster");
    public static final Attribute<Long> USED_HEAP = new Attribute<Long>("usedHeap");
    public static final Attribute<Long> MAX_HEAP = new Attribute<Long>("maxHeap");
    public static final Attribute<Integer> JMX_PORT = new Attribute<Integer>("jmxPort");

    protected final BasicAttributeRef<Integer> httPort = newBasicAttributeRef(HTTP_PORT);
    protected final BasicAttributeRef<Integer> shutdownPort = newBasicAttributeRef(SHUTDOWN_PORT);
    protected final BasicAttributeRef<Integer> jmxPort = newBasicAttributeRef(JMX_PORT);
    protected final BasicAttributeRef<String> state = newBasicAttributeRef(STATE);
    protected final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(CLUSTER);
    protected final BasicAttributeRef<Long> usedHeap = newBasicAttributeRef(USED_HEAP);
    protected final BasicAttributeRef<Long> maxHeap = newBasicAttributeRef(MAX_HEAP);

    private JmxConnection jmxConnection;

    @Override
    public Class<? extends SoftwareProcessDriver> getDriverClass() {
        return TomcatDriver.class;
    }

    @Override
    public void init(ActorRecipe actorRecipe) {
        super.init(actorRecipe);

        getActorRuntime().repeat(self(), new JmxUpdate(), 1000);
    }

    public void receive(UndeployMessage msg, ActorRef sender) {
        System.out.println("Undeploy");
        getDriver().undeploy();
    }

    public void receive(DeployMessage msg, ActorRef sender) {
        System.out.println("Deploying:" + msg.url);
        //todo: would be best to offload the work of the potentially long copy action
        getDriver().deploy(msg.url);
    }

    public void receive(StartTomcatMessage msg, ActorRef sender) {
        System.out.println("StartTomcat");

        //cluster.set(msg.cluster);
        //locationAttribute.set(msg.location);

        try {
            state.set("Starting");
            TomcatDriver driver = getDriver();
            driver.install();
            driver.customize();
            driver.launch();
            jmxConnection = driver.getJmxConnection();
            state.set("Started");
        } catch (Exception e) {
            e.printStackTrace();
            state.set("On fire");
        }
    }

    public void receive(StopTomcatMessage msg, ActorRef sender) {
        System.out.println("StopTomcat");
        try {
            state.set("Stopping");
            getDriver().stop();
            state.set("Stopped");
        } catch (Exception e) {
            e.printStackTrace();
            state.set("On fire");
        }
    }

    public void receive(TomcatFailureMessage msg, ActorRef sender) {
        System.out.println("TomcatFailure at: " + self());
        ActorRef cluster = this.cluster.get();
        if (cluster != null) {
            getActorRuntime().send(cluster, new WebCluster.ChildFailureMessage(self()));
        }
    }

    public void receive(JmxUpdate msg, ActorRef sender) {
        if (jmxConnection == null) {
            usedHeap.set(-1L);
            maxHeap.set(-1L);
        } else {
            MBeanServerConnection c = jmxConnection.getConnection();
            try {
                ObjectName operatingSystemMXBean = new ObjectName("java.lang:type=Memory");
                CompositeData heapMemoryUsage = (CompositeData) c.getAttribute(operatingSystemMXBean, "HeapMemoryUsage");
                long max = (Long) heapMemoryUsage.get("max");
                long used = (Long) heapMemoryUsage.get("used");
                usedHeap.set(used);
                maxHeap.set(max);
                //System.out.println(self() + " max:" + max + " used:" + used);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //jmxConnection
        }
    }

    public static class TomcatFailureMessage implements Serializable {
    }

    public static class StopTomcatMessage implements Serializable {
    }

    public static class JmxUpdate implements Serializable {
    }

    public static class StartTomcatMessage extends StartMessage {
        public final ActorRef cluster;

        public StartTomcatMessage(String location) {
            this(location, null);
        }

        public StartTomcatMessage(String location, ActorRef cluster) {
            super(location);
            this.cluster = cluster;
        }
    }

    public static class UndeployMessage implements Serializable {
    }

    public static class DeployMessage implements Serializable {
        public final String url;

        public DeployMessage(String url) {
            this.url = url;
        }
    }
}
