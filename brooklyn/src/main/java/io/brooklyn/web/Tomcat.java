package io.brooklyn.web;

import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.SoftwareProcessDriver;
import io.brooklyn.SoftwareProcessEntity;
import io.brooklyn.SoftwareProcessEntityStatus;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.util.JmxConnection;

import javax.management.openmbean.CompositeData;
import java.io.Serializable;

/**
 * The current start map tomcat is a blocking operation, meaning: as long as the installation (install/customize/launch)
 * is executing, the actor will not be processing any other messages.
 *
 * This is undesirable, and also a violation what you normally want to do with actors: keep processing messages
 * as short as possible. What should be done is that the driver calls should be offloaded to another thread and
 * a message should be send as soon as the task is complete. Tomcat then should respond to these messages (e.g.
 * InstallComplete) and start to execute the following step, e.g. 'customize'.
 *
 * The problem is that it could be that other operations like Deploy are being send before the Tomcat machine
 * is fully started. This can be solved in different ways;
 * - only send deploy message when tomcat is running
 * - store the deploy messages in tomcat and process them as soon as you receive the 'Running' event from the
 * driver.
 */
public class Tomcat extends SoftwareProcessEntity<TomcatDriver> {

    public static final Attribute<Integer> HTTP_PORT = new Attribute<Integer>("httpPort", 8080);
    public static final Attribute<Integer> SHUTDOWN_PORT = new Attribute<Integer>("shutdownPort", 8005);
    public static final Attribute<ActorRef> CLUSTER = new Attribute<ActorRef>("cluster");
    public static final Attribute<Long> USED_HEAP = new Attribute<Long>("usedHeap");
    public static final Attribute<Long> MAX_HEAP = new Attribute<Long>("maxHeap");
    public static final Attribute<Integer> JMX_PORT = new Attribute<Integer>("jmxPort");
    public static final Attribute<String> VERSION = new Attribute<String>("version", "7.0.32");

    public final BasicAttributeRef<Integer> httPort = newBasicAttributeRef(HTTP_PORT);
    public final BasicAttributeRef<Integer> shutdownPort = newBasicAttributeRef(SHUTDOWN_PORT);
    public final BasicAttributeRef<Integer> jmxPort = newBasicAttributeRef(JMX_PORT);
    public final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(CLUSTER);
    public final BasicAttributeRef<Long> usedHeap = newBasicAttributeRef(USED_HEAP);
    public final BasicAttributeRef<Long> maxHeap = newBasicAttributeRef(MAX_HEAP);
    public final BasicAttributeRef<String> version = newBasicAttributeRef(VERSION);

    public JmxConnection jmxConnection = new JmxConnection();

    @Override
    public Class<? extends SoftwareProcessDriver> getDriverClass() {
        return TomcatDriver.class;
    }

    @Override
    public void init(ActorRecipe actorRecipe)throws Exception {
        super.init(actorRecipe);

        //the actor will register itself, so that every second it gets a message to update is jmx information
        //if that is available.
        getActorRuntime().repeat(self(), new JmxUpdate(), 1000);
    }

    public void receive(UndeployMessage msg) {
        System.out.println("Undeploy");
        getDriver().undeploy();
    }

    public void receive(DeployMessage msg) {
        System.out.println("Deploying:" + msg.url);
        //todo: would be best to offload the work map the potentially long copy action
        getDriver().deploy(msg.url);
    }

    public void receive(StartTomcatMessage msg) {
        System.out.println("StartTomcat");

        cluster.set(msg.cluster);
        location.set(msg.location);

        try {
            state.set(SoftwareProcessEntityStatus.STARTING);
            TomcatDriver driver = getDriver();
            driver.install();
            driver.customize();
            driver.launch();
            state.set(SoftwareProcessEntityStatus.RUNNING);
        } catch (Exception e) {
            e.printStackTrace();
            state.set(SoftwareProcessEntityStatus.FAILURE);
        }
    }

    public void receive(StopTomcatMessage msg) {
        System.out.println("StopTomcat");
        try {
            state.set(SoftwareProcessEntityStatus.STOPPING);
            getDriver().stop();
            state.set(SoftwareProcessEntityStatus.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            state.set(SoftwareProcessEntityStatus.FAILURE);
        }
    }

    public void receive(TomcatFailureMessage msg) {
        System.out.println("TomcatFailure at: " + self());
        ActorRef cluster = this.cluster.get();
        if (cluster != null) {
            getActorRuntime().send(cluster, new WebCluster.ChildFailureMessage(self()));
        }
    }

    public void receive(JmxUpdate msg) {
        CompositeData heapData = (CompositeData) jmxConnection.getAttribute("java.lang:type=Memory", "HeapMemoryUsage");
        if (heapData == null) {
            usedHeap.set(-1L);
            maxHeap.set(-1L);
        } else {
            usedHeap.set((Long) heapData.get("used"));
            maxHeap.set((Long) heapData.get("max"));
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
