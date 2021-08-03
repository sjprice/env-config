package au.com.muel.envconfig;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;


class DefaultTypeConverter implements TypeConverter {

    private final ParserRegistry parserRegistry;

    DefaultTypeConverter(ParserRegistry parserRegistry) {
        this.parserRegistry = Objects.requireNonNull(parserRegistry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convert(Type targetType, String value) {

        if (targetType instanceof Class<?>) {

            final Class<?> clazz = ((Class<?>) targetType);
            if (clazz.isArray()) {

                return parserType(Array.class).parse(value, this, clazz.getComponentType());
            }

            if (clazz.isEnum()) {

                return parserType(Enum.class).parse(value, this, clazz);
            }

            return parserType(targetType).parse(value, this);
        }

        if (targetType instanceof ParameterizedType) {

            ParameterizedType paramType = (ParameterizedType) targetType;
            final Type rawType = paramType.getRawType();
            return parserType(rawType).parse(value, this, paramType.getActualTypeArguments());
        }

        throw new UnsupportedOperationException("Unsupported type: " + targetType);
    }

    private ValueParser<?> parserType(Type targetType) {
        return parserRegistry.parserForType(targetType)
                .orElseThrow(() -> new IllegalStateException("No parser registered for: " + targetType));
    }

}
