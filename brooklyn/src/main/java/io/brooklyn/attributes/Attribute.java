package io.brooklyn.attributes;

/**
 * An Attribute is a single occurrence of a key/value pair (although value can also be a complex data-structure
 * like the List).
 *
 * If there are 10 entities, that all have an attribute 'foo'. There only needs to be a single AttributeType
 * name with some type (e.g. Long) and name 'foo'. But there will be 10 attribute instances since each entity
 * will have its own instance of that attribute.
 *
 * So an Attribute can be compared to an AtomicReference, that stores some reference. This will become visible if
 * you look at the {@link ReferenceAttribute}.
 *
 *
 * @author Peter Veentjer.
 */
public interface Attribute {

    /**
     * The name of this attribute.
     *
     * @return
     */
    String getName();

    /**
     * Returns the AttributeType that describes this attribute.
     *
     * @return the AttributeType.
     */
    AttributeType getAttributeType();
}
