package io.brooklyn.entity.web;

import brooklyn.entity.basic.Lifecycle;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.LongAttribute;
import io.brooklyn.attributes.PortAttribute;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.entity.Stop;
import io.brooklyn.entity.enrichers.RollingTimeWindowMeanEnricher;
import io.brooklyn.entity.softwareprocess.JvmProcess;
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
public class Tomcat extends JvmProcess<TomcatDriver> {

    private static final Logger log = LoggerFactory.getLogger(Tomcat.class);

    //these attribute references are 'handy', but not mandatory. They read and write to a AttributeMap which is just
    //a map (backed up by hazelcast). This map can be accessed directly either using strings or attributes.
    //So these references are here to demonstrate an alternative way of accessing attributes.
    public final PortAttribute httPort = newPortAttributeRef(TomcatConfig.HTTP_PORT);
    public final PortAttribute shutdownPort = newPortAttributeRef(TomcatConfig.SHUTDOWN_PORT);
    public final PortAttribute jmxPort = newPortAttributeRef(TomcatConfig.JMX_PORT);

    public final ReferenceAttribute<String> version = newReferenceAttribute(TomcatConfig.VERSION);

    @Override
    public Class<TomcatDriver> getDriverClass() {
        return TomcatDriver.class;
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

    public static class Undeployment extends AbstractMessage {
    }

    public static class Deployment extends AbstractMessage {
        public final String url;

        public Deployment(String url) {
            this.url = url;
        }
    }
}
