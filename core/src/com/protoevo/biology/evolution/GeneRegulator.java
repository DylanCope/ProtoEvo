package com.protoevo.biology.evolution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneRegulator {
    String name();
    float min() default 0f;
    float max() default 1f;
}
