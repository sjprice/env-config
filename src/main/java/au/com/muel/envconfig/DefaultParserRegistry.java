package au.com.muel.envconfig;

import static au.com.muel.envconfig.ParserUtils.fromFunction;
import static au.com.muel.envconfig.ParserUtils.genericTypeParser;
import static au.com.muel.envconfig.ParserUtils.tokenise;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class DefaultParserRegistry implements ParserRegistry {

    private static final Map<Type, Type> WRAPPER_TO_PRIMITIVES;

    static {

        final Map<Type, Type> wrapperToPrimitives = new HashMap<>();
        wrapperToPrimitives.put(Boolean.class,   boolean.class);
        wrapperToPrimitives.put(Character.class, char.class);
        wrapperToPrimitives.put(Byte.class,      byte.class);
        wrapperToPrimitives.put(Short.class,     short.class);
        wrapperToPrimitives.put(Integer.class,   int.class);
        wrapperToPrimitives.put(Long.class,      long.class);
        wrapperToPrimitives.put(Float.class,     float.class);
        wrapperToPrimitives.put(Double.class,    double.class);
        WRAPPER_TO_PRIMITIVES = Collections.unmodifiableMap(wrapperToPrimitives);

    }

    private final Map<Type, ValueParser<?>> parsers;

    DefaultParserRegistry() {
        this.parsers = new HashMap<>(64);
        registerDefaultTypes();
    }

    private final void registerParser(Type type, ValueParser<?> parser) {
        this.parsers.put(type, parser);

        final Type primitiveType = WRAPPER_TO_PRIMITIVES.get(type);
        if (primitiveType != null) {
            this.parsers.put(primitiveType, parser);
        }
    }

    private final void registerDefaultTypes() {
        registerCommonTypes();
        registerGenericTypesAndArray();
        registerEnumAndMiscTypes();
        registerJavaTimeTypes();
    }

    private final void registerCommonTypes() {

        // special parsing for boolean so that we can distinguish between a false value and an
        // invalid value
        registerParser(Boolean.class, fromFunction(s -> {
            if (Boolean.parseBoolean(s)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(s)) {
                return Boolean.FALSE;
            }
            throw new EnvConfigException("Invalid boolean, got \"" + s + "\"");
        }));

        // integer types
        registerParser(Byte.class,       fromFunction(Byte::parseByte));
        registerParser(Short.class,      fromFunction(Short::parseShort));
        registerParser(Integer.class,    fromFunction(Integer::parseInt));
        registerParser(Long.class,       fromFunction(Long::parseLong));
        registerParser(BigInteger.class, fromFunction(BigInteger::new));

        // floating point types
        registerParser(Float.class,      fromFunction(Float::parseFloat));
        registerParser(Double.class,     fromFunction(Double::parseDouble));
        registerParser(BigDecimal.class, fromFunction(BigDecimal::new));

        // string
        registerParser(String.class, fromFunction(s -> {
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Missing value, got: \"\"");
            }
            return s;
        }));
    }

    private final void registerGenericTypesAndArray() {

        registerParser(Optional.class, (s, c, types) -> {

            if (s == null || s.isEmpty()) {
                return Optional.empty();
            }

            final Object value = c.convert(types[0], s);
            return Optional.of(value);
        });

        registerParser(Set.class, genericTypeParser(toSet(),  Collections::unmodifiableSet));

        // a bit cheeky but convenient - a candidate for future rework
        final ValueParser<List<Object>> listParser = genericTypeParser(toList(), Collections::unmodifiableList);
        registerParser(List.class, listParser);
        registerParser(Array.class, (s, c, types) -> {

            final List<Object> values = listParser.parse(s, c, types);
            final Object array = Array.newInstance((Class<?>) types[0], values.size());
            for (int i=0; i<values.size(); i++) {
                Array.set(array, i, values.get(i));
            }

            return array;
        });

        registerParser(Map.class, (s, c, types) -> {

            return unmodifiableMap(tokenise(s, ',')
                .map(entry -> tokenise(entry, ':').collect(Collectors.toList()))
                .collect(Collectors.toMap(
                        l -> c.convert(types[0], l.get(0)),
                        l -> c.convert(types[1], l.get(1))
                )));
        });
    }

    private final void registerEnumAndMiscTypes() {

        registerParser(Enum.class, (s, c, types) -> {

            @SuppressWarnings({"rawtypes", "unchecked"})
            final Enum<?> e = Enum.valueOf((Class<? extends Enum>) types[0], s);
            return e;
        });

        registerParser(URL.class, fromFunction(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }));

        registerParser(URI.class, fromFunction(URI::create));
    }

    private final void registerJavaTimeTypes() {

        registerParser(Duration.class,       fromFunction(Duration::parse));
        registerParser(Period.class,         fromFunction(Period::parse));
        registerParser(Instant.class,        fromFunction(Instant::parse));
        registerParser(LocalTime.class,      fromFunction(LocalTime::parse));
        registerParser(LocalDate.class,      fromFunction(LocalDate::parse));
        registerParser(LocalDateTime.class,  fromFunction(LocalDateTime::parse));
        registerParser(ZonedDateTime.class,  fromFunction(ZonedDateTime::parse));
        registerParser(OffsetTime.class,     fromFunction(OffsetTime::parse));
        registerParser(OffsetDateTime.class, fromFunction(OffsetDateTime::parse));
        registerParser(ZoneId.class,         fromFunction(ZoneId::of));
        registerParser(ZoneOffset.class,     fromFunction(ZoneOffset::of));
        registerParser(MonthDay.class,       fromFunction(MonthDay::parse));
        registerParser(Year.class,           fromFunction(Year::parse));
        registerParser(YearMonth.class,      fromFunction(YearMonth::parse));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerCustomParsers(ValueParser<?>... parsers) {

        Method conversionMethod = ValueParser.class.getDeclaredMethods()[0];
        for (final ValueParser<?> parser : parsers) {

            try {

                Method method = parser.getClass().getMethod(conversionMethod.getName(), conversionMethod.getParameterTypes());
                Type returnType = method.getGenericReturnType();
                registerParser(returnType, parser);
            } catch (NoSuchMethodException | SecurityException e) {

                throw new EnvConfigException("Failed to find return-type of custom ValueParser: " + parser);
            }
            
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ValueParser<?>> parserForType(Type type) {
        return Optional.ofNullable(parsers.get(type));
    }

}
