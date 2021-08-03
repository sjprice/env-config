package au.com.muel.envconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnvVar {

    boolean splitWords() default true;

    String defaultValue() default "";

    String envVarName() default "";

    Class<? extends ValueParser<?>>[] customParsers() default {};

}
