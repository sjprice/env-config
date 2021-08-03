package au.com.muel.envconfig;

import java.lang.reflect.Type;

@FunctionalInterface
public interface ValueParser<T> {

    T parse(String value, TypeConverter typeConverter, Type... paramTypes);

}
