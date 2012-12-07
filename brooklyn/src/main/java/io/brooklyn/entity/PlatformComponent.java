package io.brooklyn.entity;

import brooklyn.location.Location;
import brooklyn.location.PortRange;
import io.brooklyn.attributes.Attribute;
import io.brooklyn.attributes.BasicAttributeRef;
import io.brooklyn.attributes.PortAttributeRef;

public class PlatformComponent extends Entity {

    public final BasicAttributeRef<Location> location = newBasicAttributeRef("location");

    public final PortAttributeRef newPortAttributeRef(Attribute<PortRange> attribute) {
        return getAttributeMap().newPortAttributeRef(attribute);
    }
}
