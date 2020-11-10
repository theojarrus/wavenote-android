package com.theost.wavenote.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import androidx.core.content.ContextCompat;

import com.theost.wavenote.R;

import java.lang.reflect.Field;
import java.util.Locale;

public class ResUtils {

    public static String[] getKeywordTypes(Context context) {
        return context.getResources().getStringArray(R.array.keyword_types);
    }

    public static int[] getDialogColors(Context context) {
        int colorDisabled = ContextCompat.getColor(context, R.color.gray_20);
        int colorEnabled = ThemeUtils.getColorFromAttribute(context, R.attr.colorAccent);
        return new int[]{colorDisabled, colorEnabled};
    }

    public static int getKeywordMaxLength(Context context) {
        return context.getResources().getInteger(R.integer.dialog_input_max);
    }

    public static void restoreDictionary(Context context) {
        DatabaseHelper database = new DatabaseHelper(context);
        String[] keywordTypes = ResUtils.getKeywordTypes(context);
        String[] resourceTitles = context.getResources().getStringArray(R.array.array_musical_titles);
        String[] resourceWords = context.getResources().getStringArray(R.array.array_musical_words);

        database.removeDictionaryData(DatabaseHelper.COL_0);
        for (String j : resourceTitles) database.insertDictionaryData(j, keywordTypes[0]);
        for (String i : resourceWords) database.insertDictionaryData(i, keywordTypes[1]);
    }

    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static String getResStringLanguage(Context context, int id, String lang) {
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;
        Configuration confAr = context.getResources().getConfiguration();
        confAr.locale = new Locale(lang);
        DisplayMetrics metrics = new DisplayMetrics();
        Resources resources = new Resources(context.getAssets(), metrics, confAr);
        String string = resources.getString(id);
        conf.locale = savedLocale;
        res.updateConfiguration(conf, null);
        return string;
    }

}