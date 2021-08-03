package au.com.muel.envconfig;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.stream.IntStream.concat;
import static java.util.stream.IntStream.of;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

public final class ParserUtils {

    private static final int TOKEN_BUFFER_SIZE = parseInt(getProperty("envconfig.token.buffersize", "1024"));

    private ParserUtils() {
        throw new UnsupportedOperationException();
    }


    public static <T> ValueParser<T> fromFunction(final Function<String, ? extends T> function) {
        return (value, converters, paramTypes) -> function.apply(value);
    }

    public static Stream<String> tokenise(String value, int splittingChar) {

        final int[] buffer = new int[TOKEN_BUFFER_SIZE];
        final AtomicInteger index = new AtomicInteger();

        return concat(value.codePoints(), of(splittingChar)).boxed().flatMap(i -> {

            final int codePoint = i.intValue();
            if (codePoint == splittingChar) {

                final String token = new String(buffer, 0, index.get());
                index.set(0);
                return Stream.of(token);
            }

            buffer[index.getAndIncrement()] = codePoint;

            return Stream.empty();
        });
    }

    public static <T extends Collection<?>> ValueParser<T> genericTypeParser(Collector<Object, ?, T> collector,
            Function<T, T> immutabilityFunction) {

        return (value, parsers, types) -> immutabilityFunction.apply(tokenise(value, ',')
                    .map(token -> parsers.convert(types[0], token))
                    .collect(collector));
    }

}
