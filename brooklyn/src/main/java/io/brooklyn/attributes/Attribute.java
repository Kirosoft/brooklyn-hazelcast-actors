package io.brooklyn.attributes;

import java.io.Serializable;

public class Attribute<E> implements Serializable {
    private final String name;
    private final E defaultValue;

    public Attribute(String name) {
        this(name, null);
    }

    public Attribute(String name, E defaultValue) {
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
