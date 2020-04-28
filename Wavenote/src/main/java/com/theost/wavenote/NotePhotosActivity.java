package com.theost.wavenote;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;

import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.ThemeUtils;

public class NotePhotosActivity extends ThemedAppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.note_photos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        setTitle(R.string.photos);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items_list, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);

        return true;
    }

    public void animateTrash(View view) {
        ImageButton btn = findViewById(R.id.remove_item);
        Drawable drawable = btn.getDrawable();
        DrawableUtils.startAnimatedVectorDrawable(drawable);
    }

}
