package au.com.muel.envconfig;

import static au.com.muel.envconfig.ParserUtils.fromFunction;
import static au.com.muel.envconfig.ParserUtils.genericTypeParser;
import static au.com.muel.envconfig.ParserUtils.tokenise;
import static java.util.Collections.unmodifiableMap;
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
    private static final Map<Type, ValueParser<?>> PARSERS;

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

        final Map<Type, ValueParser<?>> parsers = new HashMap<>();

        final ValueParser<Boolean> booleanParser = fromFunction(Boolean::parseBoolean);
        parsers.put(boolean.class, booleanParser);
        parsers.put(Boolean.class, booleanParser);

        // integer types
        final ValueParser<Byte> byteParser = fromFunction(Byte::parseByte);
        parsers.put(byte.class, byteParser);
        parsers.put(Byte.class, byteParser);
        final ValueParser<Short> shortParser = fromFunction(Short::parseShort);
        parsers.put(short.class, shortParser);
        parsers.put(Short.class, shortParser);
        final ValueParser<Integer> intParser = fromFunction(Integer::parseInt);
        parsers.put(int.class, intParser);
        parsers.put(Integer.class, intParser);
        final ValueParser<Long> longParser = fromFunction(Long::parseLong);
        parsers.put(long.class, longParser);
        parsers.put(Long.class, longParser);
        parsers.put(BigInteger.class, fromFunction(BigInteger::new));

        // floating point types
        final ValueParser<Float> floatParser = fromFunction(Float::parseFloat);
        parsers.put(float.class, floatParser);
        parsers.put(Float.class, floatParser);
        final ValueParser<Double> doubleParser = fromFunction(Double::parseDouble);
        parsers.put(double.class, doubleParser);
        parsers.put(Double.class, doubleParser);
        parsers.put(BigDecimal.class, fromFunction(BigDecimal::new));
        
        // string
        parsers.put(String.class, fromFunction(s -> s));

        
        parsers.put(Optional.class, (s, c, types) -> {
            if (s == null) {
                return Optional.empty();
            }

            final Object value = c.convert(types[0], s);
            return Optional.of(value);
        });

        final ValueParser<List<Object>> listParser = genericTypeParser(Collectors.toList(), Collections::unmodifiableList);
        parsers.put(List.class, listParser);
        parsers.put(Set.class, genericTypeParser(toSet(),  Collections::unmodifiableSet));

        parsers.put(Array.class, (s, c, types) -> {

            final List<Object> values = listParser.parse(s, c, types);
            final Object array = Array.newInstance((Class<?>) types[0], values.size());
            for (int i=0; i<values.size(); i++) {
                Array.set(array, i, values.get(i));
            }

            return array;
        });

        parsers.put(Map.class, (s, c, types) -> {

            return unmodifiableMap(tokenise(s, ',')
                .map(entry -> tokenise(entry, ':').collect(Collectors.toList()))
                .collect(Collectors.toMap(
                        l -> c.convert(types[0], l.get(0)),
                        l -> c.convert(types[1], l.get(1))
                )));
        });

        parsers.put(Enum.class, (s, c, types) -> {

            @SuppressWarnings({"rawtypes", "unchecked"})
            final Enum<?> e = Enum.valueOf((Class<? extends Enum>) types[0], s);
            return e;
        });

        parsers.put(URL.class, fromFunction(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }));
        parsers.put(URI.class, fromFunction(URI::create));

        parsers.put(Duration.class,       fromFunction(Duration::parse));
        parsers.put(Period.class,         fromFunction(Period::parse));
        parsers.put(Instant.class,        fromFunction(Instant::parse));
        parsers.put(LocalTime.class,      fromFunction(LocalTime::parse));
        parsers.put(LocalDate.class,      fromFunction(LocalDate::parse));
        parsers.put(LocalDateTime.class,  fromFunction(LocalDateTime::parse));
        parsers.put(ZonedDateTime.class,  fromFunction(ZonedDateTime::parse));
        parsers.put(OffsetTime.class,     fromFunction(OffsetTime::parse));
        parsers.put(OffsetDateTime.class, fromFunction(OffsetDateTime::parse));
        parsers.put(ZoneId.class,         fromFunction(ZoneId::of));
        parsers.put(ZoneOffset.class,     fromFunction(ZoneOffset::of));
        parsers.put(MonthDay.class,       fromFunction(MonthDay::parse));
        parsers.put(Year.class,           fromFunction(Year::parse));
        parsers.put(YearMonth.class,      fromFunction(YearMonth::parse));

        PARSERS = Collections.unmodifiableMap(parsers);
    }

    private final Map<Type, ValueParser<?>> parsers;

    DefaultParserRegistry() {
        this.parsers = new HashMap<>(PARSERS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerCustomParsers(ValueParser<?>... parsers) {

        Method conversionMethod = ValueParser.class.getDeclaredMethods()[0];
        for (final ValueParser<?> parser : parsers) {

            try {

                Method method = parser.getClass().getDeclaredMethod(conversionMethod.getName(), conversionMethod.getParameterTypes());
                Type returnType = method.getGenericReturnType();
                this.parsers.put(returnType, parser);

                final Type primitiveType = WRAPPER_TO_PRIMITIVES.get(returnType);
                if (primitiveType != null) {
                    this.parsers.put(primitiveType, parser);
                }
            } catch (NoSuchMethodException | SecurityException e) {

// TODO Auto-generated catch block
e.printStackTrace();
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
