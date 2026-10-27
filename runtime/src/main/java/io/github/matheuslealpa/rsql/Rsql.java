package io.github.matheuslealpa.rsql;

import java.util.Objects;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;

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
 * Selectors must match a field of the entity class, or a path through its single-valued
 * associations and embeddables such as {@code farm.name}. Values are always bound as named
 * parameters, so a filter can't inject arbitrary HQL.
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
        Objects.requireNonNull(entityClass, "entityClass");
        if (filter == null || filter.isBlank()) {
            return new HqlBuilder(entityClass).build(null);
        }
        Node node;
        try {
            node = PARSER.parse(filter);
        } catch (RSQLParserException ex) {
            throw new RsqlException("Invalid RSQL filter: " + filter, ex);
        }
        return new HqlBuilder(entityClass).build(node);
    }

}
