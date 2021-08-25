# env-config

![build status](https://github.com/sjprice/env-config/actions/workflows/ci.yml/badge.svg)
![codeql status](https://github.com/sjprice/env-config/actions/workflows/codeql-analysis.yml/badge.svg)

A Java library for deriving application configuration from environment variables.

It's a Java spin on the excellent Golang libraries:
* [envconfig](https://github.com/kelseyhightower/envconfig)
* [go-envconfig](https://github.com/sethvargo/go-envconfig)

:warning: Until this library reaches v1.0.0, there may be a contract break or two. Beware!

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

If standard environment variable and Java naming conventions are followed, then configuration is
not needed.

## Thread-safety and immutability

All configuration proxies are thread-safe and immutable. All environment variable values are
immutable with the sole exception of arrays. (These are the values that are returned from config
interface methods).

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

## Usage

The majority of types are straightforward to use: an env var is parsed to become the desired type.
Eg:

Env Var | Type
------- | ----
`"true"` | `boolean`
`"3"` | `byte`
`"1970-01-01T00:00:00Z"` | `Instant`
`"PT4H10M"` | `Duration`

Array, `Set`, and `List` values are delimited by a `,` and each element is parsed according to the
target type. Eg:


Env Var | Type | Values
------- | ---- | ------
`"1,2,3,4"` | `List<Integer>` | `[1, 2, 3, 4]`
`"https://google.com,https://bing.com"` | `Set<URL>` | `[https://google.com, https://bing.com]`
`"one,two,three"` | `String[]` | `["one", "two", "three"]`
`"DAYS:3,HOURS:4"` | `Map<TimeUnit, Integer>` | `{DAYS=3, HOURS=4}`


## Limitations

Generic types and arrays do not support nesting/multiple dimensions. Delimiter escaping is not
supported yet either - so a value can't contain a delimiter.

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

