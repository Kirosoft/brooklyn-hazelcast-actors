package io.brooklyn.entity;

import io.brooklyn.attributes.AttributeType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import static com.hazelcast.actors.utils.Util.notNull;

/**
 * Contains the configuration to create an Entity.
 *
 * Essentially the EntityConfig is a Map with strings (the name of the property) as key and some object as value.
 *
 * The EntityConfig can be extended, e.g. to create an EntityConfig with entity specific setters. E.g. Tomcat +
 * TomcatConfig. Where you could add attributes and getters/setters on the TomcatConfig to give it a less error
 * prone configuration.
 *
 * All the properties inside the EntityConfig are automatically copied to the attributes of an entity when the Entity
 * is started.
 *
 * EntityConfig is mutable and not threadsafe. In most cases they will be created (written) before they are used (read).
 * So this should not be an issue. Using an immutable approach would also be a solution.
 *
 * @param <E>
 */
public class EntityConfig<E extends Entity> implements Serializable {

    private final Map<String, Object> properties;
    //we should not pass the class around (we don't want to send the class definition to another jvm...
    //so we should store the string).
    private final String entityClazz;

    public EntityConfig(Class<E> entityClazz) {
        this(entityClazz, new HashMap<String, Object>());
    }

    public EntityConfig(Class<E> entityClazz, Map<String, Object> properties) {
        this.entityClazz = notNull(entityClazz, "entityClazz").getName();
        this.properties = notNull(properties, "config");
    }

    public Class<E> getEntityClass() {
        try {
            return (Class<E>)Class.forName(entityClazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: Currently we always overwrite; but perhaps we want to have options for this.
    public void addProperties(Entity entity){
        notNull(entity,"entity");

        Map<String,Object> inheritableProps = entity.getAttributeMap().getInheritableAttributes();
        properties.putAll(inheritableProps);
    }

    public <A> EntityConfig<E> addProperty(AttributeType<A> attributeType, A value) {
        notNull(attributeType, "attribute");
        return addProperty(attributeType.getName(), value);
    }

    public EntityConfig<E> addProperty(String name, Object value) {
        notNull(name, "name");
        notNull(value, "value");
        properties.put(name, value);
        return this;
    }

    public Iterator<String> getPropertyNames() {
        return new HashSet<>(properties.keySet()).iterator();
    }

    public <A> A getProperty(AttributeType<A> attributeType){
        notNull(attributeType,"attribute");
        return (A)properties.get(attributeType.getName());
    }

    public Object getProperty(String name) {
        notNull(name, "name");
        return properties.get(name);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "EntityConfig{" +
                "config=" + properties +
                '}';
    }
}
