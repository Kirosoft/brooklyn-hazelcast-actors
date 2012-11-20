package io.brooklyn.entity;

import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.locations.Location;

import java.io.Serializable;

public class Start implements Serializable {
    public final Location location;

    public Start(Location location) {
        this.location = location;
    }

    public Start(BasicAttributeRef<Location> location) {
        this(location.get());
    }
}
