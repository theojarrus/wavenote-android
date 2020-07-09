package com.theost.wavenote.utils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ArrayUtils {

    public static List<String> jsonToList(JSONArray json) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            try {
                list.add(json.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static<T> boolean isEmpty(List<T> list) {
        return list == null || list.isEmpty();
    }

    public static<T> boolean isNotEmpty(List<T> list) {
        return list != null && !list.isEmpty();
    }

}
