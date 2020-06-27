package com.theost.wavenote.utils;

import android.content.Context;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;
import com.theost.wavenote.R;

public class ResUtils {

    public static String[] getKeywordTypes(Context context) {
        return context.getResources().getStringArray(R.array.keyword_types);
    }

    public static int[] getDialogColors(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        int colorDisabled = ContextCompat.getColor(context, R.color.gray_20);
        int colorEnabled = ContextCompat.getColor(context, typedValue.resourceId);
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

}