package com.theost.wavenote.utils;

import android.os.Build;
import android.text.Spannable;
import android.text.Spanned;

public class HtmlCompat {
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    public static String toHtml(Spannable source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.toHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.toHtml(source);
        }
    }

}
