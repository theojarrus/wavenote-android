package com.theost.wavenote.utils;

import android.content.Context;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.util.Arrays;

public class ViewUtils {

    public static void disableAutoCompleteTextView(Context context, AutoCompleteTextView view, String[] hints) {
        NoFilterArrayAdapter<String> adapter = new NoFilterArrayAdapter<>(context, android.R.layout.simple_list_item_1, Arrays.asList(hints));
        view.setInputType(InputType.TYPE_NULL);
        view.setCursorVisible(false);
        view.setKeyListener(null);
        view.setAdapter(adapter);
    }

    public static void disableEditText(EditText view) {
        view.setInputType(InputType.TYPE_NULL);
        view.setCursorVisible(false);
        view.setKeyListener(null);
    }

}
