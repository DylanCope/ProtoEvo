package com.protoevo.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Settings implements Serializable {
    public static final long serialVersionUID = 1L;

    private final String name;
    private final List<Parameter<?>> parameters;

    public Settings(String name) {
        this.name = name;
        parameters = new ArrayList<>();
    }

    protected void collectParameters() {
        for (java.lang.reflect.Field field : getClass().getFields()) {
            try {
                Object value = field.get(this);
                if (value instanceof Parameter) {
                    Parameter<?> parameter = (Parameter<?>) value;
                    parameter.setFieldName(field.getName());
                    parameters.add(parameter);
                }
            } catch (IllegalAccessException ignored) {}
        }
    }

    public String getName() {
        return name;
    }

    public List<Parameter<?>> getParameters() {
        return parameters;
    }
}
