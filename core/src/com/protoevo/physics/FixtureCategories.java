package com.protoevo.physics;

public class FixtureCategories {
    public static final short SENSOR = 0x0001;
    public static final short PLANT = 0x0002;
    public static final short PROTOZOAN = 0x0004;
    public static final short MEAT = 0x0008;
    public static final short CELL = PLANT | PROTOZOAN | MEAT;
    public static final short WALL = 0x0010;
}
