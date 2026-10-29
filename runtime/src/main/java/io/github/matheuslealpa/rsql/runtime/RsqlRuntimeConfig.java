package io.github.matheuslealpa.rsql.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Limits on the filters received from clients, so that a filter can't make the parser and the
 * database work arbitrarily hard. Exceeding one throws an {@code RsqlException}.
 */
@ConfigMapping(prefix = "quarkus.rsql")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RsqlRuntimeConfig {

    /**
     * The maximum length of a filter, and of a sort, in characters, checked before parsing them.
     */
    @WithDefault("2048")
    int maxFilterLength();

    /**
     * The maximum number of comparisons, such as {@code name==Apple}, in a filter.
     */
    @WithDefault("64")
    int maxComparisons();

    /**
     * The maximum number of values of an {@code =in=} or {@code =out=} comparison.
     */
    @WithDefault("256")
    int maxInValues();

}
