package com.volmit.adapt.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListC {
    public static List<List<String>> convert(String[][] array) {
        List<List<String>> result = new ArrayList<>();

        for (String[] innerArray : array) {
            List<String> innerList = new ArrayList<>();
            Collections.addAll(innerList, innerArray);
            result.add(innerList);
        }

        return result;
    }
}
