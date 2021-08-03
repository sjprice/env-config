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
naming

* Thread-safety and immutability

## Low overhead

Not dependent on any other library. All configuration proxies are effectively singletons, 

# Type support

## Limitations

NavigableSet, nested generic types

# Configuration

The hope is that `env-config` supports most scenarios without requiring configuration.




In scenario

