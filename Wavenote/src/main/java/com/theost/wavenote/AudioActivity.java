package com.theost.wavenote;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.ViewUtils;

public class AudioActivity extends ThemedAppCompatActivity {

    private final String[] BEATS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //RecyclerView recyclerView = findViewById(R.id.audio_tracks);
        LinearLayout emptyView = findViewById(R.id.empty_view);

        // if data == null
        //recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);

        //AutoCompleteTextView mBeatAccentTextView = findViewById(R.id.beat_1);
        //AutoCompleteTextView mBeatCountTextView = findViewById(R.id.beat_2);

        //ViewUtils.disableAutoCompleteTextView(this, mBeatAccentTextView, BEATS);
        //ViewUtils.disableAutoCompleteTextView(this, mBeatCountTextView, BEATS);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.audio_list, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_remove:
                // todo
                return true;
            case R.id.menu_import:
                // todo
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
