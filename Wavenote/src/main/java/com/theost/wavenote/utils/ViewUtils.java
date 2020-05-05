package com.theost.wavenote.utils;

import android.content.Context;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

public class ViewUtils {

    public static void disableAutoCompleteTextView(Context context, AutoCompleteTextView view, String defaultText, String[] hints) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, hints);
        view.setInputType(InputType.TYPE_NULL);
        view.setText(defaultText);
        view.setCursorVisible(false);
        view.setKeyListener(null);
        view.setAdapter(adapter);
    }

}
