package com.hazelcast.actors.impl;

import com.hazelcast.actors.api.Actor;
import com.hazelcast.actors.api.ActorFactory;
import com.hazelcast.actors.api.ActorRecipe;
import com.hazelcast.actors.api.Injected;
import com.hazelcast.actors.api.exceptions.ActorInstantiationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hazelcast.actors.utils.Util.notNull;
import static java.lang.String.format;

/**
 * An {@link com.hazelcast.actors.api.ActorFactory} that creates an Actor based on the no-arg constructor and is able
 * to inject dependencies into this actor using reflection in combination with fields that have on @Autowired annotation.
 */
public class BasicActorFactory implements ActorFactory {
    private final Map<String, Object> dependencies;
    private final ConcurrentHashMap<Class<? extends Actor>, FieldSetter[]> fieldSetterMap = new ConcurrentHashMap<>();

    public BasicActorFactory() {
        this(Collections.EMPTY_MAP);
    }

    public BasicActorFactory(Map<String, Object> dependencies) {
        this.dependencies = notNull(dependencies, "dependencies");
    }

    @Override
    public <A extends Actor> A newActor(ActorRecipe<A> recipe) {
        notNull(recipe, "recipe");

        A actor;
        try {
            Constructor<A> constructor = recipe.getActorClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            actor = constructor.newInstance();
        } catch (NoSuchMethodError e) {
            throw new ActorInstantiationException(format("Failed to instantiate Actor class '%s' is missing mandatory no-arg constructor",
                    recipe.getActorClass().getName()), e);
        } catch (Exception e) {
            throw new ActorInstantiationException(
                    format("Failed to instantiate Actor class '%s' using recipe %s", recipe.getActorClass().getName(),recipe), e);
        }

        for (FieldSetter setter : getFieldSetters(recipe.getActorClass())) {
            setter.set(actor);
        }
        return actor;
    }

    private static class FieldSetter {
        private final Field field;
        private final Object value;

        private FieldSetter(Field field, Object value) {
            this.field = field;
            this.value = value;
        }

        public void set(Actor actor) {
            try {
                field.set(actor, value);
            } catch (IllegalAccessException e) {
                //should not happen since the field always is accessible.
                throw new RuntimeException(e);
            }
        }
    }

    private FieldSetter[] getFieldSetters(final Class<? extends Actor> actorClass) {
        FieldSetter[] fieldSetters = fieldSetterMap.get(actorClass);

        if (fieldSetters == null) {
            List<FieldSetter> fieldSetterList = new LinkedList<>();
            Class clazz = actorClass;
            do {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Injected.class)) {
                        field.setAccessible(true);

                        if (Modifier.isStatic(field.getModifiers())) {
                            throw new ActorInstantiationException(
                                    format("Can't inject to static field '%s.%s'",
                                            field.getDeclaringClass().getName(), field.getName()));

                        }

                        if (Modifier.isFinal(field.getModifiers())) {
                            throw new ActorInstantiationException(
                                    format("Can't inject to final field '%s.%s'",
                                            field.getDeclaringClass().getName(), field.getName()));

                        }

                        Object dependency = dependencies.get(field.getName());
                        if (dependency == null) {
                            throw new ActorInstantiationException(
                                    format("Can't inject to field '%s.%s', no dependency found with name '%s'",
                                            field.getDeclaringClass().getName(), field.getName(), field.getName()));
                        }


                        if (!field.getType().isAssignableFrom(dependency.getClass())) {
                            throw new ActorInstantiationException(
                                    format("Can't inject to field '%s.%s', type mismatch found: expected '%s' but found '%s'",
                                            field.getDeclaringClass().getName(), field.getName(),
                                            field.getType().getName(), dependency.getClass().getName()));

                        }
                        fieldSetterList.add(new FieldSetter(field, dependency));
                    }
                }

                clazz = clazz.getSuperclass();

            } while (clazz != null);

            fieldSetters = fieldSetterList.toArray(new FieldSetter[fieldSetterList.size()]);
            FieldSetter[] found = fieldSetterMap.putIfAbsent(actorClass, fieldSetters);
            fieldSetters = found == null ? fieldSetters : found;
        }

        return fieldSetters;
    }
}
