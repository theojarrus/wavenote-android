package com.theost.wavenote;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.theost.wavenote.models.Keyword;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DisplayUtils;
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

        updateColors();
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
        ViewUtils.disbaleInput(editText);
        ViewUtils.disbaleInput(autoCompleteTextView);
        ViewUtils.updateDropdown(this, autoCompleteTextView, KEYWORD_TYPES);
    }

    private void updateColors() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        colorEnabled = ContextCompat.getColor(this, typedValue.resourceId);
        colorDisabled = ContextCompat.getColor(this, R.color.gray_20);
    }

    private void showDialog() {
        MaterialDialog keywordDialog = new MaterialDialog.Builder(this)
                .customView(R.layout.add_dialog, false)
                .title(R.string.add_keyword)
                .positiveText(R.string.add)
                .positiveColor(colorDisabled)
                .onPositive((dialog, which) -> insertKeyword(mAddKeywordEditText.getText().toString()))
                .negativeText(R.string.cancel).build();
        MDButton addButton = keywordDialog.getActionButton(DialogAction.POSITIVE);
        mAddKeywordEditText = keywordDialog.getCustomView().findViewById(R.id.dialog_input);
        mAddKeywordEditText.setText("");
        mAddKeywordEditText.requestFocus();
        mAddKeywordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (mAddKeywordEditText.getText().length() == 0) {
                    addButton.setTextColor(colorDisabled);
                } else {
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
        keywordDialog.show();
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
        String[] resourceWords = this.getResources().getStringArray(R.array.array_musical_words);
        String[] resourceTitles = this.getResources().getStringArray(R.array.array_musical_titles);
        for (String i : resourceWords) localDatabase.insertDictionaryData(i, KEYWORD_TYPES[0]);
        for (String j : resourceTitles) localDatabase.insertDictionaryData(j, KEYWORD_TYPES[1]);
        updateData();
        sortKeywords(false);
        checkEmptyView();
    }

    private void insertKeyword(String keyword) {
        if (keyword.equals("")) return;
        boolean isInserted = localDatabase.insertDictionaryData(keyword, KEYWORD_TYPES[0]);
        if (!isInserted) {
            DisplayUtils.showToast(this, this.getResources().getString(R.string.database_error));
        }
        updateData();
        sortKeywords(false);
        checkEmptyView();
    }

    public boolean renameKeyword(String id, String type) {
        boolean isRenamed = localDatabase.renameDictionaryData(id, type);
        if (!isRenamed) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
            return false;
        }
        return true;
    }

    public boolean removeKeyword(String id) {
        if (adapter.getItemCount() == 0) return false;
        boolean isRemoved;
        if (id == null) {
            isRemoved = localDatabase.removeDictionaryData(DatabaseHelper.COL_0);
            if (isRemoved) {
                adapter.clearData();
                checkEmptyView();
            }
        } else {
            isRemoved = localDatabase.removeDictionaryData(id);
        }
        if (!isRemoved) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
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
