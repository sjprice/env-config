package au.com.muel.envconfig;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DefaultInvocationHandler implements InvocationHandler {

    private static final EnvVar DEFAULT_VALUES = annotationDefaults();

    private final char separator = '_';

    private final Optional<String> prefix;
    private final Class<?> configType;
    private final Map<String, String> configSource;
    private final Map<Method, Object> cachedResults = new HashMap<>();

    protected DefaultInvocationHandler(Optional<String> prefix, Class<?> configType, Map<String, String> configSource) {
        this.prefix = Objects.requireNonNull(prefix);
        this.configType = Objects.requireNonNull(configType);
        this.configSource = Objects.requireNonNull(configSource);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        validateMethod(method);

        if (configType.equals(method.getDeclaringClass())) {
            return cachedResults.computeIfAbsent(method, this::invokeConfigInterfaceMethod);
        }

        if (Object.class.equals(method.getDeclaringClass())) {

            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }

            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }

            if ("toString".equals(method.getName())) {
                return format("%s<Proxy:%s>", configType.getSimpleName(), prefix);
            }
        }

        throw new UnsupportedOperationException("Unsupported method invoked: " + method);
    }

    // TODO consider moving out of this class and into EnvConfig
    private void validateMethod(Method method) {

        if (method.getParameterCount() > 0) {
            throw new IllegalArgumentException("Methods with arguments are not allowed: " + method);
        }

        if (method.isDefault()) {
            // There's no nice interoperable way to handle default methods across different Java
            // versions, so it's easiest to just prevent usage.
            // https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
            final String msg = "Default methods are not supported, found: " + method;
            throw new UnsupportedOperationException(msg);
        }

        final Type returnType = method.getGenericReturnType();
        if (returnType instanceof TypeVariable<?>) {
            throw new IllegalArgumentException("Type variable return types are not allowed: " + method);
        }

        if (returnType instanceof WildcardType) {
            throw new IllegalArgumentException("Wildcard return types are not allowed: " + method);
        }

        if (returnType instanceof GenericArrayType) {
            throw new IllegalArgumentException("Generic array return types are not allowed: " + method);
        }
     }

    private final Object invokeConfigInterfaceMethod(Method method) {

        final EnvVar envVarConfig = resolveEnvVarConfig(DEFAULT_VALUES, method.getAnnotation(EnvVar.class));

        final String envVarName = resolveEnvVarName(prefix, method.getName(), envVarConfig);

        final String envVarValue = resolveEnvVarValue(envVarConfig, envVarName, configSource);

        final ParserRegistry parserRegistry = createParserRegistry();
        final TypeConverter converter = createTypeConverter(parserRegistry);
        final Type targetType = method.getGenericReturnType();
        try {

            return parseEnvVarValue(envVarConfig, parserRegistry, converter, targetType, envVarValue);
        } catch (EnvConfigException e) {

            throw e;
        } catch (RuntimeException e) {

            final String msg = format("failed to parse \"%s\" for %s (%s)", envVarValue, envVarName, e.toString());
            throw new EnvConfigException(msg);
        }
    }

    protected ParserRegistry createParserRegistry() {
        return new DefaultParserRegistry();
    }

    protected TypeConverter createTypeConverter(ParserRegistry parserRegistry) {
        return new DefaultTypeConverter(parserRegistry);
    }

    protected EnvVar resolveEnvVarConfig(EnvVar defaultConfig, EnvVar userDefinedConfig) {
        return userDefinedConfig == null ? defaultConfig : userDefinedConfig;
    }

    protected String resolveEnvVarName(Optional<String> prefix, String methodName, EnvVar config) {

        if (!config.envVarName().isEmpty()) {
            return config.envVarName();
        }

        final String suffix;
        if (config.splitWords()) {

            final Matcher matcher = Pattern.compile("([a-z][A-Z])").matcher(methodName);
            final StringBuilder builder = new StringBuilder(methodName.length() + 8);
            int start = 0;
            while (matcher.find()) {

                final int middle = matcher.start()+1;
                builder.append(methodName.substring(start, middle));
                builder.append(separator);
                builder.append(methodName.substring(middle, matcher.end()));
                start = matcher.end();
            }

            suffix = builder.append(methodName.substring(start)).toString();
        } else {
            suffix = methodName;
        }

        return prefix.map(p -> p + separator + suffix).orElse(suffix).toUpperCase();
    }

    protected String resolveEnvVarValue(EnvVar config, String envVarName, Map<String, String> configSource) {

        final String envVarValue = ofNullable(configSource.get(envVarName)).orElse("");
        if (envVarValue.isEmpty() && !config.defaultValue().isEmpty()) {
            return config.defaultValue();
        }

        return envVarValue;
    }

    protected Object parseEnvVarValue(EnvVar config, ParserRegistry registry, TypeConverter typeConverter,
            Type targetType, String envVarValue) {

        for (Class<? extends ValueParser<?>> valueParserClass : config.customParsers()) {

            try {

                @SuppressWarnings("unchecked")
                final Constructor<ValueParser<?>> noArgsConstructor =
                    (Constructor<ValueParser<?>>) valueParserClass.getDeclaredConstructor();
                noArgsConstructor.setAccessible(true);

                registry.registerCustomParsers(noArgsConstructor.newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException e) {

                final String msg  = format("Custom ValueParser could not be instantiated: %s (%s)", valueParserClass, e);
                throw new EnvConfigException(msg);
            } catch (NoSuchMethodException e) {

                final String msg = format("Custom ValueParser is missing a no-args construction: %s", valueParserClass);
                throw new EnvConfigException(msg);
            }
        }

        return typeConverter.convert(targetType, envVarValue);
    }

    @EnvVar
    static final EnvVar annotationDefaults() {

        try {

            Method thisMethod = DefaultInvocationHandler.class.getDeclaredMethod("annotationDefaults");
            return thisMethod.getAnnotation(EnvVar.class);
        } catch (NoSuchMethodException | SecurityException e) {

            throw new IllegalStateException("Can't retrieve EnvVar from annotationDefaults()", e);
        }
    }

}
