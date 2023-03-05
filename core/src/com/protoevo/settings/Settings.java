package com.protoevo.settings;

import com.protoevo.core.Statistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Settings implements Serializable {
    public static final long serialVersionUID = 1L;

    public static class Parameter<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        private T value;
        private String name;
        private String description;
        private List<Runnable> onChange = new ArrayList<>();
        private boolean changeable = false;
        private Statistics.ComplexUnit unit = null;

        public Parameter(String name, String description, T defaultValue) {
            this.value = defaultValue;
            this.name = name;
            this.description = description;
        }

        public Parameter(String name, String description, T defaultValue, Statistics.ComplexUnit unit) {
            this.value = defaultValue;
            this.name = name;
            this.description = description;
            this.unit = unit;
        }

        public Parameter(String name, String description, T defaultValue, boolean changeable) {
            this.value = defaultValue;
            this.name = name;
            this.description = description;
            this.changeable = changeable;
        }

        public void addOnChange(Runnable runnable) {
            onChange.add(runnable);
        }

        public void removeOnChange(Runnable runnable) {
            onChange.remove(runnable);
        }

        public void set(T value) {
            this.value = value;
            onChange.forEach(Runnable::run);
        }

        public Statistics.Stat asStatistic() {
            Statistics.StatType type = Statistics.StatType.fromClass(value.getClass());
            Statistics.Stat stat = new Statistics.Stat(name, type);
            stat.setValue(value);
            stat.setUnit(unit);
            // TODO: get unit multipliers
            return stat;
        }

        public void set(String value) {
            if (this.value instanceof Integer) {
                set((T) Integer.valueOf(value));
            } else if (this.value instanceof Float) {
                set((T) Float.valueOf(value));
            } else if (this.value instanceof Boolean) {
                set((T) Boolean.valueOf(value));
            } else if (this.value instanceof Long) {
                set((T) Long.valueOf(value));
            }  else if (this.value instanceof String) {
                set((T) value);
            } else {
                throw new RuntimeException("Unknown type: " + this.value.getClass());
            }
        }

        public T get() {
            return value;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isChangeable() {
            return changeable;
        }
    }

    private final String name;

    public Settings(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
