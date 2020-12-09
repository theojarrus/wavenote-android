package com.theost.wavenote;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.theost.wavenote.adapters.DictionaryAdapter;
import com.theost.wavenote.models.Keyword;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.ImportUtils;
import com.theost.wavenote.utils.ResUtils;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

public class DictionaryActivity extends ThemedAppCompatActivity {

    private int[] keywordColors;
    private String[] keywordTypes;
    private DictionaryAdapter adapter;
    private List<Keyword> mKeywordList;
    private List<String> mWordList;
    private LinearLayout emptyView;
    private LinearLayout mSortLayout;
    private EditText mAddKeywordEditText;
    private RadioGroup mAddKeywordType;
    private DatabaseHelper localDatabase;
    private RecyclerView mKeywordRecyclerView;
    private String activeSortType;
    private MenuItem mRemoveItem;

    private RelativeLayout mSortLayoutContent;
    private ObjectAnimator mSortDirectionAnimation;
    private ImageView mSortDirection;
    private TextView mSortOrder;
    private boolean mIsSortDown;
    private boolean mIsSortReverse;

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

        Note.setNeedResourceUpdate(true);

        emptyView = findViewById(android.R.id.empty);
        ImageView mEmptyViewImage = emptyView.findViewById(R.id.image);
        TextView mEmptyViewText = emptyView.findViewById(R.id.text);
        mEmptyViewImage.setImageResource(R.drawable.av_theory_24dp);
        mEmptyViewText.setText(R.string.empty_dictionary);

        keywordColors = ResUtils.getDialogColors(this);
        keywordTypes = ResUtils.getKeywordTypes(this);

        mKeywordRecyclerView = findViewById(R.id.keywords_list);
        localDatabase = new DatabaseHelper(this);

        mSortLayout = findViewById(R.id.sort_layout);

        mKeywordList = new ArrayList<>();

        adapter = new DictionaryAdapter(this, mKeywordList);
        mKeywordRecyclerView.setAdapter(adapter);

        if (Note.getDictionaryActiveSortMode() == 0)
            Note.setDictionaryActiveSortMode(R.id.sort_by_date);

        mSortLayoutContent = findViewById(R.id.sort_content);
        mSortOrder = findViewById(R.id.sort_order);

        PopupMenu popup = new PopupMenu(mSortOrder.getContext(), mSortOrder, Gravity.START);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.items_sort, popup.getMenu());

        mSortOrder.setText((popup.getMenu().findItem(Note.getDictionaryActiveSortMode())).getTitle());

        mSortLayoutContent.setOnClickListener(v -> {
            popup.setOnMenuItemClickListener(item -> {
                // Do nothing when same sort is selected.
                if (mSortOrder.getText().equals(item.getTitle()))
                    return false;
                mSortOrder.setText(item.getTitle());
                Note.setDictionaryActiveSortMode(item.getItemId());
                sortItems();
                return true;
            });
            popup.show();
        });

        mIsSortReverse = Note.isDictionarySortDirRev();

        mSortDirection = findViewById(R.id.sort_direction);
        ImageView sortDirectionSwitch = findViewById(R.id.sort_direction_switch);
        sortDirectionSwitch.setImageResource(R.drawable.ic_sort_order_24dp);
        sortDirectionSwitch.setOnClickListener(v -> {
            if (Note.isDictionarySortDirRev()) {
                Note.setDictionarySortDirRev(false);
                if (mIsSortReverse) {
                    mSortDirectionAnimation.start();
                } else {
                    mSortDirectionAnimation.reverse();
                }
            } else {
                Note.setDictionarySortDirRev(true);
                if (!mIsSortReverse) {
                    mSortDirectionAnimation.start();
                } else {
                    mSortDirectionAnimation.reverse();
                }
            }
            sortItems();
        });

        sortDirectionSwitch.setOnLongClickListener(v -> {
            if (v.isHapticFeedbackEnabled()) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            Toast.makeText(this, this.getString(R.string.sort_search_reverse_order), Toast.LENGTH_SHORT).show();
            return true;
        });

        setSortDirection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items_list, menu);
        DrawableUtils.tintMenuWithAttribute(this, menu, R.attr.toolbarIconColor);
        mRemoveItem = menu.findItem(R.id.menu_remove);
        MenuCompat.setGroupDividerEnabled(menu, true);
        updateData();
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                showKeywordDialog();
                return true;
            case R.id.menu_restore:
                restoreData();
                return true;
            case R.id.menu_remove:
                showRemoveDialog();
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return false;
        }
    }

    public boolean updateEmptyView() {
        if (adapter == null || adapter.getItemCount() == 0) {
            mRemoveItem.setEnabled(false);
            mKeywordRecyclerView.setHasFixedSize(false);
            mKeywordRecyclerView.setVisibility(View.INVISIBLE);
            mSortLayout.setVisibility(View.INVISIBLE);
            emptyView.setVisibility(View.VISIBLE);
            return false;
        } else {
            mKeywordRecyclerView.setVisibility(View.VISIBLE);
            mSortLayout.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            mRemoveItem.setEnabled(true);
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localDatabase != null) localDatabase.close();
    }

    public void disableDictionaryInputs(EditText editText, AutoCompleteTextView autoCompleteTextView) {
        ViewUtils.disbaleInput(editText);
        ViewUtils.disbaleInput(autoCompleteTextView);
        ViewUtils.updateDropdown(this, autoCompleteTextView, keywordTypes);
    }

    private void showRemoveDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.remove_all)
                .content(R.string.confirm_delete_all)
                .positiveText(R.string.yes)
                .onPositive((dialog, which) -> removeKeyword(null, null))
                .negativeText(R.string.no).show();
    }

    private void showKeywordDialog() {
        MaterialDialog keywordDialog = new MaterialDialog.Builder(this)
                .customView(R.layout.add_dialog, false)
                .title(R.string.add_keyword)
                .positiveText(R.string.import_text)
                .positiveColor(keywordColors[0])
                .onPositive((dialog, which) -> insertKeyword(mAddKeywordEditText.getText().toString()))
                .negativeText(R.string.cancel).build();
        MDButton addButton = keywordDialog.getActionButton(DialogAction.POSITIVE);
        mAddKeywordType = keywordDialog.getCustomView().findViewById(R.id.keyword_type);
        mAddKeywordType.setVisibility(View.VISIBLE);
        mAddKeywordEditText = keywordDialog.getCustomView().findViewById(R.id.dialog_input);
        mAddKeywordEditText.setText("");
        mAddKeywordEditText.setHint(R.string.word);
        mAddKeywordEditText.requestFocus();
        mAddKeywordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (mAddKeywordEditText.getText().length() == 0) {
                    addButton.setEnabled(false);
                    addButton.setTextColor(keywordColors[0]);
                } else {
                    addButton.setEnabled(true);
                    addButton.setTextColor(keywordColors[1]);
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

    private void updateAdapter() {
        mKeywordRecyclerView.setHasFixedSize(false);
        adapter.updateData(mKeywordList);
        if (updateEmptyView()) sortItems();
    }

    private void updateData() {
        new UpdateDataThread().start();
    }

    private final Handler mUpdateHandler = new Handler(Looper.getMainLooper(), msg -> {
        updateAdapter();
        return true;
    });

    private class UpdateDataThread extends Thread {
        public void run() {
            mWordList = new ArrayList<>();
            mKeywordList = new ArrayList<>();
            Cursor mKeywordData = localDatabase.getAllDictionaryData();
            if (mKeywordData == null || mKeywordData.getCount() == 0) {
                mSortLayout.setVisibility(View.INVISIBLE);
            } else {
                while (mKeywordData.moveToNext()) {
                    String id = mKeywordData.getString(0);
                    String word = mKeywordData.getString(1);
                    String type = mKeywordData.getString(2);
                    Keyword keyword = new Keyword(id, word, type);
                    mWordList.add(word.toLowerCase());
                    mKeywordList.add(keyword);
                }
            }
            mUpdateHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
        }
    }

    public void restoreData() {
        ResUtils.restoreDictionary(this);
        updateData();
    }

    @SuppressLint("NonConstantResourceId")
    private void insertKeyword(String keyword) {
        if (keyword.equals("") || mAddKeywordType.getCheckedRadioButtonId() == -1) return;
        if (mWordList.contains(keyword.toLowerCase())) {
            DisplayUtils.showToast(this, getString(R.string.exist_error));
            return;
        }
        String type;
        switch (mAddKeywordType.getCheckedRadioButtonId()) {
            case R.id.type_title:
                type = keywordTypes[0];
                break;
            case R.id.type_word:
                type = keywordTypes[1];
                break;
            default:
                return;
        }
        boolean isInserted = localDatabase.insertDictionaryData(keyword, type);
        if (!isInserted) {
            DisplayUtils.showToast(this, this.getString(R.string.database_error));
        }
        updateData();
    }

    public boolean renameKeyword(String id, String type) {
        boolean isRenamed = localDatabase.renameDictionaryData(id, type);
        if (!isRenamed) {
            DisplayUtils.showToast(this, getString(R.string.database_error));
            return false;
        }
        return true;
    }

    public boolean removeKeyword(String id, String keyword) {
        mKeywordRecyclerView.setHasFixedSize(true);
        boolean isRemoved;
        if (id == null) {
            isRemoved = localDatabase.removeDictionaryData(DatabaseHelper.COL_0);
            if (isRemoved) {
                mWordList = new ArrayList<>();
                adapter.clearData();
                updateEmptyView();
            }
        } else {
            isRemoved = localDatabase.removeDictionaryData(id);
            if (isRemoved)
                mWordList.remove(keyword.toLowerCase());
        }
        if (!isRemoved) {
            DisplayUtils.showToast(this, getString(R.string.database_error));
            return false;
        }
        return true;
    }

    private void setSortDirection() {

        if (Note.isDictionarySortDirRev()) {
            mSortDirection.setContentDescription(getString(R.string.description_up));
            mSortDirection.setImageResource(R.drawable.ic_arrow_up_16dp);
            mIsSortDown = false;
        } else {
            mSortDirection.setContentDescription(getString(R.string.description_down));
            mSortDirection.setImageResource(R.drawable.ic_arrow_down_16dp);
            mIsSortDown = true;
        }

        mSortDirectionAnimation = ObjectAnimator.ofFloat(
                mSortDirection,
                View.ROTATION,
                0f,
                mIsSortDown ? -180f : 180f
        ).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
    }

    @SuppressLint("NonConstantResourceId")
    private void sortItems() {
        switch (Note.getDictionaryActiveSortMode()) {
            case R.id.sort_by_date:
                adapter.sortByDate(Note.isDictionarySortDirRev());
                break;
            case R.id.sort_by_name:
                adapter.sortByName(Note.isDictionarySortDirRev());
                break;
            case R.id.sort_by_type:
                adapter.sortByType(Note.isDictionarySortDirRev());
                break;
        }
    }

}
