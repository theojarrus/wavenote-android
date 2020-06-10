package com.theost.wavenote.utils;

import android.content.Context;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;
import com.theost.wavenote.R;

public class DictionaryUtils {

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

}