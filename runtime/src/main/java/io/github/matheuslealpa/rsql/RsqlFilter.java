package io.github.matheuslealpa.rsql;

import java.util.Set;

/**
 * An RSQL filter received as a request parameter, for example in a REST endpoint:
 *
 * <pre>
 * &#64;GET
 * public List&lt;Fruit&gt; list(&#64;QueryParam("filter") &#64;DefaultValue("") RsqlFilter filter) {
 *     RsqlQuery query = filter.toQuery(Fruit.class);
 *     return Fruit.find(query.query(), query.parameters()).list();
 * }
 * </pre>
 *
 * The filter is only parsed by {@link #toQuery(Class)}, once the entity is known, so that an invalid
 * filter throws an {@link RsqlException} instead of failing the parameter conversion, which REST
 * frameworks report as {@code 404 Not Found} for query parameters.
 */
public final class RsqlFilter {

    private final String filter;

    private RsqlFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Creates a filter from its RSQL string, as REST frameworks do to convert parameters.
     *
     * @param filter the RSQL filter, may be {@code null} or blank to match all entities
     * @return the filter, not yet parsed
     */
    public static RsqlFilter fromString(String filter) {
        return new RsqlFilter(filter);
    }

    /**
     * Translates the filter into an HQL query for the given entity.
     *
     * @see Rsql#toQuery(String, Class)
     */
    public RsqlQuery toQuery(Class<?> entityClass) {
        return Rsql.toQuery(this.filter, entityClass);
    }

    /**
     * Translates the filter into an HQL query for the given entity, accepting only the given
     * selectors.
     *
     * @see Rsql#toQuery(String, Class, Set)
     */
    public RsqlQuery toQuery(Class<?> entityClass, Set<String> selectors) {
        return Rsql.toQuery(this.filter, entityClass, selectors);
    }

    @Override
    public String toString() {
        return this.filter;
    }

}
