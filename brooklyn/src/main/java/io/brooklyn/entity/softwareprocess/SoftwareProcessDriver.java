package io.brooklyn.entity.softwareprocess;

/**
 *
 *
 * A SoftwareProcessDriver is a throw away object. If a machine where a softwareprocess-entity is running fails,
 * the attributes of that entity will be highly available. Therefor the entity can be restored and the driver can
 * be recreated. So the SoftwareProcessDriver should not store any highly available data; if you need such a thing,
 * store it in the attributes of the entity.
 */
public interface SoftwareProcessDriver {

    void install();

    void customize();

    void launch();

    void stop();

    boolean isRunning();
}
