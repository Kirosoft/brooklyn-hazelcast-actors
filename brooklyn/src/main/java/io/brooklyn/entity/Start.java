package io.brooklyn.entity;

import brooklyn.location.Location;
import io.brooklyn.attributes.BasicAttributeRef;

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
