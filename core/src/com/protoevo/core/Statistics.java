package com.protoevo.core;


import com.badlogic.gdx.Gdx;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Statistics implements Serializable, Iterable<Statistics.Stat> {
    
    private static final long serialVersionUID = 1L;

    public enum StatType {
        BOOLEAN(Boolean.class),
        INTEGER(Integer.class),
        FLOAT(Float.class),
        STRING(String.class);

        final Class<?> clazz;

        StatType(Class<?> clazz) {
            this.clazz = clazz;
        }
    }

    public enum BaseUnit {
        COUNT(""),
        PERCENTAGE("%"),
        ANGLE("°"),
        MASS("g"),
        ENERGY("J"),
        TEMPERATURE("K"),
        DISTANCE("m"),
        TIME("s");

        private final String symbol;

        BaseUnit(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static int getMinimumOrderOfMagnitude() {
            return -18;
        }

        public static int getMaximumOrderOfMagnitude() {
            return 18;
        }

        public static String getPrefix(int orderOfMagnitude) {
            if (orderOfMagnitude <= -24)
                return "y";
            else if (orderOfMagnitude <= -21)
                return "z";
            else if (orderOfMagnitude <= -18)
                return "a";
            else if (orderOfMagnitude <= -15)
                return "f";
            else if (orderOfMagnitude <= -12)
                return "p";
            else if (orderOfMagnitude <= -9)
                return "n";
            else if (orderOfMagnitude <= -6)
                return "µ";
            else if (orderOfMagnitude <= -3)
                return "m";
            else if (orderOfMagnitude < 3)
                return "";
            else if (orderOfMagnitude < 6)
                return "k";
            else if (orderOfMagnitude < 9)
                return "M";
            else if (orderOfMagnitude < 12)
                return "G";
            else if (orderOfMagnitude < 15)
                return "T";
            else if (orderOfMagnitude < 18)
                return "P";
            else if (orderOfMagnitude < 21)
                return "E";
            else if (orderOfMagnitude < 24)
                return "Z";
            else
                return "Y";
        }
    }

    public static class ComplexUnit implements Serializable {

        
        private static final long serialVersionUID = 1L;

        public static final ComplexUnit COUNT = new ComplexUnit(BaseUnit.COUNT);
        public static final ComplexUnit PERCENTAGE = new ComplexUnit(BaseUnit.PERCENTAGE);
        public static final ComplexUnit PERCENTAGE_PER_TIME = new ComplexUnit(BaseUnit.PERCENTAGE).divide(BaseUnit.TIME);
        public static final ComplexUnit ANGLE = new ComplexUnit(BaseUnit.ANGLE);
        public static final ComplexUnit DISTANCE = new ComplexUnit(BaseUnit.DISTANCE);
        public static final ComplexUnit AREA = new ComplexUnit(BaseUnit.DISTANCE, 2);
        public static final ComplexUnit VOLUME = new ComplexUnit(BaseUnit.DISTANCE, 3);
        public static final ComplexUnit TIME = new ComplexUnit(BaseUnit.TIME);
        public static final ComplexUnit FREQUENCY = new ComplexUnit(BaseUnit.TIME, -1);
        public static final ComplexUnit MASS = new ComplexUnit(BaseUnit.MASS);
        public static final ComplexUnit MASS_DENSITY = new ComplexUnit(BaseUnit.MASS).divide(AREA);
        public static final ComplexUnit MASS_PER_TIME = new ComplexUnit(BaseUnit.MASS).divide(BaseUnit.TIME);
        public static final ComplexUnit ENERGY = new ComplexUnit(BaseUnit.ENERGY);
        public static final ComplexUnit TEMPERATURE = new ComplexUnit(BaseUnit.TEMPERATURE);
        public static final ComplexUnit SPEED = new ComplexUnit(BaseUnit.DISTANCE).divide(BaseUnit.TIME);
        public static final ComplexUnit ACCELERATION = new ComplexUnit(BaseUnit.DISTANCE).divide(BaseUnit.TIME, 2);
        public static final ComplexUnit FORCE = new ComplexUnit(BaseUnit.MASS).multiply(ACCELERATION);
        public static final ComplexUnit PRESSURE = new ComplexUnit(BaseUnit.ENERGY).divide(AREA);
        public static final ComplexUnit POWER = new ComplexUnit(BaseUnit.ENERGY).divide(BaseUnit.TIME);
        public static final ComplexUnit IMPULSE = new ComplexUnit(BaseUnit.MASS).multiply(SPEED);
        public static final ComplexUnit MOMENTUM = new ComplexUnit(BaseUnit.MASS).multiply(SPEED);
        public static final ComplexUnit TORQUE = new ComplexUnit(FORCE).multiply(DISTANCE);

        private final Map<BaseUnit, Integer> units = new TreeMap<>();

        public ComplexUnit() {}

        public ComplexUnit(BaseUnit unit) {
            units.put(unit, 1);
        }

        public ComplexUnit(ComplexUnit unit) {
            units.putAll(unit.units);
        }

        public ComplexUnit(BaseUnit unit, int exponent) {
            units.put(unit, exponent);
        }

        public int getExponent(BaseUnit unit) {
            return units.getOrDefault(unit, 0);
        }

        public ComplexUnit multiply(BaseUnit unit) {
            return multiply(unit, 1);
        }

        public ComplexUnit multiply(BaseUnit unit, int exponent) {
            units.put(unit, units.getOrDefault(unit, 0) + exponent);
            return this;
        }

        public ComplexUnit divide(BaseUnit unit) {
            return divide(unit, 1);
        }

        public ComplexUnit divide(BaseUnit unit, int exponent) {
            units.put(unit, units.getOrDefault(unit, 0) - exponent);
            if (units.get(unit) == 0) {
                units.remove(unit);
            }
            return this;
        }

        public ComplexUnit multiply(ComplexUnit unit) {
            for (Map.Entry<BaseUnit, Integer> entry : unit.units.entrySet()) {
                multiply(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public ComplexUnit divide(ComplexUnit unit) {
            for (Map.Entry<BaseUnit, Integer> entry : unit.units.entrySet()) {
                divide(entry.getKey(), entry.getValue());
            }
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            int entries = 0;
            boolean negative = false;
            int nEntries = units.size();
            boolean openedParens = false;
            Iterator<BaseUnit> unitsIterator = units.keySet().stream()
                    .sorted((u1, u2) -> Integer.compare(getExponent(u2), getExponent(u1)))
                    .iterator();
            if (!unitsIterator.hasNext())
                return "";

            while (unitsIterator.hasNext()) {
                BaseUnit unit = unitsIterator.next();
                int exp = units.get(unit);

                if (exp == 0) {
                    continue;
                }
                if (exp < 0 && !negative) {
                    negative = true;
                    if (entries == 0)
                        sb.append("1");
                    sb.append("/");
                    if ((nEntries - entries) > 1) {
                        sb.append("(");
                        openedParens = true;
                    }
                }

                if (Math.abs(exp) == 1)
                    sb.append(unit.getSymbol());
                else
                    sb.append(unit.getSymbol()).append("^").append(Math.abs(exp));

                entries++;
            }
            if (openedParens) {
                sb.append(")");
            }
            return sb.toString();
        }
    }

    public static class Stat implements Serializable {
        
        private static final long serialVersionUID = 1L;

        private final String name;
        private final StatType type;
        private Object value;
        private float error;
        private ComplexUnit unit;
        private Map<BaseUnit, Float> unitMultipliers;

        public Stat(String name, StatType type) {
            this.name = name;
            this.type = type;
        }

        public Stat(Stat stat) {
            this(stat.name, stat);
        }

        public Stat(String name, Stat stat) {
            this.name = name;
            this.type = stat.type;
            this.value = stat.value;
            this.unit = stat.unit;
            this.unitMultipliers = stat.unitMultipliers;
        }

        public void setValue(Object value) {
            if (value.getClass() != type.clazz) {
                throw new IllegalArgumentException(
                        "Invalid type for stat " + name + ": "
                        + value.getClass().getSimpleName());
            }
            this.value = value;
        }

        public void setUnit(ComplexUnit unit) {
            this.unit = unit;
        }

        public void set(Object value, ComplexUnit unit) {
            setValue(value);
            setUnit(unit);
        }

        public Object getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public ComplexUnit getUnit() {
            return unit;
        }

        public void setUnitMultipliers(Map<BaseUnit, Float> unitMultipliers) {
            this.unitMultipliers = unitMultipliers;
        }

        public int getStandardExponent(float number) {
            if (number == 0) {
                return 0;
            }
            int exp = (int) Math.log10(Math.abs(number));
            if (exp >= 0)
                return exp;
            return exp - 1;
        }

        public float getStandardMantissa(float number) {
            int exp = getStandardExponent(number);
            return number / (float) Math.pow(1000, exp / 3);
        }

        public boolean canBeNumeric() {
            return isNumeric() || type.equals(StatType.BOOLEAN);
        }

        public float getFloat() {
            if (type.equals(StatType.BOOLEAN))
                return ((Boolean) value) ? 1 : 0;

            return ((Number) value).floatValue();
        }

        public float getMultipliedFloat() {
            float number = getFloat();
            if (unit != null) {
                for (Map.Entry<BaseUnit, Float> entry : unitMultipliers.entrySet()) {
                    if (unit.getExponent(entry.getKey()) != 0)
                        number *= Math.pow(entry.getValue(),
                                unit.getExponent(entry.getKey()));
                }
            }
            return number;
        }

        public boolean isNumeric() {
            return type.equals(StatType.FLOAT) || type.equals(StatType.INTEGER);
        }

        public Stat add(Number n) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) + n.intValue();
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) + n.floatValue();
            }
            return this;
        }

        public Stat add(Stat stat) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) + ((Integer) stat.value);
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) + ((Float) stat.value);
            }
            return this;
        }

        public Stat sub(Number n) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) - n.intValue();
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) - n.floatValue();
            }
            return this;
        }

        public Stat sub(Stat stat) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) - ((Integer) stat.value);
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) - ((Float) stat.value);
            }
            return this;
        }

        public Stat mul(Number n) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) * n.intValue();
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) * n.floatValue();
            }
            return this;
        }

        public Stat mul(Stat stat) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) * ((Integer) stat.value);
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) * ((Float) stat.value);
            }
            return this;
        }

        public Stat div(Number n) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) / n.intValue();
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) / n.floatValue();
            }
            return this;
        }

        public Stat div(Stat stat) {
            if (type.equals(StatType.INTEGER)) {
                value = ((Integer) value) / ((Integer) stat.value);
            } else if (type.equals(StatType.FLOAT)) {
                value = ((Float) value) / ((Float) stat.value);
            }
            return this;
        }

        public String getValueString() {
            if (isNumeric()) {
                float number = getMultipliedFloat();
                float mantissa = getStandardMantissa(number);
                int exp = getStandardExponent(number);

                if (exp <= BaseUnit.getMinimumOrderOfMagnitude())
                    return "0" + (unit != null ? unit.toString() : "");

                String unitStr = BaseUnit.getPrefix(exp);
                unitStr += unit != null ? unit.toString() : "";

                String valueStr = String.format("%.2f", mantissa);
                if (valueStr.endsWith(".00"))
                    valueStr = valueStr.substring(0, valueStr.length() - 3);

                if (error > 0) {
                    String errorStr = String.format("%.2f", error);
                    if (errorStr.endsWith(".00"))
                        errorStr = errorStr.substring(0, errorStr.length() - 3);
                    return valueStr + " ± " + errorStr + " " + unitStr;
                }

                if (unitStr.length() > 1)
                    return valueStr + " " + unitStr;
                else
                    return valueStr + unitStr;
            }
            return "" + value;
        }

        @Override
        public String toString() {
            return name + ": " + getValueString();
        }
    }

    private final Map<String, Stat> stats = new ConcurrentHashMap<>();
    private final Map<BaseUnit, Float> unitMultipliers = new HashMap<>();

    public Statistics() {
        setUnitMultiplier(BaseUnit.DISTANCE, 1e-6f);
        setUnitMultiplier(BaseUnit.MASS, 1e-3f);
    }

    public void setUnitMultiplier(BaseUnit unit, float multiplier) {
        unitMultipliers.put(unit, multiplier);
    }

    public Stat put(String name, StatType type, Comparable<?> value) {
        Stat stat = stats.containsKey(name) ? stats.get(name) : new Stat(name, type);
        stat.setValue(value);
        stat.setUnitMultipliers(unitMultipliers);
        stats.put(name, stat);
        return stat;
    }

    public void put(Stat stat) {
        stats.put(stat.getName(), stat);
    }

    public Stat put(String name, float value) {
        return put(name, StatType.FLOAT, value);
    }

    public Stat put(String name, int value) {
        return put(name, StatType.INTEGER, value);
    }

    public Stat put(String name, boolean value) {
        return put(name, StatType.BOOLEAN, value);
    }

    public Stat put(String name, String value) {
        return put(name, StatType.STRING, value);
    }

    public Stat putCount(String name, int count) {
        Stat stat = put(name, StatType.INTEGER, count);
        stat.setUnit(ComplexUnit.COUNT);
        return stat;
    }

    public Stat put(String name, float value, ComplexUnit unit) {
        Stat stat = put(name, StatType.FLOAT, value);
        stat.setUnit(unit);
        return stat;
    }

    public Stat putRadian(String name, float value) {
        return put(name, (float) Math.toDegrees(value), ComplexUnit.ANGLE);
    }

    public Stat putPercentage(String name, float percentage) {
        return put(name, percentage, ComplexUnit.PERCENTAGE);
    }

    public Stat putDistance(String name, float distance) {
        return put(name, distance, ComplexUnit.DISTANCE);
    }

    public Stat putTime(String name, float time) {
        return put(name, time, ComplexUnit.TIME);
    }

    public Stat putRate(String name, float rate) {
        return put(name, rate, ComplexUnit.FREQUENCY);
    }

    public Stat putVolume(String name, float volume) {
        return put(name, volume, ComplexUnit.VOLUME);
    }

    public Stat putMass(String name, float mass) {
        return put(name, mass, ComplexUnit.MASS);
    }

    public Stat putMassDensity(String name, float mass) {
        return put(name, mass, ComplexUnit.MASS_DENSITY);
    }

    public Stat putArea(String name, float area) {
        return put(name, area, ComplexUnit.AREA);
    }

    public Stat putEnergy(String name, float energy) {
        return put(name, energy, ComplexUnit.ENERGY);
    }

    public Stat putSpeed(String name, float speed) {
        return put(name, speed, ComplexUnit.SPEED);
    }

    public Stat putAcceleration(String name, float acceleration) {
        return put(name, acceleration, ComplexUnit.ACCELERATION);
    }

    public Stat putForce(String name, float force) {
        return put(name, force, ComplexUnit.FORCE);
    }

    public Stat putBoolean(String name, boolean value) {
        return put(name, StatType.BOOLEAN, value);
    }

    public Stat putString(String name, String value) {
        return put(name, StatType.STRING, value);
    }

    public Collection<Stat> getStats() {
        return stats.values();
    }

    public Map<String, Stat> getStatsMap() {
        return stats;
    }

    public void clear() {
        stats.clear();
    }

    @Override
    public Iterator<Stat> iterator() {
        return getStats().iterator();
    }

    public void putAll(Statistics other) {
        for (Stat stat : other)
            put(new Stat(stat));
    }

    public static Statistics computeSummaryStatistics(Iterator<Statistics> iterator) {
        return computeSummaryStatistics(iterator, false);
    }

    public static Statistics computeSummaryStatistics(Iterator<Statistics> iterator, boolean computeLogSummaries) {
        Statistics summaryStats = new Statistics();
        Set<String> statNames = new HashSet<>();

        // Implementation of Welford's algorithm for computing mean and variance
        // https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
        while (iterator.hasNext()) {
            Statistics stats = iterator.next();
            for (Stat stat : stats) {
                if (stat.canBeNumeric()) {
                    if (!statNames.contains(stat.name)) {
                        summaryStats.put(stat.name + " Mean", 0f, stat.unit);
                        summaryStats.putCount(stat.name + " Count",0);
                        if (computeLogSummaries)
                            summaryStats.put(stat.name + " Log Mean", 0f);
                    }
                    statNames.add(stat.name);

                    summaryStats.stats.get(stat.name + " Count").add(1);
                    Stat meanStat = summaryStats.stats.get(stat.name + " Mean");

                    float newValue = stat.getFloat();
                    float mean = summaryStats.stats.get(stat.name + " Mean").getFloat();
                    float delta = newValue - mean;
                    int count = (int) summaryStats.stats.get(stat.name + " Count").getFloat();
                    mean += delta / count;
                    float delta2 = newValue - mean;

                    meanStat.setValue(mean);
                    meanStat.error += delta * delta2;

                    if (computeLogSummaries) {
                        float logValue = (float) Math.log(newValue);
                        Stat logMeanStat = summaryStats.stats.get(stat.name + " Log Mean");
                        float logMean = logMeanStat.getFloat();
                        float logDelta = logValue - logMean;
                        logMean += logDelta / count;
                        float logDelta2 = logValue - logMean;

                        logMeanStat.setValue(logMean);
                        logMeanStat.error += logDelta * logDelta2;
                    }
                }
            }
        }

        for (String name : statNames) {
            int count = (int) summaryStats.stats.get(name + " Count").getFloat();
            Stat meanStat = summaryStats.stats.get(name + " Mean");
            if (count > 1) {
                meanStat.error = (float) Math.sqrt(meanStat.error / count);
                if (computeLogSummaries) {
                    Stat logMeanStat = summaryStats.stats.get(name + " Log Mean");
                    logMeanStat.error = (float) Math.sqrt(logMeanStat.error / count);
                }
            } else {
                meanStat.error = 0;
                if (computeLogSummaries) {
                    Stat logMeanStat = summaryStats.stats.get(name + " Log Mean");
                    logMeanStat.error = 0;
                }
            }
        }

        return summaryStats;
    }
}
