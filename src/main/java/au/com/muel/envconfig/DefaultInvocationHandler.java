package au.com.muel.envconfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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

//TODO validate against wildcard and variable types

        if (method.isDefault()) {
            // There's no nice interoperable way to handle default methods across different Java
            // versions, so it's easiest to just prevent usage.
            // https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
            final String msg = "Default methods are not supported, found: " + method;
            throw new UnsupportedOperationException(msg);
        }
        if (configType.equals(method.getDeclaringClass())) {

            return cachedResults.computeIfAbsent(method, key -> {

                // 1. resolve env var name: custom, split words
                // 2. determine if required, and therefore default values
                // 3. Value parsing (lists and maps)
                // 4. Type conversion


                // 1. resolve env var name: custom, split words
                EnvVar envVarConfig = resolveEnvVarConfig(DEFAULT_VALUES, key.getAnnotation(EnvVar.class));

                final String envVarName = resolveEnvVarName(prefix, key.getName(), envVarConfig);

                // 2. determine if required, and therefore default values
                final String envVarValue = resolveEnvVarValue(envVarConfig, envVarName, configSource);

                final ParserRegistry parserRegistry = createParserRegistry();
                final TypeConverter converter = createTypeConverter(parserRegistry);
                final Type targetType = key.getGenericReturnType();
                try {

                return parseEnvVarValue(envVarConfig, parserRegistry, converter, targetType, envVarValue);
                } catch (Exception e) {
throw new IllegalArgumentException(
                    String.format("failed to parse %s for %s (%s)", envVarValue, envVarName, e.toString()));
//                    e.printStackTrace();
                }
            });
        }

        if (Object.class.equals(method.getDeclaringClass())) {
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }
//TODO cache these
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(method.getName())) {
                return String.format("%s<Proxy:%s>", configType.getSimpleName(), prefix);
            }
         }
        try {

            return method.invoke(proxy, args);
        } catch (Throwable t) {

            System.err.println("boohoo");
            System.err.println(t.getMessage());
            return null;
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

        final String suffix;
        if (!config.envVarName().isEmpty()) {

            suffix = config.envVarName();
        } else if (config.splitWords()) {

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

        final String envVarValue = this.configSource.get(envVarName);
        if (envVarValue == null && !config.defaultValue().isEmpty()) {
            return config.defaultValue();
        }

        return envVarValue;
    }

    protected Object parseEnvVarValue(EnvVar config, ParserRegistry registry, TypeConverter typeConverter,
            Type targetType, String envVarValue) {

        for (Class<? extends ValueParser<?>> typeConverterClass : config.customParsers()) {

            try {

                @SuppressWarnings("unchecked")
                final Constructor<ValueParser<?>> noArgsConstructor = (Constructor<ValueParser<?>>) typeConverterClass.getDeclaredConstructor();
                noArgsConstructor.setAccessible(true);
                final ValueParser<?> valueParser = noArgsConstructor.newInstance();
                registry.registerCustomParsers(valueParser);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException e) {

e.printStackTrace();
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(envVarValue, e);
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
