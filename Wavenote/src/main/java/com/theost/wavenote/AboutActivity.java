package com.theost.wavenote;

import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class AboutActivity extends AppCompatActivity {

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);

        int appColor = ContextCompat.getColor(this, R.color.blue);
        toolbar.setBackgroundColor(appColor);
        getWindow().setNavigationBarColor(appColor);
        getWindow().getDecorView().setBackgroundColor(appColor);

        setSupportActionBar(toolbar);
        setTitle("");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setTaskDescription(new ActivityManager.TaskDescription(null, null, appColor));
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_24dp);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
