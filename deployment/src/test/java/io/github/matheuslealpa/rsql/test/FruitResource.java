package io.github.matheuslealpa.rsql.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.github.matheuslealpa.rsql.Rsql;

/**
 * Returns the query generated for the filter, or fails with an {@code RsqlException}.
 */
@Path("/fruits")
public class FruitResource {

    @GET
    public String query(@QueryParam("filter") String filter) {
        return Rsql.toQuery(filter, Fruit.class).query();
    }

}
