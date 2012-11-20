package io.brooklyn.attributes;

public interface BasicAttributeRef<E> extends AttributeRef {

    E get();

    void set(E newValue);

    boolean isNull();
}
