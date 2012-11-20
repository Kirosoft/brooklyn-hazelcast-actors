package io.brooklyn.attributes;

public interface IntAttributeRef extends AttributeRef {

    int get();

    void set(int newValue);

    int getAndInc();
}
