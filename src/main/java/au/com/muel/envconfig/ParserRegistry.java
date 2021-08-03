package au.com.muel.envconfig;

import java.lang.reflect.Type;
import java.util.Optional;

public interface ParserRegistry {

    void registerCustomParsers(ValueParser<?>... converters);

    Optional<ValueParser<?>> parserForType(Type type);

}
