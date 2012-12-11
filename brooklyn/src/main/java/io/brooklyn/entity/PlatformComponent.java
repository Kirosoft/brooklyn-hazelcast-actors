package io.brooklyn.entity;

import brooklyn.location.Location;
import brooklyn.location.PortRange;
import io.brooklyn.attributes.AttributeType;
import io.brooklyn.attributes.ReferenceAttribute;
import io.brooklyn.attributes.PortAttribute;

//TODO: It is unclear what the purpose of the PlatformComponent is.
public class PlatformComponent extends Entity {

    public final ReferenceAttribute<Location> location = newReferenceAttribute("location");

    public final PortAttribute newPortAttributeRef(AttributeType<PortRange> attributeType) {
        return getAttributeMap().newPortAttribute(attributeType);
    }
}
