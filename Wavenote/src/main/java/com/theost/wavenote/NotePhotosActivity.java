package com.theost.wavenote;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;

import com.theost.wavenote.utils.ThemeUtils;

public class NotePhotosActivity extends ThemedAppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                addPhoto();
                return true;
            case R.id.menu_sort:
                sortPhotos();
                return true;
            case R.id.menu_remove:
                removePhoto(-1);
                return true;
            default:
                return false;
        }
    }

    private void addPhoto() {
        // todo
    }

    private  void editPhoto() {
        // todo
    }

    private void removePhoto(int position) {
        /*
        if (position == -1) {
            todo: remove all
            return;
        }
        todo
        return;
         */
    }

    private void sortPhotos() {
        // todo
    }

    /* Animation
    public void animateTrash(View view) {
        ImageButton btn = findViewById(R.id.remove_item);
        Drawable drawable = btn.getDrawable();
        DrawableUtils.startAnimatedVectorDrawable(drawable);
    }
     */

}
