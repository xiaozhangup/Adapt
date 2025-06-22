package com.volmit.adapt.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Color {
    private static final HashMap<String, List<String>> colorCache = new HashMap<>();

    public static List<String> gradientColors(String cA, String cB, int number) {
        int[] a = hex2RGB(cA);
        int[] b = hex2RGB(cB);
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            int aR = a[0];
            int aG = a[1];
            int aB = a[2];
            int bR = b[0];
            int bG = b[1];
            int bB = b[2];
            colors.add(rgb2Hex(calculateColor(aR, bR, number - 1, i), calculateColor(aG, bG, number - 1, i), calculateColor(aB, bB, number - 1, i)));
        }
        return colors;
    }

    public static List<String> gradientWhiteColors(String cA, int number) {
        var cached = colorCache.getOrDefault(cA, null);
        if (cached != null) return cached;
        var colors = gradientColors("#ffffff", cA, number);
        colorCache.put(cA, colors);
        return colors;
    }

    public static int calculateColor(int a, int b, int step, int number) {
        return a + (b - a) * number / step;
    }

    public static String color2Hex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String rgb2Hex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static int[] hex2RGB(String hexStr) {
        if (hexStr.length() == 7) {
            int[] rgb = new int[3];
            rgb[0] = Integer.parseInt(hexStr.substring(1, 3), 16);
            rgb[1] = Integer.parseInt(hexStr.substring(3, 5), 16);
            rgb[2] = Integer.parseInt(hexStr.substring(5), 16);
            return rgb;
        }
        return null;
    }
}