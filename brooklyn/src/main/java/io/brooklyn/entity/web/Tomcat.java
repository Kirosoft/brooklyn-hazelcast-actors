package io.brooklyn.entity.web;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.entity.softwareprocess.SoftwareProcessDriver;
import io.brooklyn.entity.softwareprocess.SoftwareProcessStatus;
import io.brooklyn.util.JmxConnection;

import javax.management.openmbean.CompositeData;
import java.io.Serializable;

/**
 * The current start map tomcat is a blocking operation, meaning: as long as the installation (install/customize/launch)
 * is executing, the actor will not be processing any other messages.
 * <p/>
 * This is undesirable, and also a violation what you normally want to do with actors: keep processing messages
 * as short as possible. What should be done is that the driver calls should be offloaded to another thread and
 * a message should be send as soon as the task is complete. Tomcat then should respond to these messages (e.g.
 * InstallComplete) and start to execute the following step, e.g. 'customize'.
 * <p/>
 * The problem is that it could be that other operations like Deploy are being send before the Tomcat machine
 * is fully started. This can be solved in different ways;
 * - only send deploy message when tomcat is running
 * - store the deploy messages in tomcat and process them as soon as you receive the 'Running' event from the
 * driver.
 */
public class Tomcat extends SoftwareProcess<TomcatDriver> {

    //these attribute references are 'handy', but not mandatory. They read and write to a AttributeMap which is just
    //a map (backedup by hazelcast). This map can be accessed directly either using strings or attributes.
    //So these references are here to demonstrate an alternative way of accessing attributes.
    public final BasicAttributeRef<Integer> httPort = newBasicAttributeRef(TomcatConfig.HTTP_PORT);
    public final BasicAttributeRef<Integer> shutdownPort = newBasicAttributeRef(TomcatConfig.SHUTDOWN_PORT);
    public final BasicAttributeRef<Integer> jmxPort = newBasicAttributeRef(TomcatConfig.JMX_PORT);
    public final BasicAttributeRef<ActorRef> cluster = newBasicAttributeRef(TomcatConfig.CLUSTER);
    public final BasicAttributeRef<Long> usedHeap = newBasicAttributeRef(TomcatConfig.USED_HEAP);
    public final BasicAttributeRef<Long> maxHeap = newBasicAttributeRef(TomcatConfig.MAX_HEAP);
    public final BasicAttributeRef<String> version = newBasicAttributeRef(TomcatConfig.VERSION);

    public final JmxConnection jmxConnection = new JmxConnection();

    @Override
    public Class<? extends SoftwareProcessDriver> getDriverClass() {
        return TomcatDriver.class;
    }

    @Override
    public void activate() throws Exception {
        super.activate();

        //the actor will register itself, so that every second it gets a message to update is jmx information
        //if that is available.
        getActorRuntime().repeat(self(), new JmxUpdate(), 1000);
    }

    public void receive(Undeployment msg) {
        System.out.println("Undeploy");
        getDriver().undeploy();
    }

    public void receive(Deployment msg) {
        System.out.println("Deploying:" + msg.url);
        //todo: would be best to offload the work map the potentially long copy action
        getDriver().deploy(msg.url);
    }

    public void receive(StartTomcat msg) {
        System.out.println("StartTomcat");

        cluster.set(msg.cluster);
        location.set(msg.location);

        try {
            state.set(SoftwareProcessStatus.STARTING);
            TomcatDriver driver = getDriver();
            driver.install();
            driver.customize();
            driver.launch();
        } catch (Exception e) {
            e.printStackTrace();
            state.set(SoftwareProcessStatus.FAILURE);
        }
    }

    public void receive(StopTomcat msg) {
        System.out.println("StopTomcat");
        try {
            state.set(SoftwareProcessStatus.STOPPING);
            getDriver().stop();
            state.set(SoftwareProcessStatus.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            state.set(SoftwareProcessStatus.FAILURE);
        }
    }

    public void receive(TomcatFailure msg) {
        System.out.println("TomcatFailure at: " + self());
        ActorRef cluster = this.cluster.get();
        if (cluster != null) {
            getActorRuntime().send(cluster, new WebCluster.ChildFailure(self()));
        }
    }

    public void receive(JmxUpdate msg) {
        if (state.get().equals(SoftwareProcessStatus.UNSTARTED)) {
            return;
        }

        if (!jmxConnection.isConnected()) {
            state.set(SoftwareProcessStatus.UNREACHABLE);
            return;
        }

        state.set(SoftwareProcessStatus.RUNNING);

        CompositeData heapData = (CompositeData) jmxConnection.getAttribute("java.lang:type=Memory", "HeapMemoryUsage");
        if (heapData == null) {
            usedHeap.set(-1L);
            maxHeap.set(-1L);
        } else {
            usedHeap.set((Long) heapData.get("used"));
            maxHeap.set((Long) heapData.get("max"));
        }
    }

    public static class TomcatFailure implements Serializable {
    }

    public static class StopTomcat implements Serializable {
    }

    public static class JmxUpdate implements Serializable {
    }

    public static class StartTomcat extends Start {
        public final ActorRef cluster;

        public StartTomcat(String location) {
            this(location, null);
        }

        public StartTomcat(String location, ActorRef cluster) {
            super(location);
            this.cluster = cluster;
        }
    }

    public static class Undeployment implements Serializable {
    }

    public static class Deployment implements Serializable {
        public final String url;

        public Deployment(String url) {
            this.url = url;
        }
    }
}
