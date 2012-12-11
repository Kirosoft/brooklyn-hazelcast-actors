package io.brooklyn.attributes;

/**
 * An {@link Attribute} that stores a primitive long.
 */
public interface LongAttribute extends Attribute {

    long get();

    void set(long newValue);

    long getAndInc();
}
