package io.brooklyn.attributes;

import java.io.Serializable;

/**
 * An AttributeType describes an {@link Attribute}.
 *
 * @param <E>
 */
public class AttributeType<E> implements Serializable {
    private final String name;
    private final E defaultValue;

    public AttributeType(String name) {
        this(name, null);
    }

    public AttributeType(String name, E defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public E getDefaultValue() {
        return defaultValue;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", defaultValue=" + defaultValue +
                '}';
    }
}
