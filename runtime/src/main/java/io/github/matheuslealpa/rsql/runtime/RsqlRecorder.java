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

}
