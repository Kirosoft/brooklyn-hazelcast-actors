package example.hazelcast.brooklyn.attributes;

public interface BasicAttribute<E> extends Attribute{

    E get();

    void set(E newValue);
}
