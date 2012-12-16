package io.brooklyn.entity.softwareprocess;

import brooklyn.entity.basic.Lifecycle;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.LongAttribute;
import io.brooklyn.entity.enrichers.RollingTimeWindowMeanEnricher;
import io.brooklyn.util.JmxConnection;
import io.brooklyn.util.TooManyRetriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;

public abstract class JvmProcess<D extends SoftwareProcessDriver> extends SoftwareProcess<D> {

    private static final Logger log = LoggerFactory.getLogger(JvmProcess.class);

    public static final AttributeType<Long> USED_HEAP = new AttributeType<>("usedHeap", 0L);
    public static final AttributeType<Double> AVERAGE_USED_HEAP = new AttributeType<>("averageUsedHeap", 0d);
    public static final AttributeType<Long> MAX_HEAP = new AttributeType<>("maxHeap", 0L);

    public final LongAttribute usedHeap = newLongAttribute(USED_HEAP);
    public final LongAttribute maxHeap = newLongAttribute(MAX_HEAP);
    public final JmxConnection jmxConnection = new JmxConnection();

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
        startEnricher(averageUsedHeapEnricherConfig);
    }

    public final void start() {
        if (log.isDebugEnabled()) log.debug(self() + ":JvmProcess:Start");

        try {
            state.set(Lifecycle.STARTING);
            SoftwareProcessDriver driver = getDriver();
            driver.install();
            driver.customize();
            driver.launch();
            if (log.isDebugEnabled()) log.debug(self() + ":JvmProcess:Start completed");
        } catch (Exception e) {
            e.printStackTrace();
            state.set(Lifecycle.ON_FIRE);
            if (log.isDebugEnabled()) log.debug(self() + ":JvmProcess:Start completed with error");
        }
    }

    public final void stop() {
        if (log.isDebugEnabled()) log.debug(self() + ":JvmProcess:Stop");
        try {
            state.set(Lifecycle.STOPPING);
            getDriver().stop();
            state.set(Lifecycle.STOPPED);
        } catch (Exception e) {
            e.printStackTrace();
            state.set(Lifecycle.ON_FIRE);
        }
        if (log.isDebugEnabled()) log.debug(self() + ":JvmProcess:Stop completed");
    }

    public final void receive(JmxUpdate update) {
        if (log.isDebugEnabled()) log.debug(self() + ":JvmProcess:JmxUpdate");

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
}
