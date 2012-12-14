package io.brooklyn.entity.web;

import brooklyn.entity.basic.Lifecycle;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.LongAttribute;
import io.brooklyn.attributes.PortAttribute;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.entity.Stop;
import io.brooklyn.entity.enrichers.RollingTimeWindowMeanEnricher;
import io.brooklyn.entity.softwareprocess.SoftwareProcess;
import io.brooklyn.util.JmxConnection;
import io.brooklyn.util.TooManyRetriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(Tomcat.class);


    public static final AttributeType<Long> USED_HEAP = new AttributeType<>("usedHeap", 0L);
    public static final AttributeType<Double> AVERAGE_USED_HEAP = new AttributeType<>("averageUsedHeap", 0d);
    public static final AttributeType<Long> MAX_HEAP = new AttributeType<>("maxHeap", 0L);

    //these attribute references are 'handy', but not mandatory. They read and write to a AttributeMap which is just
    //a map (backed up by hazelcast). This map can be accessed directly either using strings or attributes.
    //So these references are here to demonstrate an alternative way of accessing attributes.
    public final PortAttribute httPort = newPortAttributeRef(TomcatConfig.HTTP_PORT);
    public final PortAttribute shutdownPort = newPortAttributeRef(TomcatConfig.SHUTDOWN_PORT);
    public final PortAttribute jmxPort = newPortAttributeRef(TomcatConfig.JMX_PORT);

    public final LongAttribute usedHeap = newLongAttribute(USED_HEAP);
    public final LongAttribute maxHeap = newLongAttribute(MAX_HEAP);
    public final ReferenceAttribute<String> version = newReferenceAttribute(TomcatConfig.VERSION);

    public final JmxConnection jmxConnection = new JmxConnection();

    @Override
    public Class<TomcatDriver> getDriverClass() {
        return TomcatDriver.class;
    }

    @Override
    public void onActivation() throws Exception {
        super.onActivation();

        //the actor will start itself, so that every second it gets a message to update its jmx information
        //if that is available.
        notifySelf(new JmxUpdate(), 1000);

        RollingTimeWindowMeanEnricher.Config averageUsedHeapEnricherConfig = new RollingTimeWindowMeanEnricher.Config()
                .targetAttribute(AVERAGE_USED_HEAP)
                .sourceAttribute(USED_HEAP)
                .source(self());
        //the created enricher (which is also an entity) is linked to this process. So if tomcat exits, also the
        //enricher is going to exit. Also the enricher will be created in the same partition as tomcat; we want them
        //to be as close as possible to prevent remoting.
        spawnAndLink(averageUsedHeapEnricherConfig);
    }

    public void receive(Undeployment undeployment) {
        if (log.isDebugEnabled()) log.debug(self() + ":Undeploy");

        getDriver().undeploy();
    }

    public void receive(Deployment deployment) {
        if (log.isDebugEnabled()) log.debug("Deploying:" + deployment.url);
        //todo: would be best to offload the run because potentially long copy action
        getDriver().deploy(deployment.url);
    }

    public void receive(Start start) {
        if (log.isDebugEnabled()) log.debug(self() + ":Tomcat:Start");

        super.receive(start);

        location.set(start.location);

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

        if (log.isDebugEnabled()) log.debug(self() + ":Tomcat:Start completed");
    }

    public void receive(Stop stop) {
        if (log.isDebugEnabled()) log.debug(self() + ":Tomcat:Stop");
        try {
            state.set(Lifecycle.STOPPING);
            getDriver().stop();
            state.set(Lifecycle.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            state.set(Lifecycle.ON_FIRE);
        }
        if (log.isDebugEnabled()) log.debug(self() + ":Tomcat:Stop completed");
    }

    public void receive(JmxUpdate update) {
        if (log.isDebugEnabled()) log.debug(self() + ":Tomcat:JmxUpdate");

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

    public static class JmxUpdate extends AbstractMessage {
    }

    public static class Undeployment extends AbstractMessage {
    }

    public static class Deployment extends AbstractMessage {
        public final String url;

        public Deployment(String url) {
            this.url = url;
        }
    }
}
