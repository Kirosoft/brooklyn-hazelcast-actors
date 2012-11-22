package io.brooklyn.entity.web;

import brooklyn.entity.basic.Lifecycle;
import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.LongAttributeRef;
import io.brooklyn.attributes.PortAttributeRef;
import io.brooklyn.entity.Start;
import io.brooklyn.entity.Stop;
import io.brooklyn.entity.enrichers.RollingTimeWindowMeanEnricher;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.util.JmxConnection;
import io.brooklyn.util.TooManyRetriesException;

import javax.management.openmbean.CompositeData;

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

    public static final Attribute<Long> USED_HEAP = new Attribute<>("usedHeap", 0L);
    public static final Attribute<Double> AVERAGE_USED_HEAP = new Attribute<>("averageUsedHeap", 0d);
    public static final Attribute<Long> MAX_HEAP = new Attribute<>("maxHeap", 0L);

    //these attribute references are 'handy', but not mandatory. They read and write to a AttributeMap which is just
    //a map (backedup by hazelcast). This map can be accessed directly either using strings or attributes.
    //So these references are here to demonstrate an alternative way of accessing attributes.
    public final PortAttributeRef httPort = newPortAttributeRef(TomcatConfig.HTTP_PORT);
    public final PortAttributeRef shutdownPort = newPortAttributeRef(TomcatConfig.SHUTDOWN_PORT);
    public final PortAttributeRef jmxPort = newPortAttributeRef(TomcatConfig.JMX_PORT);

    public final LongAttributeRef usedHeap = newLongAttributeRef(USED_HEAP);
    public final LongAttributeRef maxHeap = newLongAttributeRef(MAX_HEAP);
    public final BasicAttributeRef<String> version = newBasicAttributeRef(TomcatConfig.VERSION);

    public final JmxConnection jmxConnection = new JmxConnection();

    @Override
    public Class<TomcatDriver> getDriverClass() {
        return TomcatDriver.class;
    }

    @Override
    public void activate() throws Exception {
        super.activate();

        //the actor will register itself, so that every second it gets a message to update its jmx information
        //if that is available.
        repeatingSelfNotification(new JmxUpdate(), 1000);
    }

    public void receive(Undeployment undeployment) {
        System.out.println(self() + ":Undeploy");
        getDriver().undeploy();
    }

    public void receive(Deployment deployment) {
        System.out.println("Deploying:" + deployment.url);
        //todo: would be best to offload the work because potentially long copy action
        getDriver().deploy(deployment.url);
    }

    public void receive(Start start) {
        System.out.println(self() + ":Tomcat:Start");

        //TODO: This should be placed in the 'activate'. The problem is that Hazelcast deadlocks in that method.
        //the average used heap is calculated using an enricher.
        RollingTimeWindowMeanEnricher.Config config = new RollingTimeWindowMeanEnricher.Config()
                .targetAttribute(AVERAGE_USED_HEAP)
                .sourceAttribute(USED_HEAP)
                .source(self());
        ActorRef averageUsedHeapEnricher = newEntity(config);
        //TODO: This method can be deleted as soon as hazelcast doesn't deadlock in activate.
        send(averageUsedHeapEnricher,start);

        location.set(start.location);
        System.out.println("Tomcat:start location set");

        try {
            state.set(Lifecycle.STARTING);
            TomcatDriver driver = getDriver();
            driver.install();
            driver.customize();
            driver.launch();
        } catch (Exception e) {
            e.printStackTrace();
            state.set(Lifecycle.ON_FIRE);
        }

        System.out.println(self() + ":Tomcat:Start completed");
    }

    public void receive(Stop stop) {
        System.out.println(self() + ":Tomcat:Stop");
        try {
            state.set(Lifecycle.STOPPING);
            getDriver().stop();
            state.set(Lifecycle.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            state.set(Lifecycle.ON_FIRE);
        }
        System.out.println(self() + ":Tomcat:Stop completed");
    }

    public void receive(JmxUpdate update) {
        //System.out.println(self() + ":Tomcat:JmxUpdate");

        Lifecycle lifecycle = state.get();
        if (!(Lifecycle.STARTING.equals(lifecycle) || Lifecycle.RUNNING.equals(lifecycle))) {
            return;
        }

        try {
            if (!jmxConnection.connect()) {
                return;
            }
        } catch (TooManyRetriesException e) {
            state.set(Lifecycle.ON_FIRE);
            return;
        }

        state.set(Lifecycle.RUNNING);

        CompositeData heapData = (CompositeData) jmxConnection.getAttribute("java.lang:type=Memory", "HeapMemoryUsage");
        if (heapData == null) {
            usedHeap.set(-1L);
            maxHeap.set(-1L);
        } else {
            usedHeap.set((Long) heapData.get("used"));
            maxHeap.set((Long) heapData.get("max"));
        }
    }


    public static class JmxUpdate extends AbstractMessage {}

    public static class Undeployment extends AbstractMessage {}

    public static class Deployment extends AbstractMessage {
        public final String url;

        public Deployment(String url) {
            this.url = url;
        }
    }
}
