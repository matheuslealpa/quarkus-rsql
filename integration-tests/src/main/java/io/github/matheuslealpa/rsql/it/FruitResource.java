package io.github.matheuslealpa.rsql.it;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.github.matheuslealpa.rsql.Rsql;
import io.github.matheuslealpa.rsql.RsqlException;
import io.github.matheuslealpa.rsql.RsqlFilter;
import io.github.matheuslealpa.rsql.RsqlQuery;
import io.github.matheuslealpa.rsql.RsqlSort;
import io.quarkus.panache.common.Sort;

@Path("/fruits")
public class FruitResource {

    @GET
    public List<String> list(@QueryParam("filter") @DefaultValue("") RsqlFilter filter,
            @QueryParam("sort") @DefaultValue("id") RsqlSort sort) {
        RsqlQuery query = filter.toQuery(Fruit.class, sort);
        return Fruit.<Fruit> find(query.query(), query.parameters())
                .stream()
                .map((fruit) -> fruit.name)
                .toList();
    }

    @GET
    @Path("/by-name-or-color")
    public List<String> listByNameOrColor(@QueryParam("filter") String filter) {
        RsqlQuery query = Rsql.toQuery(filter, Fruit.class, Set.of("name", "color"));
        return Fruit.<Fruit> find(query.query(), Sort.by(Rsql.ALIAS + ".id"), query.parameters())
                .stream()
                .map((fruit) -> fruit.name)
                .toList();
    }

    @GET
    @Path("/count")
    public long count(@QueryParam("filter") @DefaultValue("") RsqlFilter filter) {
        RsqlQuery query = filter.toQuery(Fruit.class);
        return Fruit.count(query.query(), query.parameters());
    }

    @ServerExceptionMapper
    Response invalidFilter(RsqlException ex) {
        return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
    }

}
