package io.brooklyn.attributes;

public interface LongAttributeRef extends AttributeRef {

    long get();

    void set(long newValue);

    long getAndInc();
}
