package io.github.matheuslealpa.rsql;

import java.util.Map;

/**
 * An HQL restriction produced from an RSQL filter, with its named parameters.
 *
 * @param query the HQL restriction, usable as a Panache query
 * @param parameters the named parameters referenced by the restriction
 */
public record RsqlQuery(String query, Map<String, Object> parameters) {

    static final RsqlQuery MATCH_ALL = new RsqlQuery("1 = 1", Map.of());

    public RsqlQuery {
        parameters = Map.copyOf(parameters);
    }

}
