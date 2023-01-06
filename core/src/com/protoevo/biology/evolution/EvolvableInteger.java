package com.protoevo.biology.evolution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EvolvableInteger {

    enum MutationMethod {
        INCREMENT_ANY_DIR, RANDOM_SAMPLE, INCREMENT_ONLY_UP
    }

    String name();
    boolean randomInitialValue() default true;
    int initValue() default 0;
    int min() default 0;
    int max() default 1;
    MutationMethod mutateMethod() default MutationMethod.INCREMENT_ANY_DIR;
    int maxIncrement() default 1;
    String[] geneDependencies() default "";
    boolean regulate() default true;
    boolean canDisable() default false;
    int disableValue() default 0;
}
