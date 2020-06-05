package com.theost.wavenote.utils;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.util.Arrays;

public class ViewUtils {

    public static void updateDropdown(Context context, AutoCompleteTextView view, String[] hints) {
        NoFilterArrayAdapter<String> adapter = new NoFilterArrayAdapter<>(context, android.R.layout.simple_list_item_1, Arrays.asList(hints));
        view.setAdapter(adapter);
        view.dismissDropDown();
    }

    public static void disbaleInput(EditText view) {
        view.setInputType(InputType.TYPE_NULL);
        view.setCursorVisible(false);
        view.setKeyListener(null);
    }

    public static void restoreFocus(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
    }

    public static void removeFocus(View view) {
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
    }

}
