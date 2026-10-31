package io.github.matheuslealpa.rsql.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.Any;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * The metamodel of the entities, looked up in their persistence unit the first time each one is
 * filtered and cached afterwards, since it never changes once the application has started.
 * <p>
 * Internal to the extension, not part of its API.
 */
public final class EntityTypes {

    private static final Map<Class<?>, EntityType<?>> ENTITY_TYPES = new ConcurrentHashMap<>();

    private EntityTypes() {
    }

    /**
     * Returns the metamodel of the entity, looking it up only the first time.
     *
     * @throws IllegalArgumentException if the class isn't an entity of any persistence unit
     */
    public static EntityType<?> get(Class<?> entityClass) {
        return ENTITY_TYPES.computeIfAbsent(entityClass, EntityTypes::find);
    }

    /**
     * Forgets the cached metamodels, which belong to the classes and persistence units of a
     * previous start of the application in dev mode.
     */
    static void clear() {
        ENTITY_TYPES.clear();
    }

    /**
     * Finds the metamodel of the entity in the persistence unit that manages it.
     */
    private static EntityType<?> find(Class<?> entityClass) {
        ArcContainer container = Arc.container();
        if (container == null) {
            throw new IllegalStateException("Rsql requires a running Quarkus application");
        }
        for (EntityManagerFactory factory : container.select(EntityManagerFactory.class, Any.Literal.INSTANCE)
                .listActive()) {
            for (EntityType<?> type : factory.getMetamodel().getEntities()) {
                if (type.getJavaType() == entityClass) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException(entityClass.getName() + " isn't an entity of any persistence unit");
    }

}
