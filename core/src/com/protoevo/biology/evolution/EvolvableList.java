package com.protoevo.biology.evolution;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EvolvableList {
    String name();
    String elementClassPath();
    int minSize() default 0;
    int maxSize() default Integer.MAX_VALUE;
    int initialSize() default 0;
    String[] geneDependencies() default "";
}
