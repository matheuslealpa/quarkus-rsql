package io.github.matheuslealpa.rsql;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

/**
 * Builds an HQL query from an RSQL node tree. Not thread-safe; create one per filter.
 * <p>
 * Associations to other entities are joined with left joins, so that entities whose
 * association is {@code null} are still matched by the other conditions of an {@code or}.
 */
final class HqlBuilder implements RSQLVisitor<String, Void> {

    private static final Pattern PROPERTY_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final String ENTITY_ANNOTATION = "jakarta.persistence.Entity";

    static final String ROOT_ALIAS = "r";

    private final Class<?> entityClass;

    private final Map<String, Object> parameters = new HashMap<>();

    /** Joined association paths, such as {@code r.farm}, mapped to their alias. */
    private final Map<String, String> joins = new LinkedHashMap<>();

    HqlBuilder(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    RsqlQuery build(Node node) {
        String restriction = (node != null) ? node.accept(this) : null;
        StringBuilder query = new StringBuilder("select ").append(ROOT_ALIAS).append(" from ")
                .append(entityName(this.entityClass)).append(' ').append(ROOT_ALIAS);
        this.joins.forEach((path, alias) -> query.append(" left join ").append(path).append(' ').append(alias));
        if (restriction != null) {
            query.append(" where ").append(restriction);
        }
        return new RsqlQuery(query.toString(), this.parameters);
    }

    @Override
    public String visit(AndNode node, Void param) {
        return join(node, " and ");
    }

    @Override
    public String visit(OrNode node, Void param) {
        return join(node, " or ");
    }

    @Override
    public String visit(ComparisonNode node, Void param) {
        String selector = node.getSelector();
        Property property = resolve(selector);
        String path = property.path();
        Class<?> type = property.type();
        ComparisonOperator operator = node.getOperator();
        List<String> arguments = node.getArguments();
        if (operator.equals(RSQLOperators.IS_NULL)) {
            return path + " is null";
        }
        if (operator.equals(RSQLOperators.NOT_NULL)) {
            return path + " is not null";
        }
        if (operator.equals(RSQLOperators.IN) || operator.equals(RSQLOperators.NOT_IN)) {
            boolean in = operator.equals(RSQLOperators.IN);
            if (arguments.isEmpty()) {
                return in ? "1 = 0" : "1 = 1";
            }
            List<Object> values = new ArrayList<>(arguments.size());
            for (String argument : arguments) {
                values.add(ValueConverter.convert(argument, type, selector));
            }
            return path + (in ? " in " : " not in ") + parameter(values);
        }
        String argument = arguments.get(0);
        if (operator.equals(RSQLOperators.EQUAL) || operator.equals(RSQLOperators.NOT_EQUAL)) {
            boolean equal = operator.equals(RSQLOperators.EQUAL);
            if (type == String.class && argument.indexOf('*') >= 0) {
                return path + (equal ? " like " : " not like ") + parameter(likePattern(argument)) + " escape '\\'";
            }
            return path + (equal ? " = " : " <> ") + parameter(ValueConverter.convert(argument, type, selector));
        }
        String comparison = comparison(operator);
        return path + " " + comparison + " " + parameter(ValueConverter.convert(argument, type, selector));
    }

    private String join(LogicalNode node, String operator) {
        return node.getChildren().stream()
                .map((child) -> child.accept(this))
                .collect(Collectors.joining(operator, "(", ")"));
    }

    private String parameter(Object value) {
        String name = "p" + this.parameters.size();
        this.parameters.put(name, value);
        return ":" + name;
    }

    /**
     * Resolves a selector such as {@code name} or {@code farm.address.city} to its HQL path
     * and Java type. Intermediate segments must be single-valued associations, which are
     * joined, or embeddables.
     */
    private Property resolve(String selector) {
        String[] segments = selector.split("\\.", -1);
        String path = ROOT_ALIAS;
        Class<?> type = this.entityClass;
        for (int i = 0; i < segments.length; i++) {
            Field field = findField(type, segments[i]);
            if (field == null) {
                throw new RsqlException("Unknown property '" + selector + "' for " + this.entityClass.getSimpleName());
            }
            type = field.getType();
            String fieldPath = path + "." + segments[i];
            if (i == segments.length - 1) {
                return new Property(fieldPath, type);
            }
            if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type) || type.isArray()) {
                throw new RsqlException("Collection property '" + segments[i] + "' isn't supported in '" + selector + "'");
            }
            if (ValueConverter.isBasic(type)) {
                throw new RsqlException("Unknown property '" + selector + "' for " + this.entityClass.getSimpleName());
            }
            path = isEntity(type) ? this.joins.computeIfAbsent(fieldPath, (key) -> "j" + this.joins.size()) : fieldPath;
        }
        throw new IllegalStateException("Empty selector");
    }

    private static Field findField(Class<?> declaringType, String name) {
        if (!PROPERTY_NAME.matcher(name).matches()) {
            return null;
        }
        for (Class<?> type = declaringType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().equals(name) && !Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            }
        }
        return null;
    }

    private static boolean isEntity(Class<?> type) {
        return entityAnnotation(type) != null;
    }

    /**
     * Returns the entity name used in HQL: the {@code name} of {@code @Entity}, or the
     * simple class name.
     */
    private static String entityName(Class<?> type) {
        Annotation entity = entityAnnotation(type);
        if (entity != null) {
            try {
                String name = (String) entity.annotationType().getMethod("name").invoke(entity);
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unable to read the entity name of " + type.getName(), ex);
            }
        }
        return type.getSimpleName();
    }

    private static Annotation entityAnnotation(Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType().getName().equals(ENTITY_ANNOTATION)) {
                return annotation;
            }
        }
        return null;
    }

    private static String comparison(ComparisonOperator operator) {
        if (operator.equals(RSQLOperators.GREATER_THAN)) {
            return ">";
        }
        if (operator.equals(RSQLOperators.GREATER_THAN_OR_EQUAL)) {
            return ">=";
        }
        if (operator.equals(RSQLOperators.LESS_THAN)) {
            return "<";
        }
        if (operator.equals(RSQLOperators.LESS_THAN_OR_EQUAL)) {
            return "<=";
        }
        throw new RsqlException("Unsupported operator '" + operator + "'");
    }

    private static String likePattern(String argument) {
        StringBuilder pattern = new StringBuilder(argument.length());
        for (char c : argument.toCharArray()) {
            switch (c) {
                case '\\', '%', '_' -> pattern.append('\\').append(c);
                case '*' -> pattern.append('%');
                default -> pattern.append(c);
            }
        }
        return pattern.toString();
    }

    private record Property(String path, Class<?> type) {
    }

}
