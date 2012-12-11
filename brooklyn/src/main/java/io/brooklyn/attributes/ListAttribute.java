package io.brooklyn.attributes;

public interface ListAttribute<E> extends Attribute, Iterable<E> {

    int size();

    boolean isEmpty();

    void add(E item);

    boolean remove(E item);

    E get(int index);

    E removeFirst();
}
