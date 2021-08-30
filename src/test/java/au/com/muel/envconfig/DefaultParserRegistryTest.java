package au.com.muel.envconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


class DefaultParserRegistryTest {

    private final DefaultParserRegistry registry = new DefaultParserRegistry();

    @Test
    void testIntegerTypes() {
        assertEquals((byte) 10, registry.parserForType(byte.class).get().parse("10", null));
        assertEquals((byte) 10, registry.parserForType(Byte.class).get().parse("10", null));
        assertEquals((short) 10, registry.parserForType(short.class).get().parse("10", null));
        assertEquals((short) 10, registry.parserForType(Short.class).get().parse("10", null));
        assertEquals(10, registry.parserForType(int.class).get().parse("10", null));
        assertEquals(10, registry.parserForType(Integer.class).get().parse("10", null));
        assertEquals(10L, registry.parserForType(long.class).get().parse("10", null));
        assertEquals(10L, registry.parserForType(Long.class).get().parse("10", null));
        assertEquals(new BigInteger("10"), registry.parserForType(BigInteger.class).get().parse("10", null));
    }

    @ParameterizedTest
    @MethodSource
    void testIntegerTypesTooLarge(String s, Class<?> clazz) {
        assertThrows(NumberFormatException.class, () -> registry.parserForType(clazz).get().parse(s, null));
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
        assertThrows(NumberFormatException.class, () -> registry.parserForType(clazz).get().parse(s, null));
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
        assertEquals(1.2F, registry.parserForType(float.class).get().parse("1.2", null));
        assertEquals(1.2F, registry.parserForType(Float.class).get().parse("1.2", null));
        assertEquals(1.2D, registry.parserForType(double.class).get().parse("1.2", null));
        assertEquals(1.2D, registry.parserForType(Double.class).get().parse("1.2", null));
        assertEquals(new BigDecimal("1.2"), registry.parserForType(BigDecimal.class).get().parse("1.2", null));
    }

    @Test
    void testBooleanTypes() {
        assertTrue((Boolean) registry.parserForType(boolean.class).get().parse("true", null));
        assertTrue((Boolean) registry.parserForType(Boolean.class).get().parse("true", null));
        assertFalse((Boolean) registry.parserForType(boolean.class).get().parse("false", null));
        assertFalse((Boolean) registry.parserForType(Boolean.class).get().parse("false", null));
    }

    @Test
    void testStringType() {
        assertEquals("Hello", registry.parserForType(String.class).get().parse("Hello", null));
        assertThrows(IllegalArgumentException.class, () -> registry.parserForType(String.class).get().parse("", null));
    }

    @Test
    void testTimeTypes() {
        assertEquals(LocalTime.parse("23:10:28.059"), registry.parserForType(LocalTime.class).get().parse("23:10:28.059", null));
        assertEquals(LocalDate.parse("2021-08-30"), registry.parserForType(LocalDate.class).get().parse("2021-08-30", null));
        assertEquals(LocalDateTime.parse("2021-08-30T23:37:18.790"), registry.parserForType(LocalDateTime.class).get().parse("2021-08-30T23:37:18.790", null));
        assertEquals(Instant.EPOCH, registry.parserForType(Instant.class).get().parse("1970-01-01T00:00:00Z", null));
        assertEquals(ZonedDateTime.parse("2021-08-30T23:38:40.436576+08:00[Australia/Perth]"),
                registry.parserForType(ZonedDateTime.class).get().parse("2021-08-30T23:38:40.436576+08:00[Australia/Perth]", null));
        assertEquals(OffsetTime.parse("23:39:11.899767+08:00"), registry.parserForType(OffsetTime.class).get().parse("23:39:11.899767+08:00", null));
        assertEquals(OffsetDateTime.parse("2021-08-30T23:38:57.316746+08:00"),
                registry.parserForType(OffsetDateTime.class).get().parse("2021-08-30T23:38:57.316746+08:00", null));
    }

}
