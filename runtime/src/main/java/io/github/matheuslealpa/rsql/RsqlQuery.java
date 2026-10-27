package io.github.matheuslealpa.rsql;

import java.util.Map;

/**
 * An HQL query produced from an RSQL filter, with its named parameters.
 * <p>
 * The query selects the entity with the alias {@code r} and joins its associations with left
 * joins, for example {@code select r from Fruit r left join r.farm j0 where j0.name = :p0}.
 *
 * @param query the HQL query, usable as a Panache query
 * @param parameters the named parameters referenced by the restriction
 */
public record RsqlQuery(String query, Map<String, Object> parameters) {

    public RsqlQuery {
        parameters = Map.copyOf(parameters);
    }

}
