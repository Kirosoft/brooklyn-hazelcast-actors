package io.brooklyn.entity.enrichers;

import com.hazelcast.actors.api.ActorRef;
import io.brooklyn.attributes.*;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.attributes.ListAttribute;
import io.brooklyn.entity.EntityConfig;

import java.io.Serializable;
import java.util.Iterator;

public class RollingTimeWindowMeanEnricher extends Enricher {

    public static final AttributeType<AttributeType<Double>> TARGET_ATTRIBUTE_TYPE = new AttributeType<>("targetAttribute");
    public static final AttributeType<AttributeType<Number>> SOURCE_ATTRIBUTE_TYPE = new AttributeType<>("sourceAttribute");
    public static final AttributeType<ActorRef> SOURCE = new AttributeType<>("source");

    public final ReferenceAttribute<AttributeType<Double>> targetAttribute = newReferenceAttribute(TARGET_ATTRIBUTE_TYPE);
    public final ReferenceAttribute<AttributeType<Number>> sourceAttribute = newReferenceAttribute(SOURCE_ATTRIBUTE_TYPE);
    public final ReferenceAttribute<ActorRef> source = newReferenceAttribute(SOURCE);

    public static class ConfidenceQualifiedNumber implements Serializable {
        final Double value;
        final double confidence;

        public ConfidenceQualifiedNumber(Double value, double confidence) {
            this.value = value;
            this.confidence = confidence;
        }
    }

    private final ListAttribute<Double> values = newListAttribute("values", Double.class);
    private final ListAttribute<Long> timestamps = newListAttribute("timestamps", Long.class);
    private final ReferenceAttribute<ConfidenceQualifiedNumber> lastAverage = newReferenceAttribute("lastAverage", new ConfidenceQualifiedNumber(0d, 0d));
    private final ReferenceAttribute<Long> timePeriod = newReferenceAttribute("timePeriod", 10 * 1000L);

    @Override
    public void onActivation() throws Exception {
        super.onActivation();

        subscribeToAttribute(self(), source.get(), sourceAttribute.get());
    }

    public void receive(SensorEvent event) {
        //System.out.println(self()+"RollingTimeWindowMeanEnricher:"+event);

        if (event.getNewValue() == null) {
            return;
        }

        double d = ((Number) event.getNewValue()).doubleValue();
        values.add(d);
        timestamps.add(event.getTimestamp());
        pruneValues(event.getTimestamp());
        Double average = getAverage(event.getTimestamp()).value;
        send(source, new AttributePublication<>(targetAttribute, average));
    }

    public ConfidenceQualifiedNumber getAverage(long now) {
        pruneValues(now);
        if (timestamps.isEmpty()) {
            lastAverage.set(new ConfidenceQualifiedNumber(lastAverage.get().value, 0.0d));
            return lastAverage.get();
        }

        long lastTimestamp = timestamps.get(timestamps.size() - 1);
        Double confidence = ((double) (timePeriod.get() - (now - lastTimestamp))) / timePeriod.get();
        if (confidence <= 0.0d) {
            double lastValue = values.get(values.size() - 1);
            lastAverage.set(new ConfidenceQualifiedNumber(lastValue, 0.0d));
            return lastAverage.get();
        }

        long start = (now - timePeriod.get());
        long end;
        double weightedAverage = 0.0d;

        Iterator<Double> valuesIter = values.iterator();
        Iterator<Long> timestampsIter = timestamps.iterator();
        while (valuesIter.hasNext()) {
            // Ignores out-of-date values (and also values that are received out-of-order, but that shouldn't happen!)
            double val = valuesIter.next();
            long timestamp = timestampsIter.next();
            if (timestamp >= start) {
                end = timestamp;
                weightedAverage += ((end - start) / (confidence * timePeriod.get())) * val;
                start = timestamp;
            }
        }

        lastAverage.set(new ConfidenceQualifiedNumber(weightedAverage, confidence));
        return lastAverage.get();
    }

    /**
     * Discards out-of-date values, but keeps at least one value.
     */
    private void pruneValues(long now) {
        while (timestamps.size() > 1 && timestamps.get(0) < (now - timePeriod.get())) {
            timestamps.removeFirst();
            values.removeFirst();
        }
    }

    public static class Config extends EntityConfig {
        public Config() {
            super(RollingTimeWindowMeanEnricher.class);
        }

        public Config targetAttribute(AttributeType<Double> attributeType) {
            addProperty(TARGET_ATTRIBUTE_TYPE, attributeType);
            return this;
        }

        public Config sourceAttribute(AttributeType<? extends Number> attributeType) {
            addProperty(SOURCE_ATTRIBUTE_TYPE, attributeType);
            return this;
        }

        public Config source(ActorRef source) {
            addProperty(SOURCE, source);
            return this;
        }
    }
}
