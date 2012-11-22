package io.brooklyn.entity;

import brooklyn.location.Location;
import io.brooklyn.AbstractMessage;
import io.brooklyn.attributes.BasicAttributeRef;

public class Start extends AbstractMessage {
    public final Location location;

    public Start(Location location) {
        this.location = location;
    }

    public Start(BasicAttributeRef<Location> location) {
        this(location.get());
    }
}
