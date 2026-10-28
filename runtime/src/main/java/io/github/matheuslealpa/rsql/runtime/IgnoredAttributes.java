package io.github.matheuslealpa.rsql.runtime;

import java.util.Set;

/**
 * The fields and getters annotated with {@link io.github.matheuslealpa.rsql.RsqlIgnore}, collected
 * from the Jandex index at build time, so that they are found without reflection at runtime.
 * <p>
 * Internal to the extension, not part of its API.
 */
public final class IgnoredAttributes {

    private static volatile Set<String> members;

    private IgnoredAttributes() {
    }

    static void set(Set<String> members) {
        IgnoredAttributes.members = Set.copyOf(members);
    }

    /**
     * Returns the key of a field, such as {@code com.acme.Owner#passwordHash}.
     */
    public static String fieldKey(String className, String field) {
        return className + "#" + field;
    }

    /**
     * Returns the key of a method without parameters, such as {@code com.acme.Owner#getApiToken()}.
     */
    public static String methodKey(String className, String method) {
        return className + "#" + method + "()";
    }

    /**
     * Whether the given field, or one of the given getters, is declared on the type and annotated.
     */
    public static boolean contains(Class<?> declaringType, String field, String... getters) {
        Set<String> members = IgnoredAttributes.members;
        if (members == null) {
            // Fail closed: without the collected members, ignored attributes would be filterable
            throw new IllegalStateException("The RSQL extension wasn't initialized");
        }
        if (members.contains(fieldKey(declaringType.getName(), field))) {
            return true;
        }
        for (String getter : getters) {
            if (members.contains(methodKey(declaringType.getName(), getter))) {
                return true;
            }
        }
        return false;
    }

}
