package io.brooklyn.attributes;

public interface ListAttributeRef<E> extends AttributeRef {

    int size();

    boolean isEmpty();

    void add(E item);

    void remove(E item);

    E get(int index);

    E removeFirst();
}
