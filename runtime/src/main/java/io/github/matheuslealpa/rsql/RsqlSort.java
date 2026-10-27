package io.github.matheuslealpa.rsql;

/**
 * A sort received as a request parameter, such as {@code -price,farm.name}: a comma-separated list
 * of selectors, each prefixed with {@code -} to sort in descending order.
 * <p>
 * Pass it with an {@link RsqlFilter} so that the filter and the sort share their joins:
 *
 * <pre>
 * &#64;GET
 * public List&lt;Fruit&gt; list(&#64;QueryParam("filter") &#64;DefaultValue("") RsqlFilter filter,
 *         &#64;QueryParam("sort") &#64;DefaultValue("id") RsqlSort sort) {
 *     RsqlQuery query = filter.toQuery(Fruit.class, sort);
 *     return Fruit.find(query.query(), query.parameters()).list();
 * }
 * </pre>
 *
 * Like {@link RsqlFilter}, the sort is only validated when translated into a query, so that an
 * invalid sort throws an {@link RsqlException}.
 */
public final class RsqlSort {

    private final String sort;

    private RsqlSort(String sort) {
        this.sort = sort;
    }

    /**
     * Creates a sort from its string, as REST frameworks do to convert parameters.
     *
     * @param sort the sort, may be {@code null} or blank to leave the order unspecified
     * @return the sort, not yet validated
     */
    public static RsqlSort fromString(String sort) {
        return new RsqlSort(sort);
    }

    @Override
    public String toString() {
        return this.sort;
    }

}
