package io.brooklyn.entity.enrichers;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.SensorEvent;
import io.brooklyn.entity.EntityConfig;
import io.brooklyn.entity.Start;

import java.util.Iterator;
import java.util.LinkedList;

public class RollingTimeWindowMeanEnricher extends Enricher {

    public static final Attribute<Attribute<Double>> TARGET_ATTRIBUTE = new Attribute<>("targetAttribute");
    public static final Attribute<Attribute<Number>> SOURCE_ATTRIBUTE = new Attribute<>("sourceAttribute");
    public static final Attribute<ActorRef> SOURCE = new Attribute<>("source");

    public final BasicAttributeRef<Attribute<Double>> targetAttribute = newBasicAttributeRef(TARGET_ATTRIBUTE);
    public final BasicAttributeRef<Attribute<Number>> sourceAttribute = newBasicAttributeRef(SOURCE_ATTRIBUTE);
    public final BasicAttributeRef<ActorRef> source = newBasicAttributeRef(SOURCE);

    public static class ConfidenceQualifiedNumber {
        final Double value;
        final double confidence;

        public ConfidenceQualifiedNumber(Double value, double confidence) {
            this.value = value;
            this.confidence = confidence;
        }
    }

    //todo: need to be converted to attribute/attribute-refs. Else the state will be lost on failover.
    private final LinkedList<Double> values = new LinkedList<>();
    private final LinkedList<Long> timestamps = new LinkedList<>();
    volatile ConfidenceQualifiedNumber lastAverage = new ConfidenceQualifiedNumber(0d, 0d);
    long timePeriod = 10 * 1000;

    @Override
    public void onActivation() throws Exception {
        super.onActivation();


    }

    //As soon as hazelcast doesn't deadlock on the activate method. The following content can be moved to that method
    //and the receive(Start) can be deleted.
    public void receive(Start start) {
        subscribeToAttribute(self(), source.get(), sourceAttribute.get());
    }

    public void receive(SensorEvent event) {
        //System.out.println(self()+"RollingTimeWindowMeanEnricher:"+event);


        if (event.getNewValue() == null) {
            return;
        }

        double d = ((Number) event.getNewValue()).doubleValue();
        values.addLast(d);
        timestamps.addLast(event.getTimestamp());
        pruneValues(event.getTimestamp());
        Double average = getAverage(event.getTimestamp()).value;
        send(source, new AttributePublication<>(targetAttribute, average));
    }

    public ConfidenceQualifiedNumber getAverage( long now) {
        pruneValues(now);
        if (timestamps.isEmpty()) {
            return lastAverage = new ConfidenceQualifiedNumber(lastAverage.value, 0.0d);
        }

        long lastTimestamp = timestamps.get(timestamps.size() - 1);
        Double confidence = ((double) (timePeriod - (now - lastTimestamp))) / timePeriod;
        if (confidence <= 0.0d) {
            double lastValue = values.get(values.size() - 1).doubleValue();
            return lastAverage = new ConfidenceQualifiedNumber(lastValue, 0.0d);
        }

        long start = (now - timePeriod);
        long end;
        double weightedAverage = 0.0d;

        Iterator<Double> valuesIter = values.iterator();
        Iterator<Long> timestampsIter = timestamps.iterator();
        while (valuesIter.hasNext()) {
            // Ignores out-of-date values (and also values that are received out-of-order, but that shouldn't happen!)
            double val = valuesIter.next().doubleValue();
            long timestamp = timestampsIter.next();
            if (timestamp >= start) {
                end = timestamp;
                weightedAverage += ((end - start) / (confidence * timePeriod)) * val;
                start = timestamp;
            }
        }

        return lastAverage = new ConfidenceQualifiedNumber(weightedAverage, confidence);
    }

    /**
     * Discards out-of-date values, but keeps at least one value.
     */
    private void pruneValues(long now) {
        while (timestamps.size() > 1 && timestamps.get(0) < (now - timePeriod)) {
            timestamps.removeFirst();
            values.removeFirst();
        }
    }

    public static class Config extends EntityConfig {
        public Config() {
            super(RollingTimeWindowMeanEnricher.class);
        }

        public Config targetAttribute(Attribute<Double> attribute) {
            addProperty(TARGET_ATTRIBUTE, attribute);
            return this;
        }

        public Config sourceAttribute(Attribute<? extends Number> attribute) {
            addProperty(SOURCE_ATTRIBUTE, attribute);
            return this;
        }

        public Config source(ActorRef source) {
            addProperty(SOURCE, source);
            return this;
        }
    }
}
