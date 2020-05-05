package com.theost.wavenote;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;

import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.ViewUtils;

public class DictionaryActivity extends ThemedAppCompatActivity {

    private final String DEFAULT_KEYWORD_TYPE = "Word";
    String[] keywordTypes = {"Word", "Title"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dictionary);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setTitle(R.string.dictionary);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        /* todo
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, keywordTypes);
        AutoCompleteTextView mKeywordTypeTextView = findViewById(R.id.keyword_type_input);
        ViewUtils.disableAutoCompleteTextView(this, mKeywordTypeTextView, DEFAULT_KEYWORD_TYPE, keywordTypes);
         */
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
                addKeyword();
                return true;
            case R.id.menu_sort:
                sortKeywords();
                return true;
            case R.id.menu_remove:
                removeKeyword(-1);
                return true;
            default:
                return false;
        }
    }

    private void addKeyword() {
        // todo
    }

    private  void editKeyword() {
        // todo
    }

    private void removeKeyword(int position) {
        /*
        if (position == -1) {
            todo: remove all
            return;
        }
        todo
        return;
         */
    }

    private void sortKeywords() {
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
