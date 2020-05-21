package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.models.Keyword;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.KeywordAdapter;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DictionaryActivity extends ThemedAppCompatActivity {

    private final String[] KEYWORD_TYPES = {"Word", "Title"};
    private final String[] SORT_TYPES = {"date", "name", "type"};
    private androidx.appcompat.app.AlertDialog keywordDialog;
    private KeywordAdapter adapter;
    private List<Keyword> mKeywordList;
    private LinearLayout emptyView;
    private EditText mAddKeywordEditText;
    private DatabaseHelper localDatabase;
    private RecyclerView mKeywordRecyclerView;
    private String activeSortType;
    private MenuItem mRemoveItem;
    private MenuItem mSortItem;
    private int colorEnabled;
    private int colorDisabled;

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

        emptyView = findViewById(android.R.id.empty);
        ImageView mEmptyViewImage = emptyView.findViewById(R.id.image);
        TextView mEmptyViewText = emptyView.findViewById(R.id.text);
        mEmptyViewImage.setImageResource(R.drawable.av_dictionary_24dp);
        mEmptyViewText.setText(R.string.empty_dictionary);

        activeSortType = SORT_TYPES[0];
        mKeywordRecyclerView = findViewById(R.id.keywords_list);
        localDatabase = new DatabaseHelper(this);

        createDialogView();
        updateData();
        adapter = new KeywordAdapter(this, mKeywordList);
        mKeywordRecyclerView.setAdapter(adapter);

        sortKeywords(false);
        Note.setNeedResourcesUpdate(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items_list, menu);
        mRemoveItem = menu.findItem(R.id.menu_remove);
        mSortItem = menu.findItem(R.id.menu_sort);
        MenuCompat.setGroupDividerEnabled(menu, true);
        checkEmptyView();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                showDialog();
                return true;
            case R.id.menu_sort:
                sortKeywords(true);
                return true;
            case R.id.menu_restore:
                restoreData();
                return true;
            case R.id.menu_remove:
                removeKeyword(null);
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return false;
        }
    }

    public void checkEmptyView() {
        if (adapter.getItemCount() == 0) {
            mRemoveItem.setEnabled(false);
            mSortItem.setEnabled(false);
            DrawableUtils.setMenuItemAlpha(mSortItem, 0.3);
            mKeywordRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            mKeywordRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            mRemoveItem.setEnabled(true);
            mSortItem.setEnabled(true);
            DrawableUtils.setMenuItemAlpha(mSortItem, 1.0);
        }
    }

    public void disableDictionaryInputs(EditText editText, AutoCompleteTextView autoCompleteTextView) {
        ViewUtils.disableEditText(editText);
        ViewUtils.disableAutoCompleteTextView(this, autoCompleteTextView, KEYWORD_TYPES);
    }

    private void createDialogView() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        colorEnabled = ContextCompat.getColor(this, typedValue.resourceId);
        colorDisabled = ContextCompat.getColor(this, R.color.gray_20);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Dialog));
        LayoutInflater layout = LayoutInflater.from(this);
        @SuppressLint("InflateParams") View promptsView = layout.inflate(R.layout.add_dialog, null);
        builder.setView(promptsView);
        builder.setTitle(R.string.add_keyword);
        builder.setPositiveButton(R.string.add, (dialog, whichButton) -> insertKeyword(mAddKeywordEditText.getText().toString()));
        builder.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {});
        keywordDialog = builder.create();
    }

    private void updateDialog() {
        final Button addButton = keywordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        addButton.setEnabled(false);
        addButton.setTextColor(colorDisabled);
        mAddKeywordEditText = keywordDialog.findViewById(R.id.dialog_keyword);
        mAddKeywordEditText.setText("");
        mAddKeywordEditText.requestFocus();
        mAddKeywordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (mAddKeywordEditText.getText().length() == 0) {
                    addButton.setEnabled(false);
                    addButton.setTextColor(colorDisabled);
                } else {
                    addButton.setEnabled(true);
                    addButton.setTextColor(colorEnabled);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void showDialog() {
        keywordDialog.show();
        updateDialog();
    }

    private void showError(String error) {
        Toast toast = Toast.makeText(this, error, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void updateData() {
        Cursor mKeywordData = localDatabase.getAllDictionaryData();
        if (mKeywordData == null) return;
        mKeywordList = new ArrayList<>();
        while (mKeywordData.moveToNext()) {
            String id = mKeywordData.getString(0);
            String word = mKeywordData.getString(1);
            String type = mKeywordData.getString(2);
            Keyword keyword = new Keyword(id, word, type);
            mKeywordList.add(keyword);
        }
        if (adapter != null) adapter.updateData(mKeywordList);
    }

    private void restoreData() {
        removeKeyword(null);
        String[] resourceWords = this.getResources().getStringArray(R.array.array_musical_words);
        String[] resourceTitles = this.getResources().getStringArray(R.array.array_musical_titles);
        for (String i : resourceWords) localDatabase.insertDictionaryData(i, KEYWORD_TYPES[0]);
        for (String j : resourceTitles) localDatabase.insertDictionaryData(j, KEYWORD_TYPES[1]);
        updateData();
        sortKeywords(false);
        checkEmptyView();
    }

    private void insertKeyword(String keyword) {
        boolean isInserted = localDatabase.insertDictionaryData(keyword, KEYWORD_TYPES[0]);
        if (!isInserted) {
            showError(this.getResources().getString(R.string.database_error));
        }
        updateData();
        sortKeywords(false);
        checkEmptyView();
    }

    public boolean renameKeyword(String id, String type) {
        boolean isRenamed = localDatabase.renameDictionaryData(id, type);
        if (!isRenamed) {
            showError(this.getResources().getString(R.string.database_error));
            return false;
        }
        return true;
    }

    public boolean removeKeyword(String id) {
        if (adapter.getItemCount() == 0) return false;
        boolean isRemoved;
        if (id == null) {
            isRemoved = localDatabase.removeDictionaryData("ID");
            if (isRemoved) {
                adapter.clearData();
                checkEmptyView();
            }
        } else {
            isRemoved = localDatabase.removeDictionaryData(id);
        }
        if (!isRemoved) {
            showError(this.getResources().getString(R.string.database_error));
            return false;
        }
        return true;
    }

    private void sortKeywords(boolean isModeChanged) {
        if (adapter.getItemCount() == 0) return;
        if (isModeChanged) {
            int index = Arrays.asList(SORT_TYPES).indexOf(activeSortType) + 1;
            if (index == SORT_TYPES.length) index = 0;
            activeSortType = SORT_TYPES[index];
        }
        if (activeSortType.equals(SORT_TYPES[0])) {
            adapter.sortByDate();
        } else if (activeSortType.equals(SORT_TYPES[1])) {
            adapter.sortByName();
        } else if (activeSortType.equals(SORT_TYPES[2])) {
            adapter.sortByType();
        }
    }

}
