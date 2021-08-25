package au.com.muel.envconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
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
        assertEquals(Instant.EPOCH, registry.parserForType(Instant.class).get().parse("1970-01-01T00:00:00Z", null));
    }

}
