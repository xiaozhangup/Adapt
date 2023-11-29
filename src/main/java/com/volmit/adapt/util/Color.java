package com.volmit.adapt.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Color {
    private static HashMap<String, List<String>> colorCache = new HashMap<>();

    public static String modifiedColorCode(String colorCode, float ratio) {
        String colorStr = colorCode.replaceFirst("#", "");

        int red = Integer.parseInt(colorStr.substring(0, 2), 16);
        int green = Integer.parseInt(colorStr.substring(2, 4), 16);
        int blue = Integer.parseInt(colorStr.substring(4, 6), 16);

        int newRed = (int) (red * ratio);
        int newGreen = (int) (green * ratio);
        int newBlue = (int) (blue * ratio);

        int finalRed = Math.min(newRed + 30, 255);
        int finalGreen = Math.min(newGreen + 30, 255);
        int finalBlue = Math.min(newBlue + 30, 255);

        return String.format("#%02x%02x%02x", finalRed, finalGreen, finalBlue);
    }

    public static String whiteColorCode(String colorCode) {
        return gradientColors(colorCode, "#ffffff", 10).get(8);
    }

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