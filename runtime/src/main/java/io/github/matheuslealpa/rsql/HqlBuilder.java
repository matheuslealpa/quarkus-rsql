package io.github.matheuslealpa.rsql;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import cz.jirutka.rsql.parser.ast.*;

/**
 * Builds an HQL query from an RSQL node tree. Not thread-safe; create one per filter.
 * <p>
 * Selectors are resolved against the Hibernate ORM metamodel, so only persistent attributes can be
 * filtered: transient fields and attributes annotated with {@link RsqlIgnore} are unknown.
 * <p>
 * Associations to other entities are joined with left joins, so that entities whose
 * association is {@code null} are still matched by the other conditions of an {@code or}.
 */
final class HqlBuilder implements RSQLVisitor<String, Void> {

    static final String ROOT_ALIAS = "r";

    private final EntityType<?> entityType;

    /**
     * The selectors the filter may use, or {@code null} to accept any persistent attribute.
     */
    private final Set<String> selectors;

    private final Map<String, Object> parameters = new HashMap<>();

    /**
     * Joined association paths, such as {@code r.farm}, mapped to their alias.
     */
    private final Map<String, String> joins = new LinkedHashMap<>();

    HqlBuilder(EntityType<?> entityType, Set<String> selectors) {
        this.entityType = entityType;
        this.selectors = (selectors != null) ? Set.copyOf(selectors) : null;
    }

    RsqlQuery build(Node node) {
        String restriction = (node != null) ? node.accept(this) : null;
        StringBuilder query = new StringBuilder("select ").append(ROOT_ALIAS).append(" from ")
                .append(this.entityType.getName()).append(' ').append(ROOT_ALIAS);
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
        if (this.selectors != null && !this.selectors.contains(selector)) {
            throw unknownProperty(selector);
        }
        String[] segments = selector.split("\\.", -1);
        String path = ROOT_ALIAS;
        ManagedType<?> type = this.entityType;
        for (int i = 0; i < segments.length; i++) {
            Attribute<?, ?> attribute = findAttribute(type, segments[i]);
            if (attribute == null) {
                throw unknownProperty(selector);
            }
            if (attribute.isCollection()) {
                throw new RsqlException("Collection property '" + segments[i] + "' isn't supported in '" + selector + "'");
            }
            String attributePath = path + "." + segments[i];
            if (i == segments.length - 1) {
                return new Property(attributePath, attribute.getJavaType());
            }
            path = switch (attribute.getPersistentAttributeType()) {
                case EMBEDDED -> attributePath;
                case MANY_TO_ONE, ONE_TO_ONE ->
                    this.joins.computeIfAbsent(attributePath, (key) -> "j" + this.joins.size());
                default -> throw unknownProperty(selector);
            };
            type = (ManagedType<?>) ((SingularAttribute<?, ?>) attribute).getType();
        }
        throw new IllegalStateException("Empty selector");
    }

    private RsqlException unknownProperty(String selector) {
        return new RsqlException("Unknown property '" + selector + "' for " + this.entityType.getName());
    }

    /**
     * Returns the persistent attribute with the given name, or {@code null} if there is none or
     * it is annotated with {@link RsqlIgnore}.
     */
    private static Attribute<?, ?> findAttribute(ManagedType<?> type, String name) {
        Attribute<?, ?> attribute;
        try {
            attribute = type.getAttribute(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (attribute.getJavaMember() instanceof AnnotatedElement member && member.isAnnotationPresent(RsqlIgnore.class)) {
            return null;
        }
        return attribute;
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
