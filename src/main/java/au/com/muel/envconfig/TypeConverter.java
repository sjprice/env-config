package au.com.muel.envconfig;

import java.lang.reflect.Type;

public interface TypeConverter {

    Object convert(Type targetType, String value);

}
