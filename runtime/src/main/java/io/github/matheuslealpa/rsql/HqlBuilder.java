package io.github.matheuslealpa.rsql;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Builds an HQL restriction from an RSQL node tree. Not thread-safe; create one per filter.
 */
final class HqlBuilder implements RSQLVisitor<String, Void> {

    private static final Pattern PROPERTY_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final Class<?> entityClass;

    private final Map<String, Object> parameters = new HashMap<>();

    HqlBuilder(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    RsqlQuery build(Node node) {
        String query = node.accept(this);
        return new RsqlQuery(query, this.parameters);
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
        String property = node.getSelector();
        Class<?> type = propertyType(property);
        ComparisonOperator operator = node.getOperator();
        List<String> arguments = node.getArguments();
        if (operator.equals(RSQLOperators.IS_NULL)) {
            return property + " is null";
        }
        if (operator.equals(RSQLOperators.NOT_NULL)) {
            return property + " is not null";
        }
        if (operator.equals(RSQLOperators.IN) || operator.equals(RSQLOperators.NOT_IN)) {
            boolean in = operator.equals(RSQLOperators.IN);
            if (arguments.isEmpty()) {
                return in ? "1 = 0" : "1 = 1";
            }
            List<Object> values = new ArrayList<>(arguments.size());
            for (String argument : arguments) {
                values.add(ValueConverter.convert(argument, type, property));
            }
            return property + (in ? " in " : " not in ") + parameter(values);
        }
        String argument = arguments.get(0);
        if (operator.equals(RSQLOperators.EQUAL) || operator.equals(RSQLOperators.NOT_EQUAL)) {
            boolean equal = operator.equals(RSQLOperators.EQUAL);
            if (type == String.class && argument.indexOf('*') >= 0) {
                return property + (equal ? " like " : " not like ") + parameter(likePattern(argument))
                        + " escape '\\'";
            }
            return property + (equal ? " = " : " <> ") + parameter(ValueConverter.convert(argument, type, property));
        }
        String comparison = comparison(operator);
        return property + " " + comparison + " " + parameter(ValueConverter.convert(argument, type, property));
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

    private Class<?> propertyType(String property) {
        if (PROPERTY_NAME.matcher(property).matches()) {
            for (Class<?> type = this.entityClass; type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (field.getName().equals(property) && !Modifier.isStatic(field.getModifiers())) {
                        return field.getType();
                    }
                }
            }
        }
        throw new RsqlException("Unknown property '" + property + "' for " + this.entityClass.getSimpleName());
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

}
