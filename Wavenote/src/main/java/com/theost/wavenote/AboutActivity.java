package com.theost.wavenote;

import android.app.ActivityManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.theost.wavenote.utils.DrawableUtils;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);

        int appColor;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            appColor = getResources().getColor(R.color.blue);
            toolbar.setBackgroundColor(appColor);
            getWindow().setNavigationBarColor(appColor);
            getWindow().getDecorView().setBackgroundColor(appColor);
        } else {
            appColor = getResources().getColor(R.color.blue, getTheme());
        }

        setSupportActionBar(toolbar);
        setTitle("");

        this.setTaskDescription(new ActivityManager.TaskDescription(null, null, appColor));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(DrawableUtils.tintDrawableWithResource(
                this, R.drawable.ic_cross_24dp, android.R.color.white
            ));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
