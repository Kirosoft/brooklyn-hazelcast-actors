package io.brooklyn.entity.softwareprocess;

import io.brooklyn.entity.EntityConfig;

import java.util.Map;

/**
 * The EntityConfig for the SoftwareProcess.
 *
 * @param <E>
 */
public class SoftwareProcessConfig<E extends SoftwareProcess> extends EntityConfig<E> {

    public SoftwareProcessConfig(Class<E> entityClazz) {
        super(entityClazz);
    }
}
