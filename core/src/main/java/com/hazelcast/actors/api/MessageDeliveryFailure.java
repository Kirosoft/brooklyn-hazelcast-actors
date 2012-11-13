package com.hazelcast.actors.api;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNull;

public class MessageDeliveryFailure implements Serializable {
    private final ActorRef sender;
    private final ActorRef destination;
    private final Exception exception;

    public MessageDeliveryFailure(ActorRef destination, Exception exception) {
        this(destination, null, exception);
    }

    public MessageDeliveryFailure(ActorRef destination, ActorRef sender, Exception exception) {
        this.destination = notNull(destination, "destination");
        this.sender = sender;
        this.exception = exception;
    }

    public ActorRef getDestination() {
        return destination;
    }

    public Exception getException() {
        return exception;
    }

    public ActorRef getSender() {
        return sender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageDeliveryFailure that = (MessageDeliveryFailure) o;

        if (!destination.equals(that.destination)) return false;
        if (exception != null ? !exception.equals(that.exception) : that.exception != null) return false;
        if (sender != null ? !sender.equals(that.sender) : that.sender != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sender != null ? sender.hashCode() : 0;
        result = 31 * result + destination.hashCode();
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }
}
