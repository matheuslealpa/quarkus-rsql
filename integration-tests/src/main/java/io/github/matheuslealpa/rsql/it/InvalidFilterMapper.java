package io.github.matheuslealpa.rsql.it;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.github.matheuslealpa.rsql.RsqlException;

/**
 * Reports invalid filters as {@code 400 Bad Request} for every resource.
 */
public class InvalidFilterMapper {

    @ServerExceptionMapper
    Response invalidFilter(RsqlException ex) {
        return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
    }

}
