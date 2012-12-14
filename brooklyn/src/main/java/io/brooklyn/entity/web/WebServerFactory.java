package io.brooklyn.entity.web;

import io.brooklyn.entity.Entity;
import io.brooklyn.entity.EntityReference;

import java.io.Serializable;

public interface WebServerFactory extends Serializable {
    EntityReference newWebServer(Entity owner);
}
