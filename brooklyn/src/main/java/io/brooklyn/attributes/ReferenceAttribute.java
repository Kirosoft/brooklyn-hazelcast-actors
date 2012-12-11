package io.brooklyn.attributes;

/**
 * An {@link Attribute} that contains some reference to an object.
 *
 * It looks a lot like the {@link java.util.concurrent.atomic.AtomicReference} although we don't need to deal
 * with concurrency here since attributes are part of an entity, and entity is an actor, and an actor is always accessed
 * by at most 1 thread.
 *
 * @param <E>
 */
public interface ReferenceAttribute<E> extends Attribute {

    /**
     * Gets the current value. Could be null.
     *
     * @return the current value.
     */
    E get();

    /**
     * Sets the new value. The value is allowed to be null.
     *
     * @param newValue the new value.
     */
    void set(E newValue);

    /**
     * Checks if there currently is a value stored.
     *
     * @return true if there is a value stored, false otherwise.
     */
    boolean isNull();
}
