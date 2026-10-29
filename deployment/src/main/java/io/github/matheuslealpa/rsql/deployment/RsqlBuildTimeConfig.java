package io.github.matheuslealpa.rsql.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rsql")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface RsqlBuildTimeConfig {

    /**
     * Whether an {@code RsqlException}, thrown by an invalid filter, is reported as
     * {@code 400 Bad Request} with its message as plain text. Only applies when Quarkus REST is
     * present. Disable it to map the exception yourself.
     */
    @WithDefault("true")
    boolean mapExceptions();

}
