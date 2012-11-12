package io.brooklyn.attributes;

public class AttributeType<E> {
    private final String name;
    private final Object defaultValue;

    public AttributeType(String name) {
        this(name, null);
    }

    public AttributeType(String name, Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getName() {
        return name;
    }
}
