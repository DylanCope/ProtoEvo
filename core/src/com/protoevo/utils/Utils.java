package com.protoevo.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Utils {

    public static long randomLong() {
        return UUID.randomUUID().getMostSignificantBits();
    }

    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> argsMap = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--"))
                argsMap.put(arg.replace("--", ""), "true");
            else if (arg.startsWith("-")) {
                String[] split = arg.split("=");
                if (split.length == 2 && !split[1].equals(""))
                    argsMap.put(split[0].replace("-", ""), split[1]);
                else
                    System.out.println("Ignoring invalid argument: " + arg);
            }
        }
        return argsMap;
    }


    public static String getTimeStampString() {
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
    }

    public static String numberToString(float d, int dp) {
        if (Float.isNaN(d))
            return "NaN";

        float ten = (float) Math.pow(10, dp);
        float v = ((int) (d * ten)) / ten;
        if ((int) v == v)
            return Integer.toString((int) v);
        else
            return Float.toString(v);
    }

    public static double getTimeSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    public static String superscript(String str) {
        str = str.replaceAll("0", "⁰");
        str = str.replaceAll("1", "¹");
        str = str.replaceAll("2", "²");
        str = str.replaceAll("3", "³");
        str = str.replaceAll("4", "⁴");
        str = str.replaceAll("5", "⁵");
        str = str.replaceAll("6", "⁶");
        str = str.replaceAll("7", "⁷");
        str = str.replaceAll("8", "⁸");
        str = str.replaceAll("9", "⁹");
        return str;
    }

    public static String subscript(String str) {
        str = str.replaceAll("0", "₀");
        str = str.replaceAll("1", "₁");
        str = str.replaceAll("2", "₂");
        str = str.replaceAll("3", "₃");
        str = str.replaceAll("4", "₄");
        str = str.replaceAll("5", "₅");
        str = str.replaceAll("6", "₆");
        str = str.replaceAll("7", "₇");
        str = str.replaceAll("8", "₈");
        str = str.replaceAll("9", "₉");
        return str;
    }

    public static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
