package io.brooklyn.entity.policies;

import io.brooklyn.entity.Entity;

/**
 * The Policy is an Entity, so it can do the same things an entity can do. Also by making use of the attribute map
 * of the entity, the policy is highly available.
 */
public abstract class Policy extends Entity {
}
