package io.brooklyn.attributes;

public interface ListAttribute<E> extends Attribute {

    int size();

    boolean isEmpty();

    void add(E item);

    void remove(E item);

    E get(int index);

    E removeFirst();
}
