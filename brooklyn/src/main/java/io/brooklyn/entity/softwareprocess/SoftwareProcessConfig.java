package io.brooklyn.entity.softwareprocess;

import io.brooklyn.entity.EntityConfig;

import java.util.Map;

public class SoftwareProcessConfig<E extends SoftwareProcess> extends EntityConfig<E> {

    public SoftwareProcessConfig(Class<E> entityClazz) {
        super(entityClazz);
    }

    public SoftwareProcessConfig(Class<E> entityClazz, Map<String, Object> properties) {
        super(entityClazz, properties);
    }
}
