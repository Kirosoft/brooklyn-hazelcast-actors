package io.brooklyn.attributes;

/**
 * An Attribute that stores a primitive int.
 *
 * @author Peter Veentjer.
 */
public interface IntAttribute extends Attribute {

    int get();

    void set(int newValue);

    int getAndInc();
}
