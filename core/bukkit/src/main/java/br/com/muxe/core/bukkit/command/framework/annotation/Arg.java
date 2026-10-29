package br.com.muxe.core.bukkit.command.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {

    String NONE = "\u0000";

    String value() default "";

    String defaultValue() default NONE;
}
