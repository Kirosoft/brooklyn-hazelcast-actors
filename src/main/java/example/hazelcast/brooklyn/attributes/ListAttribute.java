package example.hazelcast.brooklyn.attributes;

import example.hazelcast.brooklyn.attributes.Attribute;

public interface ListAttribute<E> extends Attribute {

    int size();

    boolean isEmpty();

    void add(E item);

    void remove(E item);

    E get(int index);

    E removeFirst();
}
