package io.brooklyn.entity.web;

import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.IntAttributeRef;
import io.brooklyn.attributes.LongAttributeRef;
import io.brooklyn.attributes.PortAttributeRef;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
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
    public final PortAttributeRef httPort = newPortAttributeRef(TomcatConfig.HTTP_PORT);
    public final PortAttributeRef shutdownPort = newPortAttributeRef(TomcatConfig.SHUTDOWN_PORT);
    public final PortAttributeRef jmxPort = newPortAttributeRef(TomcatConfig.JMX_PORT);

    public final LongAttributeRef usedHeap = newLongAttributeRef(TomcatConfig.USED_HEAP);
    public final LongAttributeRef maxHeap = newLongAttributeRef(TomcatConfig.MAX_HEAP);
    public final BasicAttributeRef<String> version = newBasicAttributeRef(TomcatConfig.VERSION);

    public final JmxConnection jmxConnection = new JmxConnection();

    @Override
    public Class<TomcatSshDriver> getDriverClass() {
        return TomcatSshDriver.class;
    }

    @Override
    public void activate() throws Exception {
        super.activate();

        //the actor will register itself, so that every second it gets a message to update its jmx information
        //if that is available.
        repeatingSelfNotification(new JmxUpdate(), 1000);
    }

    public void receive(Undeployment undeployment) {
        System.out.println(self()+":Undeploy");
        getDriver().undeploy();
    }

    public void receive(Deployment deployment) {
        System.out.println("Deploying:" + deployment.url);
        //todo: would be best to offload the work map the potentially long copy action
        getDriver().deploy(deployment.url);
    }

    public void receive(Start start) {
        System.out.println(self()+":Tomcat:Start");

        location.set(start.location);
        System.out.println("Tomcat:start location set");

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

        System.out.println(self()+":Tomcat:Start completed");
    }

    public void receive(Stop stop) {
        System.out.println(self()+":Tomcat:Stop");
        try {
            state.set(SoftwareProcessStatus.STOPPING);
            getDriver().stop();
            state.set(SoftwareProcessStatus.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            state.set(SoftwareProcessStatus.FAILURE);
        }
        System.out.println(self()+":Tomcat:Stop completed");
    }

    public void receive(TomcatFailure failure) {
        System.out.println("TomcatFailure at: " + self());
    }

    public void receive(JmxUpdate update) {
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

    public static class JmxUpdate implements Serializable {
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
