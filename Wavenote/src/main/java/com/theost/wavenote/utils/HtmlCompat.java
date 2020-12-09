package com.theost.wavenote.utils;

import android.text.Spannable;
import android.text.Spanned;

public class HtmlCompat {

    public static Spanned fromHtml(String source) {
        return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
    }


    public static String toHtml(Spannable source) {
        return Html.toHtml(source, Html.FROM_HTML_MODE_LEGACY);
    }

}
