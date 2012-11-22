package io.brooklyn.attributes;

import com.hazelcast.actors.api.ActorRef;

import java.io.Serializable;


public class SensorEvent implements Serializable {
    private final ActorRef source;
    private final Object oldValue;
    private final Object newValue;
    private final String name;
    private final long timestamp;

    public SensorEvent(ActorRef source, String name, Object oldValue, Object newValue) {
        this.newValue = newValue;
        this.name = name;
        this.source = source;
        this.oldValue = oldValue;
        this.timestamp = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public ActorRef getSource() {
        return source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SensorEvent{" +
                "source=" + source +
                ", name=" + name +
                ", old=" + oldValue +
                ", new=" + newValue +
                ", timestamp=" + timestamp +
                '}';
    }
}
