package io.github.matheuslealpa.rsql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converts RSQL arguments to the Java type of the property they are compared with.
 */
final class ValueConverter {

    private static final Map<Class<?>, Function<String, Object>> CONVERTERS = Map.ofEntries(
            Map.entry(String.class, (value) -> value),
            Map.entry(Integer.class, Integer::valueOf),
            Map.entry(int.class, Integer::valueOf),
            Map.entry(Long.class, Long::valueOf),
            Map.entry(long.class, Long::valueOf),
            Map.entry(Short.class, Short::valueOf),
            Map.entry(short.class, Short::valueOf),
            Map.entry(Byte.class, Byte::valueOf),
            Map.entry(byte.class, Byte::valueOf),
            Map.entry(Double.class, Double::valueOf),
            Map.entry(double.class, Double::valueOf),
            Map.entry(Float.class, Float::valueOf),
            Map.entry(float.class, Float::valueOf),
            Map.entry(BigDecimal.class, BigDecimal::new),
            Map.entry(BigInteger.class, BigInteger::new),
            Map.entry(Boolean.class, ValueConverter::toBoolean),
            Map.entry(boolean.class, ValueConverter::toBoolean),
            Map.entry(UUID.class, UUID::fromString),
            Map.entry(LocalDate.class, LocalDate::parse),
            Map.entry(LocalDateTime.class, LocalDateTime::parse),
            Map.entry(LocalTime.class, LocalTime::parse),
            Map.entry(OffsetDateTime.class, OffsetDateTime::parse),
            Map.entry(Instant.class, Instant::parse));

    private ValueConverter() {
    }

    static Object convert(String value, Class<?> type, String property) {
        if (type.isEnum()) {
            return toEnum(value, type, property);
        }
        Function<String, Object> converter = CONVERTERS.get(type);
        if (converter == null) {
            throw new RsqlException("Unsupported type " + type.getSimpleName() + " for property '" + property + "'");
        }
        try {
            return converter.apply(value);
        } catch (RuntimeException ex) {
            throw new RsqlException(
                    "Invalid value '" + value + "' for property '" + property + "' of type " + type.getSimpleName(), ex);
        }
    }

    private static Object toBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (value.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Not a boolean: " + value);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object toEnum(String value, Class<?> type, String property) {
        try {
            return Enum.valueOf((Class<? extends Enum>) type, value);
        } catch (IllegalArgumentException ex) {
            String allowed = Arrays.stream(type.getEnumConstants())
                    .map((constant) -> ((Enum<?>) constant).name())
                    .collect(Collectors.joining(", "));
            throw new RsqlException(
                    "Invalid value '" + value + "' for property '" + property + "', expected one of: " + allowed, ex);
        }
    }

}
