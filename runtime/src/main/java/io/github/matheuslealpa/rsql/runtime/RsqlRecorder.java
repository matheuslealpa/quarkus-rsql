package io.github.matheuslealpa.rsql.runtime;

import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RsqlRecorder {

    /**
     * Records the keys of the members annotated with {@link io.github.matheuslealpa.rsql.RsqlIgnore},
     * as built by {@link IgnoredAttributes#fieldKey} and {@link IgnoredAttributes#methodKey}.
     */
    public void setIgnoredMembers(Set<String> members) {
        IgnoredAttributes.set(members);
    }

    /**
     * Clears the cached entity metamodels on each start, because in dev mode the extension's
     * classes outlive a live reload while the entities and persistence units are replaced.
     */
    public void clearEntityTypes() {
        EntityTypes.clear();
    }

}
