package com.protoevo.biology.evolution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EvolvableObject {
    String name();
    String traitClass();
    String[] dependencies() default "";
}
