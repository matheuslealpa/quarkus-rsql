package io.github.matheuslealpa.rsql.it;

import java.util.List;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.github.matheuslealpa.rsql.Rsql;
import io.github.matheuslealpa.rsql.RsqlFilter;
import io.github.matheuslealpa.rsql.RsqlQuery;
import io.quarkus.panache.common.Sort;

@Path("/farms")
public class FarmResource {

    @GET
    public List<String> list(@QueryParam("filter") @DefaultValue("") RsqlFilter filter) {
        RsqlQuery query = filter.toQuery(Farm.class);
        return Farm.<Farm> find(query.query(), Sort.by(Rsql.ALIAS + ".id"), query.parameters())
                .stream()
                .map((farm) -> farm.name)
                .toList();
    }

}
