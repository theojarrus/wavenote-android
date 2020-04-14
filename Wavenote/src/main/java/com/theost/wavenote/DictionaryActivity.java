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

public class DictionaryActivity extends ThemedAppCompatActivity {

    public static final String DEFAULT_KEYWORD_TYPE = "Word";
    String[] keywordTypes = {"Word", "Title"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dictionary);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        setTitle(R.string.dictionary);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, keywordTypes);
        AutoCompleteTextView mKeywordTypeTextView = findViewById(R.id.keyword_type_input);
        disableInputTextView(mKeywordTypeTextView);
        mKeywordTypeTextView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.key_dictionary, menu);
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
            case R.id.menu_reset:
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

    private void removeKeyword() {
        // todo
    }

    private void sortKeywords() {
        // todo
    }

    public void disableInputTextView(TextView view) {
        view.setInputType(InputType.TYPE_NULL);
        view.setText(DEFAULT_KEYWORD_TYPE);
        view.setCursorVisible(false);
        view.setKeyListener(null);
    }

    public void animateTrash(View view) {
        ImageButton btn = findViewById(R.id.remove_keyword);
        Drawable drawable = btn.getDrawable();
        DrawableUtils.startAnimatedVectorDrawable(drawable);
    }

}
