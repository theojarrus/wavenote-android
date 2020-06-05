package com.theost.wavenote;

import android.app.ActivityManager;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Abstract class to apply {@link R.style#Theme_Wavestyle} theme to activities extending from it.
 * Override {@link ThemedAppCompatActivity#mThemeId} in extended activity to apply another theme.
 */
abstract public class ThemedAppCompatActivity extends AppCompatActivity {
    protected @StyleRes int mThemeId = R.style.Theme_Wavestyle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(mThemeId);

        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(null, null, getWindowColor());
        this.setTaskDescription(taskDescription);
    }

    protected int getWindowColor() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.mainBackgroundColor, typedValue, true);
        return ContextCompat.getColor(this, typedValue.resourceId);
    }

}
