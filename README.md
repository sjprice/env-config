# env-config

A Java library for deriving application configuration from environment variables.

It's a Java spin on the excellent Golang libraries:
* [envconfig](https://github.com/kelseyhightower/envconfig)
* [go-envconfig](https://github.com/sethvargo/go-envconfig)

# Quick start

Add Maven coordinates to the pom:

```xml
<dependency>
    <groupId>au.com.muel</groupId>
    <artifactId>env-config</artifactId>
    <version>0.8.0</version>
</dependency>
```

Create some environment variables:

```bash
export MYAPP_AUTH_SERVICE_URL=https://auth.service.com/auth
export MYAPP_DB_JDBC_URL=jdbc:postgresql://localhost/test
export MYAPP_DB_POOL_SIZE=10
export MYAPP_CORS_DOMAINS=a.foo.com,b.foo.com,c.foo.com
```

Define a configuration interface to map the environment variables to:

```java
public interface MyAppConfig {

    URL authServiceUrl();
    String dbJdbcUrl();
    int dbPoolSize();
    Set<String> corsDomains();

}
```

Create an instance of the interface:

```java
import au.com.muel.envconfig.EnvConfig;

public class QuickStart {

    public static void main(String[] args) {

        MyAppConfig config = EnvConfig.fromEnv("MYAPP", MyAppConfig.class);

        System.out.println(config.authServiceUrl());
        System.out.println(config.dbJdbcUrl());
        System.out.println(config.dbPoolSize());
        for (String corsDomain : config.corsDomains()) {
            System.out.println(corsDomain);
        }
    }

}
```

Produces the output:

```
https://auth.service.com/auth
jdbc:postgresql://localhost/test
10
a.foo.com
c.foo.com
b.foo.com
```

# Goals

`env-config` has the following design goals.

## Convention over configuration

The 

* Thread-safety and immutability

## Low overhead

`env-config` is not dependent on any other library, and the jar is less than 30kb.

All configuration proxies are effectively singletons, and attempts to recreate will instead return
the cached instance. For example:

```java
MyAppConfig config1 = EnvConfig.fromEnv("MYAPP", MyAppConfig.class);
MyAppConfig config2 = EnvConfig.fromEnv("MYAPP", MyAppConfig.class);
System.out.println(config1 == config2);
```

Prints:

```
true
```

# Type support

The following types are supported out of the box:

* `boolean`, `Boolean`
* `byte`, `Byte`, `short`, `Short`, `int`, `Integer`, `long`, `Long`, `BigInteger`
* `float`, `Float`, `double`, `Double`, `BigDecimal`
* `String`
* `URL`, `URI`
* `Duration`, `Period`
* `Instant`, `LocalTime`, `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `OffsetTime`, `OffsetDateTime`
* `ZoneId`, `ZoneOffset`
* `MonthDay`, `Year`, `YearMonth`
* `Optional`
* `List`, `Set`, `Map`
* Arrays
* Enumerated types

## Limitations

Generic types and arrays do not support nesting/multiple dimensions. 

Notable omissions - the following types aren't supported:
* `Date` - use the `java.time.*` types instead
* `Iterable`, `Collection`, `SortedSet`, `SortedMap`, `NavigableSet`, etc - configuration should be
simple. Use `List`, `Set`, `Map` instead.

# Configuration

The hope is that `env-config` supports most scenarios without requiring configuration.

In scenarios when convention can't be followed, methods can be annotated with `@EnvVar` to
customise appropriately.

## Optional environment variables

There are two ways to handle optional environment variables:

```java
public interface OptionalConfig {

    // if DEBUG_ENABLED isn't present, the value is Optional.empty()
    Optional<Boolean> debugEnabled();

    // if SERVER_PORT isn't present, the value is 8080
    @EnvVar(defaultValue = "8080")
    int serverPort();

}
```

## Environment variable names

### Namespaces

When creating a configuration instance, a namespace is used to define a common prefix amongst
environment variables (see `EnvConfig.fromEnv(String, Class)`).

```bash
export MYAPP_AUTH_SERVICE_URL=https://auth.service.com/auth
export MYAPP_DB_JDBC_URL=jdbc:postgresql://localhost/test
export MYAPP_DB_POOL_SIZE=10
```

The above environment variables are all within the namespace of `MYAPP`.

However, a namespace is purely optional (see `EnvConfig.fromEnv(Class)`). If it's omitted, then no
common prefix is required for environment variable names.

### Split words

### Arbitrary names

