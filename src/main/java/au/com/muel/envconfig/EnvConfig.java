package au.com.muel.envconfig;

import static java.lang.System.getenv;
import static java.lang.reflect.Proxy.newProxyInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class EnvConfig {

    private static final Map<String, Object> CONFIG_CACHE = new ConcurrentHashMap<>();


    public static <T> T fromEnv(final Class<T> configType) {
        return process(Optional.empty(), configType, getenv());
    }

    public static <T> T fromEnv(final String prefix, final Class<T> configType) {
        return process(Optional.of(prefix), configType, getenv());
    }

    public static <T> T fromConfigSource(final Class<T> configType,
            final Map<String, String> configSource) {
        return process(Optional.empty(), configType, configSource);
    }

    public static <T> T fromConfigSource(final String prefix, final Class<T> configType,
            final Map<String, String> configSource) {
        return process(Optional.of(prefix), configType, configSource);
    }

    public static void clear(final Optional<String> prefix, final Class<?> configType) {
        final String key = createCacheKey(prefix, configType);
        CONFIG_CACHE.remove(key);
    }

    public static void clearAll() {
        CONFIG_CACHE.clear();
    }

    private static <T> T process(final Optional<String> prefix, final Class<T> configType,
            final Map<String, String> configSource) {

        final String key = createCacheKey(prefix, configType);
        final Object config = CONFIG_CACHE.computeIfAbsent(key, k -> createConfig(prefix, configType, configSource));
        return configType.cast(config);
    }

    private static <T> T createConfig(final Optional<String> prefix, final Class<T> configType, Map<String, String> configSource) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?>[] types = new Class<?>[] {configType};
        InvocationHandler handler = new DefaultInvocationHandler(prefix, configType, configSource);

        T config = configType.cast(newProxyInstance(classLoader, types, handler));

        final Method[] methods = configType.getDeclaredMethods();
        final List<String> errors = new ArrayList<>(methods.length);
        for (Method m : methods) {

            if (!Modifier.isStatic(m.getModifiers())) {
                try {
    
                    m.invoke(config);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    errors.add(String.format("%s() - failed invocation: %s", m.getName(), e.getMessage()));
                } catch (InvocationTargetException e) {
                    errors.add(String.format("%s() - %s", m.getName(), e.getCause().getMessage()));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Failed to parse config with errors: " + errors);
        }

        return config;
    }

    private static String createCacheKey(final Optional<String> prefix, final Class<?> configType) {
        return prefix.map(String::toUpperCase).orElse("default") + "_" + configType.getName();
    }

}
