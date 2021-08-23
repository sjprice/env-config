package au.com.muel.envconfig;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;


class EnvConfigTest {

    @AfterEach
    void removeCachedConfig() {
        EnvConfig.clearAll();
    }

    @Test
    void testFromEnvNoNamespace() {

        final TestConfig config = EnvConfig.fromEnv(TestConfig.class);

        assertEquals("localhost", config.testHost());
        assertEquals(8080, config.myPort());
        assertEquals(Optional.empty(), config.missingString());
        assertEquals(of(DAYS, (byte)1, HOURS, (byte)2, MINUTES, (byte)3), config.strangeTimeMap());
    }

    static interface TestConfig {

        String testHost();
        int myPort();
        Optional<String> missingString();
        Map<TimeUnit, Byte> strangeTimeMap();

    }

    @Test
    void testFromEnvQuickStart() throws Exception {

        final MyAppConfig config = EnvConfig.fromEnv("MYAPP", MyAppConfig.class);

        assertEquals(new URL("https://auth.service.com/auth"), config.authServiceUrl());
        assertEquals("jdbc:postgresql://localhost/test", config.dbJdbcUrl());
        assertEquals(10, config.dbPoolSize());
        assertEquals(Sets.newHashSet("a.foo.com", "b.foo.com", "c.foo.com"), config.corsDomains());
    }

    static interface MyAppConfig {
        URL authServiceUrl();
        String dbJdbcUrl();
        int dbPoolSize();
        Set<String> corsDomains();
    }

}
