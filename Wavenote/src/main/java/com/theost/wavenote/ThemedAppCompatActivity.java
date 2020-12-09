package com.theost.wavenote;

import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;

import com.theost.wavenote.utils.ThemeUtils;

/**
 * Abstract class to apply {@link R.style#Theme_Wavestyle} theme to activities extending from it.
 * Override {@link ThemedAppCompatActivity#mThemeId} in extended activity to apply another theme.
 */
abstract public class ThemedAppCompatActivity extends AppCompatActivity {
    protected @StyleRes int mThemeId = R.style.Theme_Wavestyle;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(mThemeId);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setTaskDescription(new ActivityManager.TaskDescription(null, null, getWindowColor()));
        }
    }

    protected int getWindowColor() {
        return ThemeUtils.getColorFromAttribute(this, R.attr.mainBackgroundColor);
    }

}
