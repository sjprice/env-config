package au.com.muel.envconfig;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;


class DefaultParserRegistryTest {

    private final DefaultParserRegistry registry = new DefaultParserRegistry();
    private final TypeConverter tc = new DefaultTypeConverter(registry);

    @Test
    void testIntegerTypes() {
        assertEquals((byte) 10, registry.parserForType(byte.class).get().parse("10", tc));
        assertEquals((byte) 10, registry.parserForType(Byte.class).get().parse("10", tc));
        assertEquals((short) 10, registry.parserForType(short.class).get().parse("10", tc));
        assertEquals((short) 10, registry.parserForType(Short.class).get().parse("10", tc));
        assertEquals(10, registry.parserForType(int.class).get().parse("10", tc));
        assertEquals(10, registry.parserForType(Integer.class).get().parse("10", tc));
        assertEquals(10L, registry.parserForType(long.class).get().parse("10", tc));
        assertEquals(10L, registry.parserForType(Long.class).get().parse("10", tc));
        assertEquals(new BigInteger("10"), registry.parserForType(BigInteger.class).get().parse("10", tc));
    }

    @ParameterizedTest
    @MethodSource
    void testIntegerTypesTooLarge(String s, Class<?> clazz) {
        assertThrows(NumberFormatException.class, () -> registry.parserForType(clazz).get().parse(s, tc));
    }

    static Stream<Arguments> testIntegerTypesTooLarge() {
        return Stream.of(
            arguments("128", byte.class),
            arguments("128", Byte.class),
            arguments("32768", short.class),
            arguments("32768", Short.class),
            arguments("2147483648", int.class),
            arguments("2147483648", Integer.class),
            arguments("9223372036854775808", long.class),
            arguments("9223372036854775808", Long.class)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIntegerTypesTooSmall(String s, Class<?> clazz) {
        assertThrows(NumberFormatException.class, () -> registry.parserForType(clazz).get().parse(s, tc));
    }

    static Stream<Arguments> testIntegerTypesTooSmall() {
        return Stream.of(
            arguments("-129", byte.class),
            arguments("-129", Byte.class),
            arguments("-32769", short.class),
            arguments("-32769", Short.class),
            arguments("-2147483649", int.class),
            arguments("-2147483649", Integer.class),
            arguments("-9223372036854775809", long.class),
            arguments("-9223372036854775809", Long.class)
        );
    }

    @Test
    void testDecimalTypes() {
        assertEquals(1.2F, registry.parserForType(float.class).get().parse("1.2", tc));
        assertEquals(1.2F, registry.parserForType(Float.class).get().parse("1.2", tc));
        assertEquals(1.2D, registry.parserForType(double.class).get().parse("1.2", tc));
        assertEquals(1.2D, registry.parserForType(Double.class).get().parse("1.2", tc));
        assertEquals(new BigDecimal("1.2"), registry.parserForType(BigDecimal.class).get().parse("1.2", tc));
    }

    @ParameterizedTest
    @ValueSource(classes = {float.class, Float.class, double.class, Double.class, BigDecimal.class})
    void testDecimalTypesEmpty(Class<?> clazz) {
        assertThrows(IllegalArgumentException.class, () -> registry.parserForType(String.class).get().parse("", tc));
    }

    @Test
    void testBooleanTypes() {
        assertTrue((Boolean) registry.parserForType(boolean.class).get().parse("true", tc));
        assertTrue((Boolean) registry.parserForType(Boolean.class).get().parse("true", tc));
        assertFalse((Boolean) registry.parserForType(boolean.class).get().parse("false", tc));
        assertFalse((Boolean) registry.parserForType(Boolean.class).get().parse("false", tc));
    }

    @Test
    void testBooleanTypesEmpty() {
        assertThrows(EnvConfigException.class, () -> registry.parserForType(boolean.class).get().parse("", tc));
        assertThrows(EnvConfigException.class, () -> registry.parserForType(Boolean.class).get().parse("", tc));
    }

    @Test
    void testBooleanTypesInvalid() {
        assertThrows(EnvConfigException.class, () -> registry.parserForType(boolean.class).get().parse("boo", tc));
        assertThrows(EnvConfigException.class, () -> registry.parserForType(Boolean.class).get().parse("boo", tc));
    }

    @Test
    void testStringType() {
        assertEquals("Hello", registry.parserForType(String.class).get().parse("Hello", tc));
        assertThrows(IllegalArgumentException.class, () -> registry.parserForType(String.class).get().parse("", tc));
    }

    @Test
    void testArrayType() {
        assertArrayEquals(new int[] {1, 2, 3, 4}, (int[]) registry.parserForType(Array.class).get().parse("1,2,3,4", tc, int.class));
        assertArrayEquals(new String[] {"foo", "bar"}, (String[]) registry.parserForType(Array.class).get().parse("foo,bar", tc, String.class));
    }

    @Test
    void testArrayEmpty() {
        assertThrows(NumberFormatException.class, () -> registry.parserForType(Array.class).get().parse("", tc, int.class));
        assertThrows(IllegalArgumentException.class, () -> registry.parserForType(Array.class).get().parse("foo,,bar", tc, String.class));
    }

    @Test
    void testArrayInvalid() {
        assertThrows(NumberFormatException.class, () -> registry.parserForType(Array.class).get().parse("abc", tc, int.class));
    }

    @Test
    void testEnumeratedType() {
        assertSame(TimeUnit.DAYS, registry.parserForType(Enum.class).get().parse("DAYS", tc, TimeUnit.class));
    }

    @Test
    void testEnumeratedTypeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> registry.parserForType(Enum.class).get().parse("", tc, TimeUnit.class));
    }

    @Test
    void testEnumeratedTypeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> registry.parserForType(Enum.class).get().parse("boo", tc, TimeUnit.class));
    }

    @Test
    void testTimeTypes() {
        assertEquals(Duration.ofHours(5), registry.parserForType(Duration.class).get().parse("PT5H", tc));
        assertEquals(Period.ofDays(5), registry.parserForType(Period.class).get().parse("P5D", tc));
        assertEquals(LocalTime.parse("23:10:28.059"), registry.parserForType(LocalTime.class).get().parse("23:10:28.059", tc));
        assertEquals(LocalDate.parse("2021-08-30"), registry.parserForType(LocalDate.class).get().parse("2021-08-30", tc));
        assertEquals(LocalDateTime.parse("2021-08-30T23:37:18.790"), registry.parserForType(LocalDateTime.class).get().parse("2021-08-30T23:37:18.790", tc));
        assertEquals(Instant.EPOCH, registry.parserForType(Instant.class).get().parse("1970-01-01T00:00:00Z", tc));
        assertEquals(ZonedDateTime.parse("2021-08-30T23:38:40.436576+08:00[Australia/Perth]"),
                registry.parserForType(ZonedDateTime.class).get().parse("2021-08-30T23:38:40.436576+08:00[Australia/Perth]", tc));
        assertEquals(OffsetTime.parse("23:39:11.899767+08:00"), registry.parserForType(OffsetTime.class).get().parse("23:39:11.899767+08:00", tc));
        assertEquals(OffsetDateTime.parse("2021-08-30T23:38:57.316746+08:00"),
                registry.parserForType(OffsetDateTime.class).get().parse("2021-08-30T23:38:57.316746+08:00", tc));
        assertEquals(ZoneId.of("Australia/Perth"), registry.parserForType(ZoneId.class).get().parse("Australia/Perth", tc));
        assertEquals(ZoneOffset.of("+08:00"), registry.parserForType(ZoneOffset.class).get().parse("+08:00", tc));
        assertEquals(MonthDay.of(2, 29), registry.parserForType(MonthDay.class).get().parse("--02-29", tc));
        assertEquals(Year.of(2021), registry.parserForType(Year.class).get().parse("2021", tc));
        assertEquals(YearMonth.of(2021, 8), registry.parserForType(YearMonth.class).get().parse("2021-08", tc));
    }

    @ParameterizedTest
    @ValueSource(classes = {
        Duration.class,
        Period.class,
        LocalTime.class,
        LocalDate.class,
        LocalDateTime.class,
        Instant.class,
        ZonedDateTime.class,
        OffsetTime.class,
        OffsetDateTime.class,
        ZoneId.class,
        ZoneOffset.class,
        MonthDay.class,
        Year.class,
        YearMonth.class
    })
    void testTimeTypesInvalid(Class<?> clazz) {
        assertThrows(DateTimeException.class, () -> registry.parserForType(clazz).get().parse("Bad", tc));
    }

    @ParameterizedTest
    @ValueSource(classes = {
        Duration.class,
        Period.class,
        LocalTime.class,
        LocalDate.class,
        LocalDateTime.class,
        Instant.class,
        ZonedDateTime.class,
        OffsetTime.class,
        OffsetDateTime.class,
        ZoneId.class,
        ZoneOffset.class,
        MonthDay.class,
        Year.class,
        YearMonth.class
    })
    void testTimeTypesEmpty(Class<?> clazz) {
        assertThrows(DateTimeException.class, () -> registry.parserForType(clazz).get().parse("", tc));
    }

}
