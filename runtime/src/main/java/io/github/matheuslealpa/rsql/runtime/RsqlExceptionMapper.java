package io.github.matheuslealpa.rsql.runtime;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import io.github.matheuslealpa.rsql.RsqlException;

/**
 * Reports an invalid filter as {@code 400 Bad Request}, with the message of the exception as plain
 * text. Registered by the extension when Quarkus REST is present, unless
 * {@code quarkus.rsql.map-exceptions} is {@code false}.
 * <p>
 * Internal to the extension, not part of its API.
 */
public class RsqlExceptionMapper implements ExceptionMapper<RsqlException> {

    @Override
    public Response toResponse(RsqlException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(exception.getMessage())
                .build();
    }

}
