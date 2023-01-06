package com.protoevo.biology.evolution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EvolvableFloat {

    String name();
    boolean randomInitialValue() default true;
    float initValue() default 0;
    float min() default 0;
    float max() default 1;
    String[] geneDependencies() default "";
    boolean canBeRegulated() default true;
}
