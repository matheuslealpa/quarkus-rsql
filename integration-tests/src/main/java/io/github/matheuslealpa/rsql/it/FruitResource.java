package io.github.matheuslealpa.rsql.it;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.github.matheuslealpa.rsql.Rsql;
import io.github.matheuslealpa.rsql.RsqlException;
import io.github.matheuslealpa.rsql.RsqlQuery;
import io.quarkus.panache.common.Sort;

@Path("/fruits")
public class FruitResource {

    @GET
    public List<String> list(@QueryParam("filter") String filter) {
        RsqlQuery query = Rsql.toQuery(filter, Fruit.class);
        return Fruit.<Fruit> find(query.query(), Sort.by("id"), query.parameters())
                .stream()
                .map((fruit) -> fruit.name)
                .toList();
    }

    @ServerExceptionMapper
    Response invalidFilter(RsqlException ex) {
        return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
    }

}
