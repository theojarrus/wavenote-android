package com.theost.wavenote.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.theost.wavenote.R;
import com.theost.wavenote.models.Note;

import java.util.List;
import java.util.Objects;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

public class ThemeUtils {

    // theme constants
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    public static final int THEME_AUTO = 2;
    private static final int THEME_SYSTEM = 3;
    private static final String PREFERENCES_URI_AUTHORITY = "preferences";
    private static final String URI_SEGMENT_THEME = "theme";

    public static void setTheme(Activity activity) {
        // if we have a data uri that sets the theme let's do it here
        Uri data = activity.getIntent().getData();
        if (data != null) {
            if (Objects.equals(data.getAuthority(), PREFERENCES_URI_AUTHORITY)) {
                List<String> segments = data.getPathSegments();

                // check if we have reached /preferences/theme
                if (segments.size() > 0 && segments.get(0).equals(URI_SEGMENT_THEME)) {

                    // activate the theme preference
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(PrefUtils.PREF_THEME_MODIFIED, true);

                    editor.apply();
                }
            }
        }

        switch (PrefUtils.getIntPref(activity, PrefUtils.PREF_THEME, THEME_LIGHT)) {
            case THEME_AUTO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static boolean isLightTheme(Context context) {
        return (context.getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) != UI_MODE_NIGHT_YES;
    }

    public static void updateTextTheme(Context context) {
        String lightColor = "#" + Integer.toHexString(ContextCompat.getColor(context, R.color.background_light)).substring(2).toUpperCase();
        String darkColor = "#" + Integer.toHexString(ContextCompat.getColor(context, R.color.background_dark)).substring(2).toUpperCase();
        boolean isThemeLight = false;
        if (ThemeUtils.isLightTheme(context)) isThemeLight = true;
        Note.setThemeText(lightColor, darkColor, isThemeLight);
    }

    /*
     * returns the optimal pixel width to use for the menu drawer based on:
     * http://www.google.com/design/spec/layout/structure.html#structure-side-nav
     * http://www.google.com/design/spec/patterns/navigation-drawer.html
     * http://android-developers.blogspot.co.uk/2014/10/material-design-on-android-checklist.html
     * https://medium.com/sebs-top-tips/material-navigation-drawer-sizing-558aea1ad266
     */
    public static int getOptimalDrawerWidth(Activity activity) {
        Point displaySize = DisplayUtils.getDisplayPixelSize(activity);
        int appBarHeight = DisplayUtils.getActionBarHeight(activity);
        int drawerWidth = Math.min(displaySize.x, displaySize.y) - appBarHeight;
        int maxDp = (DisplayUtils.isXLarge(activity) ? 400 : 320);
        int maxPx = DisplayUtils.dpToPx(activity, maxDp);
        return Math.min(drawerWidth, maxPx);
    }

    public static int getThemeTextColorId(Context context) {
        if (context == null) {
            return 0;
        }

        int[] attrs = {R.attr.noteEditorTextColor};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int textColorId = ta.getResourceId(0, android.R.color.black);
        ta.recycle();

        return textColorId;
    }

    public static int getColorFromAttribute(@NonNull Context context, @AttrRes int attribute) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attribute});
        int colorResId = typedArray.getResourceId(0, android.R.color.black);
        typedArray.recycle();
        return ContextCompat.getColor(context, colorResId);
    }

    public static boolean isColorDark(int color) {
        if (color == Color.TRANSPARENT) {
            return false;
        }
        double brightness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return brightness >= 0.5;
    }

}