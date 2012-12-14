package io.brooklyn.entity.enrichers;

import io.brooklyn.entity.EntityConfig;

import java.util.Map;

public class EnricherConfig<E extends Enricher> extends EntityConfig<E> {

    public EnricherConfig(Class<E> enricherClass) {
        super(enricherClass);
    }

    public EnricherConfig(Class<E> enricherClass, Map<String, Object> properties) {
        super(enricherClass, properties);
    }
}
