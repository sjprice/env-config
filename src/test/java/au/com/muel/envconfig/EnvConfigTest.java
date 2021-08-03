package au.com.muel.envconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;


class EnvConfigTest {

    @Test
    void test() {


        assertEquals("localhost", System.getenv("TEST_HOST"));
long start = System.currentTimeMillis();
        TestConfig config = EnvConfig.fromEnv("test", TestConfig.class);
System.out.println("Duration = " + (System.currentTimeMillis() - start));
        System.out.println(config.host());
        System.out.println(config.toString());

        int port = config.port();
        System.out.println("port is: " + port);
        System.out.println("optional is: " + config.optional());

        config.counts().forEach(System.out::println);
        config.andCountsAgain().forEach(i -> System.out.println("_" + i));
        for (int i : config.moreCounts()) {
            System.out.println(".." + i);
        }

        System.out.println(config.colourMap());
        System.out.println("Really? " + config.enumTest());
    }

    @Test
    void missingEnvVars() {

        EnvConfig.fromConfigSource("EMPTY", TestConfig.class, new HashMap<>());
    }

    @Test
    void testQuickStart() throws Exception {

        final Map<String, String> configSource = ImmutableMap.of(
            "MYAPP_AUTH_SERVICE_URL", "https://auth.service.com/auth",
            "MYAPP_DB_JDBC_URL",      "jdbc:postgresql://localhost/test",
            "MYAPP_DB_POOL_SIZE",     "10",
            "MYAPP_CORS_DOMAINS",     "a.foo.com,b.foo.com,c.foo.com"
        );

        MyAppConfig config = EnvConfig.fromConfigSource("MYAPP", MyAppConfig.class, configSource);

        assertEquals(new URL("https://auth.service.com/auth"), config.authServiceUrl());
        assertEquals("jdbc:postgresql://localhost/test", config.dbJdbcUrl());
        assertEquals(10, config.dbPoolSize());
        assertEquals(Sets.newHashSet("a.foo.com", "b.foo.com", "c.foo.com"), config.corsDomains());

    }

    static interface TestConfig {

        String host();

        @EnvVar(defaultValue = "8080", customParsers = Seven.class)
        int port();

        Optional<String> optional();

        List<Integer> counts();

        @EnvVar(envVarName = "COUNTS")
        Set<Integer> andCountsAgain();

        @EnvVar(envVarName = "COUNTS")
        int[] moreCounts();

        @EnvVar(defaultValue = "7431234")
        int fooBar();

        @EnvVar(defaultValue = "DAYS:1,HOURS:2,MINUTES:3")
        Map<TimeUnit, String> colourMap();

        @EnvVar(defaultValue = "DAYS")
        TimeUnit enumTest();

    }

    static class Seven implements ValueParser<Integer> {

        @Override
        public Integer parse(String value, TypeConverter converter, Type... paramTypes) {
             return Integer.valueOf(7);
        }

    }

    static interface MyAppConfig {
        URL authServiceUrl();
        String dbJdbcUrl();
        int dbPoolSize();
        Set<String> corsDomains();
    }

}
