package io.github.matheuslealpa.rsql;

import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * Translates RSQL filters into Hibernate ORM (HQL) queries.
 * <p>
 * The resulting {@link RsqlQuery} can be passed directly to Panache:
 *
 * <pre>
 * RsqlQuery query = Rsql.toQuery("name==Apple;weight=gt=100", Fruit.class);
 * List&lt;Fruit&gt; fruits = Fruit.find(query.query(), query.parameters()).list();
 * </pre>
 *
 * Selectors must match a persistent attribute of the entity, or a path through its single-valued
 * associations and embeddables such as {@code farm.name}, as described by the Hibernate ORM
 * metamodel. Attributes annotated with {@link RsqlIgnore} are never filterable, and
 * {@link #toQuery(String, Class, Set)} restricts the selectors further. Values are always bound as
 * named parameters, so a filter can't inject arbitrary HQL.
 */
public final class Rsql {

    /**
     * The alias of the entity in the generated queries. Use it to qualify properties in other
     * clauses of the query, for example {@code Sort.by(Rsql.ALIAS + ".name")}.
     */
    public static final String ALIAS = HqlBuilder.ROOT_ALIAS;

    private static final RSQLParser PARSER = new RSQLParser();

    private Rsql() {
    }

    /**
     * Translates an RSQL filter into an HQL query for the given entity.
     *
     * @param filter the RSQL filter, may be {@code null} or blank to match all entities
     * @param entityClass the entity the filter applies to
     * @return the HQL query and its named parameters
     * @throws RsqlException if the filter is invalid or references an unknown property
     */
    public static RsqlQuery toQuery(String filter, Class<?> entityClass) {
        return toQuery(filter, null, entityClass, null);
    }

    /**
     * Translates an RSQL filter into an HQL query for the given entity, accepting only the given
     * selectors, such as {@code name} or {@code farm.name}.
     * <p>
     * Use it to expose only some properties to the clients of an API. Any other selector is
     * rejected as an unknown property, so that the existence of the others isn't revealed.
     *
     * @param filter the RSQL filter, may be {@code null} or blank to match all entities
     * @param entityClass the entity the filter applies to
     * @param selectors the selectors the filter may use
     * @return the HQL query and its named parameters
     * @throws RsqlException if the filter is invalid or references an unknown or not allowed
     *         property
     */
    public static RsqlQuery toQuery(String filter, Class<?> entityClass, Set<String> selectors) {
        return toQuery(filter, null, entityClass, selectors);
    }

    /**
     * Translates an RSQL filter and a sort into an HQL query for the given entity.
     * <p>
     * The sort is a comma-separated list of selectors, each prefixed with {@code -} to sort in
     * descending order, for example {@code -price,farm.name}. Only basic properties can be sorted,
     * and associations in their paths are joined like in the filter.
     *
     * @param filter the RSQL filter, may be {@code null} or blank to match all entities
     * @param sort the sort, may be {@code null} or blank to leave the order unspecified
     * @param entityClass the entity the filter applies to
     * @return the HQL query, with an {@code order by} clause if sorted, and its named parameters
     * @throws RsqlException if the filter or the sort is invalid or references an unknown property
     */
    public static RsqlQuery toQuery(String filter, String sort, Class<?> entityClass) {
        return toQuery(filter, sort, entityClass, null);
    }

    /**
     * Translates an RSQL filter and a sort into an HQL query for the given entity, accepting only
     * the given selectors in both.
     *
     * @param filter the RSQL filter, may be {@code null} or blank to match all entities
     * @param sort the sort, may be {@code null} or blank to leave the order unspecified
     * @param entityClass the entity the filter applies to
     * @param selectors the selectors the filter and the sort may use, or {@code null} to accept
     *        any persistent attribute
     * @return the HQL query, with an {@code order by} clause if sorted, and its named parameters
     * @throws RsqlException if the filter or the sort is invalid or references an unknown or not
     *         allowed property
     * @see #toQuery(String, Class, Set)
     * @see #toQuery(String, String, Class)
     */
    public static RsqlQuery toQuery(String filter, String sort, Class<?> entityClass, Set<String> selectors) {
        Objects.requireNonNull(entityClass, "entityClass");
        HqlBuilder builder = new HqlBuilder(entityType(entityClass), selectors);
        if (filter == null || filter.isBlank()) {
            return builder.build(null, sort);
        }
        Node node;
        try {
            node = PARSER.parse(filter);
        } catch (RSQLParserException ex) {
            throw new RsqlException("Invalid RSQL filter: " + filter, ex);
        }
        return builder.build(node, sort);
    }

    /**
     * Finds the metamodel of the entity in the persistence unit that manages it.
     */
    private static EntityType<?> entityType(Class<?> entityClass) {
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
