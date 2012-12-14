package io.brooklyn.attributes;

//TODO: add functionality to listen to list changes; item added, item removed etc.
public interface ListAttribute<E> extends Attribute, Iterable<E> {

    int size();

    boolean isEmpty();

    void add(E item);

    boolean remove(E item);

    E get(int index);

    E removeFirst();
}
