package com.theost.wavenote.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeMap;

public class ChordUtils {

    public static TreeMap<Integer, ArrayList<Integer>> convertArrayTreeMap(ArrayList<String> list) {
        TreeMap<Integer, ArrayList<Integer>> copy = new TreeMap<>();
        int element;
        for (int i = 0; i < list.size(); i++) {
            try {
                element = Integer.parseInt(list.get(i));
            } catch (Exception e) {
                element = -1;
            }
            ArrayList<Integer> values = new ArrayList<>();
            if (copy.containsKey(element)) values.addAll(Objects.requireNonNull(copy.get(element)));
            values.add(i);
            copy.put(element, values);
        }
        return copy;
    }

    public static ArrayList<Integer> convertArrayDigit(ArrayList<String> list) {
        ArrayList<Integer> copy = new ArrayList<>();
        for (String s : list) {
            try {
                copy.add(Integer.parseInt(s));
            } catch (Exception ignored) {
                // skip non-digit strings
            }
        }
        return copy;
    }

    public static int getMin(ArrayList<String> shape) {
        ArrayList<Integer> copy = convertArrayDigit(shape);
        for (Integer i : copy) if (i < 0) copy.remove(i);
        return Collections.min(copy);
    }

    public static int getMax(ArrayList<String> shape) {
        ArrayList<Integer> copy = convertArrayDigit(shape);
        return Collections.max(copy);
    }

    public static String removeSpace(String line) {
        return line.replaceAll("\\s+", "");
    }

}
